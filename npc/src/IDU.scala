import chisel3._
import chisel3.util.BitPat
import chisel3.util.experimental.decode._
import chisel3.util._

class IDU extends Module{
    val io = IO(new Bundle{
        val before = new Bundle{
            val valid           = Input (Bool())
            val ready           = Output(Bool())
            val PC              = Input (UInt(32.W))
            val inst            = Input (UInt(32.W))
            val branchPC        = Output(UInt(32.W))
            val ifbranch        = Output(Bool())
        }
        val next = new Bundle{
            val valid           = Output(Bool())
            val ready           = Input (Bool())
            val alu_src1        = Output(UInt(32.W))
            val alu_src2        = Output(UInt(32.W))
            val alu_op          = Output(UInt(ALUop.op_width.W))
            val reg_op          = Output(UInt(Regop.op_width.W))
            val reg_rd          = Output(UInt(5.W))
            val mem_op          = Output(UInt(Memop.op_width.W))
            val mem_src         = Output(UInt(32.W))
        }
        val regfile = new Bundle{
            val raddr1          = Output(UInt(5.W))
            val raddr2          = Output(UInt(5.W))
            val rdata1          = Input (UInt(32.W))
            val rdata2          = Input (UInt(32.W))
        }
    })
    //fluiding control signals
    val valid                   = RegInit(0.U(1.W))
    val will_out                = Wire(Bool())
    val will_in                 = Wire(Bool())
    when(will_in){
        valid                   := Mux(branch_taken,0.U(1.W),1.U(1.W))
    }.elsewhen(will_out){
        valid                   := 0.U(1.W)
    }
    will_out                    := io.next.ready & io.next.valid
    will_in                     := io.before.valid & io.before.ready
    io.before.ready             := (!valid | will_out) & !will_halt
    io.next.valid               := valid & !will_halt

    //latching signals
    val regPC                   = RegInit(0.U(32.W))
    val regInst                 = RegInit(0.U(32.W))
    when(will_in){
        regPC                   := io.before.PC
        regInst                 := io.before.inst
    }

    //register file read
    val rs1                     = regInst(19, 15)
    val rs2                     = regInst(24, 20)
    io.regfile.raddr1           := rs1
    io.regfile.raddr2           := rs2

    //decoder
    val inst_decoder            = Module(new inst_decoder)
    inst_decoder.io.inst        := regInst
    io.next.alu_op              := inst_decoder.io.alu_op
    io.next.reg_op              := inst_decoder.io.reg_op
    io.next.reg_rd              := regInst(11, 7)
    io.next.mem_op              := inst_decoder.io.mem_op

    //imm generation
    val inst_type                = inst_decoder.io.inst_type
    val imm_gen                  = Module(new imm_gen)
    imm_gen.io.inst              := regInst
    imm_gen.io.inst_type         := inst_type
    val imm                      = imm_gen.io.imm

    //branch control
    val branch_ctrl              = Module(new branch_ctrl)
    val branch_taken             = branch_ctrl.io.take_branch//there have some problem, fix in the future
    branch_ctrl.io.src1          := io.regfile.rdata1
    branch_ctrl.io.src2          := io.regfile.rdata2
    branch_ctrl.io.pc            := regPC
    branch_ctrl.io.imm           := imm
    branch_ctrl.io.branch_op     := inst_decoder.io.branch_op
    io.before.branchPC           := branch_ctrl.io.branch_target
    io.before.ifbranch           := branch_taken

    //ALU source selection
    val src1_op                  = inst_decoder.io.src1_op
    val src2_op                  = inst_decoder.io.src2_op
    io.next.alu_src1             := 0.U
    io.next.alu_src2             := 0.U
    switch(src1_op){
        is(Srcop.use_zero){io.next.alu_src1 := 0.U}
        is(Srcop.use_reg){io.next.alu_src1 := io.regfile.rdata1}
        is(Srcop.use_imm){io.next.alu_src1 := imm}
        is(Srcop.use_pc){io.next.alu_src1 := regPC}
        is(Srcop.use_four){io.next.alu_src1 := 4.U}
    }
    switch(src2_op){
        is(Srcop.use_zero){io.next.alu_src2 := 0.U}
        is(Srcop.use_reg){io.next.alu_src2 := io.regfile.rdata2}
        is(Srcop.use_imm){io.next.alu_src2 := imm}
        is(Srcop.use_pc){io.next.alu_src2 := regPC}
        is(Srcop.use_four){io.next.alu_src2 := 4.U}
    }
    io.next.mem_src              := io.regfile.rdata2

    //terminater
    val special_op               = inst_decoder.io.special_op
    val will_halt                = special_op === Specialop.halt_error || special_op === Specialop.halt_normal
    val is_error_halt            = special_op === Specialop.halt_error
    val halt_counter             = RegInit(10.U(32.W))
    when(will_halt){
        halt_counter              := halt_counter - 1.U
    }
    val u_specialio                = Module(new SpecialIO)
    u_specialio.io.halt           := halt_counter === 0.U
    u_specialio.io.error          := is_error_halt
}

class inst_decoder extends Module{
    val io = IO(new Bundle{
        val inst                = Input (UInt(32.W))
        val inst_type           = Output(UInt(InstType.type_width.W))
        val alu_op              = Output(UInt(ALUop.op_width.W))
        val reg_op              = Output(UInt(Regop.op_width.W))
        val mem_op              = Output(UInt(Memop.op_width.W))
        val branch_op           = Output(UInt(Branchop.op_width.W))
        val src1_op             = Output(UInt(Srcop.op_width.W))
        val src2_op             = Output(UInt(Srcop.op_width.W))
        val special_op          = Output(UInt(Specialop.op_width.W))
    })
    def concatBitPat(bps: BitPat*): BitPat = {
        val bits = bps.map(_.rawString.stripPrefix("b").replace("?", "_")).mkString
        BitPat("b" + bits)
    }
    val table = TruthTable(
        Map(
            InstCode.add     -> concatBitPat(InstType.b_R, ALUop.b_add, Regop.b_w_alu, Memop.b_noop     , Branchop.b_noop, Srcop.b_use_reg  , Srcop.b_use_reg ,Specialop.b_noop),
            InstCode.jalr    -> concatBitPat(InstType.b_I, ALUop.b_add, Regop.b_w_alu, Memop.b_noop     , Branchop.b_jalr, Srcop.b_use_pc   , Srcop.b_use_four,Specialop.b_noop),
            InstCode.addi    -> concatBitPat(InstType.b_I, ALUop.b_add, Regop.b_w_alu, Memop.b_noop     , Branchop.b_noop, Srcop.b_use_reg  , Srcop.b_use_imm ,Specialop.b_noop),
            InstCode.lbu     -> concatBitPat(InstType.b_I, ALUop.b_add, Regop.b_w_mem, Memop.b_l_byte_u , Branchop.b_noop, Srcop.b_use_reg  , Srcop.b_use_imm ,Specialop.b_noop),
            InstCode.lw      -> concatBitPat(InstType.b_I, ALUop.b_add, Regop.b_w_mem, Memop.b_l_word   , Branchop.b_noop, Srcop.b_use_reg  , Srcop.b_use_imm ,Specialop.b_noop),
            InstCode.ebreak  -> concatBitPat(InstType.b_I, ALUop.b_add, Regop.b_noop , Memop.b_noop     , Branchop.b_noop, Srcop.b_use_zero , Srcop.b_use_zero,Specialop.b_halt_normal),
            InstCode.sb      -> concatBitPat(InstType.b_S, ALUop.b_add, Regop.b_noop , Memop.b_s_byte   , Branchop.b_noop, Srcop.b_use_reg  , Srcop.b_use_imm ,Specialop.b_noop),
            InstCode.sw      -> concatBitPat(InstType.b_S, ALUop.b_add, Regop.b_noop , Memop.b_s_word   , Branchop.b_noop, Srcop.b_use_reg  , Srcop.b_use_imm ,Specialop.b_noop),
            InstCode.lui     -> concatBitPat(InstType.b_U, ALUop.b_add, Regop.b_w_alu, Memop.b_noop     , Branchop.b_noop, Srcop.b_use_imm  , Srcop.b_use_zero,Specialop.b_noop)
        ),
        concatBitPat(InstType.b_Invalid, ALUop.b_add, Regop.b_noop, Memop.b_noop, Branchop.b_noop, Srcop.b_use_zero, Srcop.b_use_zero, Specialop.b_halt_error)
    )

  val decoded = decoder(io.inst, table)

  var start                     = 0
  io.inst_type                  := decoded(start + InstType.type_width - 1, start)
  start                         = start + InstType.type_width
  io.alu_op                     := decoded(start + ALUop.op_width - 1, start)
  start                         = start + ALUop.op_width
  io.reg_op                     := decoded(start + Regop.op_width - 1, start)
  start                         = start + Regop.op_width
  io.mem_op                     := decoded(start + Memop.op_width - 1, start)
  start                         = start + Memop.op_width
  io.branch_op                  := decoded(start + Branchop.op_width - 1, start)
  start                         = start + Branchop.op_width
  io.src1_op                    := decoded(start + Srcop.op_width - 1, start)
  start                         = start + Srcop.op_width
  io.src2_op                    := decoded(start + Srcop.op_width - 1, start)
}

class imm_gen extends Module{
    val io = IO(new Bundle{
        val inst                = Input (UInt(32.W))
        val inst_type           = Input (UInt(InstType.type_width.W))
        val imm                 = Output(UInt(32.W))
    })
    val imm_i                   = io.inst(31, 20)
    val imm_s                   = Cat(io.inst(31, 25), io.inst(11, 7))
    val imm_b                   = Cat(io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W))
    val imm_u                   = Cat(io.inst(31, 12), 0.U(12.W))
    val imm_j                   = Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W))
    io.imm                      := 0.U
    switch(io.inst_type){
        is(InstType.I){io.imm   := Cat(Fill(20, imm_i(11)), imm_i)}
        is(InstType.S){io.imm   := Cat(Fill(20, imm_s(11)), imm_s)}
        is(InstType.B){io.imm   := Cat(Fill(19, imm_b(12)), imm_b)}
        is(InstType.U){io.imm   := imm_u}
        is(InstType.J){io.imm   := Cat(Fill(11, imm_j(20)), imm_j)}
    }
}

class branch_ctrl extends Module{
    val io = IO(new Bundle{
        val src1                = Input (UInt(32.W))
        val src2                = Input (UInt(32.W))
        val pc                  = Input (UInt(32.W))
        val imm                 = Input (UInt(32.W))
        val branch_op           = Input (UInt(Branchop.op_width.W))
        val take_branch         = Output(Bool())
        val branch_target       = Output(UInt(32.W))
    })
    io.take_branch := false.B
    switch(io.branch_op){
        is(Branchop.beq){io.take_branch     := io.src1 === io.src2}
        is(Branchop.bne){io.take_branch     := io.src1 =/= io.src2}
        is(Branchop.blt){io.take_branch     := (io.src1.asSInt < io.src2.asSInt)}
        is(Branchop.bge){io.take_branch     := (io.src1.asSInt >= io.src2.asSInt)}
        is(Branchop.bltu){io.take_branch    := (io.src1 < io.src2)}
        is(Branchop.bgeu){io.take_branch    := (io.src1 >= io.src2)}
        is(Branchop.jal){io.take_branch     := true.B}
        is(Branchop.jalr){io.take_branch    := true.B}
    }
    io.branch_target := Mux(io.branch_op === Branchop.jalr,(io.src1 + io.imm) & ~1.U,io.pc + io.imm)
}