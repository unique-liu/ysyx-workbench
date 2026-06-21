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
            val PC              = Output(UInt(32.W))
            val alu_src1        = Output(UInt(32.W))
            val alu_src2        = Output(UInt(32.W))
            val alu_op          = Output(UInt(ALUop.op_width.W))
            val reg_op          = Output(UInt(Regop.op_width.W))
            val reg_rd          = Output(UInt(5.W))
            val mem_op          = Output(UInt(Memop.op_width.W))
            val mem_src         = Output(UInt(32.W))
            val debug           = Output(new debug)
            val CSR_info        = Output(new CSR_info)
        }
        val regfile = new Bundle{
            val raddr1          = Output(UInt(5.W))
            val raddr2          = Output(UInt(5.W))
            val rdata1          = Input (UInt(32.W))
            val rdata2          = Input (UInt(32.W))
        }
        val EXU_forward = new Bundle{
            val reg_wdata       = Input (UInt(32.W))
            val reg_rd          = Input (UInt(5.W))
            val reg_useable     = Input (Bool())
        }
        val LSU_forward = new Bundle{
            val reg_wdata       = Input (UInt(32.W))
            val reg_rd          = Input (UInt(5.W))
            val reg_useable     = Input (Bool())
        }
        val WBU_forward = new Bundle{
            val reg_wdata       = Input (UInt(32.W))
            val reg_rd          = Input (UInt(5.W))
            val reg_useable     = Input (Bool())
        }
        val csr_read = new Bundle{
            val addr        = Output(UInt(12.W))
            val rdata       = Input(UInt(32.W))
        }
        val flush           = Input(Bool())
        val fencei          = Output(Bool())
    })
    //dclarations
    val rs1_stall               = Wire(Bool())
    val rs2_stall               = Wire(Bool())
    val branch_taken            = Wire(Bool())

    //fluiding control signals
    val valid                   = RegInit(0.U(1.W))
    val will_out                = Wire(Bool())
    val will_in                 = Wire(Bool())
    when(io.flush){
        valid                   := 0.U(1.W)
    }.elsewhen(will_in){
        valid                   := 1.U(1.W)
    }.elsewhen(will_out){
        valid                   := 0.U(1.W)
    }
    will_out                    := io.next.ready & io.next.valid 
    will_in                     := io.before.valid & io.before.ready
    io.before.ready             := (!valid | will_out) 
    io.next.valid               := valid & !rs1_stall & !rs2_stall

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

    val rs1_need_EXU_forward      = rs1 =/= 0.U && (rs1 === io.EXU_forward.reg_rd)
    val rs1_need_LSU_forward      = rs1 =/= 0.U && (rs1 === io.LSU_forward.reg_rd)
    val rs1_need_WBU_forward      = rs1 =/= 0.U && (rs1 === io.WBU_forward.reg_rd)
    val rs2_need_EXU_forward      = rs2 =/= 0.U && (rs2 === io.EXU_forward.reg_rd)
    val rs2_need_LSU_forward      = rs2 =/= 0.U && (rs2 === io.LSU_forward.reg_rd)
    val rs2_need_WBU_forward      = rs2 =/= 0.U && (rs2 === io.WBU_forward.reg_rd)
    val rs1_forward_need          = (rs1_need_EXU_forward || rs1_need_LSU_forward || rs1_need_WBU_forward)
    val rs2_forward_need          = (rs2_need_EXU_forward || rs2_need_LSU_forward || rs2_need_WBU_forward)
    val rs1_forward_valid         = Mux(rs1_need_EXU_forward, io.EXU_forward.reg_useable, Mux(rs1_need_LSU_forward, io.LSU_forward.reg_useable, Mux(rs1_need_WBU_forward, io.WBU_forward.reg_useable, false.B)))
    val rs2_forward_valid         = Mux(rs2_need_EXU_forward, io.EXU_forward.reg_useable, Mux(rs2_need_LSU_forward, io.LSU_forward.reg_useable, Mux(rs2_need_WBU_forward, io.WBU_forward.reg_useable, false.B)))
    val rs1_forward_data          = Mux(rs1_need_EXU_forward, io.EXU_forward.reg_wdata, Mux(rs1_need_LSU_forward, io.LSU_forward.reg_wdata, Mux(rs1_need_WBU_forward, io.WBU_forward.reg_wdata, 0.U(32.W))))
    val rs2_forward_data          = Mux(rs2_need_EXU_forward, io.EXU_forward.reg_wdata, Mux(rs2_need_LSU_forward, io.LSU_forward.reg_wdata, Mux(rs2_need_WBU_forward, io.WBU_forward.reg_wdata, 0.U(32.W))))
    rs1_stall                     := rs1_forward_need && !rs1_forward_valid
    rs2_stall                     := rs2_forward_need && !rs2_forward_valid
    val rs1_data                  = Mux(rs1_forward_need, rs1_forward_data, io.regfile.rdata1)
    val rs2_data                  = Mux(rs2_forward_need, rs2_forward_data, io.regfile.rdata2)

    //decoder
    val inst_decoder            = Module(new inst_decoder)
    inst_decoder.io.inst        := regInst
    io.next.alu_op              := inst_decoder.io.alu_op
    io.next.reg_op              := inst_decoder.io.reg_op
    io.next.reg_rd              := regInst(11, 7)
    io.next.mem_op              := inst_decoder.io.mem_op

    
    val it_code                 = Wire(UInt(8.W))
    if(Config.perf_on){
        //perf-it
        val perf_it                 = Module(new perf(PT.it))
        it_code                     := PT.it_inv
        when(inst_decoder.io.csr_op =/= CSRop.noop){
            when(inst_decoder.io.csr_op(CSRop.int_bit)){
                it_code := PT.it_crt
            }.otherwise{
                it_code := PT.it_csr
            }
        }.elsewhen(inst_decoder.io.branch_op =/= Branchop.noop){
            it_code := PT.it_b
        }.elsewhen(inst_decoder.io.mem_op =/= Memop.noop){
            when(inst_decoder.io.mem_op(Memop.load_bit)){
                it_code := PT.it_l
            }.otherwise{
                it_code := PT.it_s
            }
        }.elsewhen(inst_decoder.io.special_op =/= Specialop.noop){
            it_code := PT.it_crt
        }.otherwise{
            it_code := PT.it_c
        }
        perf_it.io.valid            := io.next.valid
        perf_it.io.code             := Cat(io.next.ready,it_code(6,0))// high bit indicates a new instruction
    }

    //imm generation
    val inst_type                = inst_decoder.io.inst_type
    val imm_gen                  = Module(new imm_gen)
    imm_gen.io.inst              := regInst
    imm_gen.io.inst_type         := inst_type
    val imm                      = imm_gen.io.imm

    //branch control
    val branch_ctrl              = Module(new branch_ctrl)
    branch_taken                 := branch_ctrl.io.take_branch & will_out
    branch_ctrl.io.src1          := rs1_data
    branch_ctrl.io.src2          := rs2_data
    branch_ctrl.io.pc            := regPC
    branch_ctrl.io.imm           := imm
    branch_ctrl.io.branch_op     := inst_decoder.io.branch_op
    io.before.branchPC           := branch_ctrl.io.branch_target
    io.before.ifbranch           := branch_taken

    io.fencei                    := inst_decoder.io.branch_op === Branchop.fencei & will_out
    //ALU source selection
    val src1_op                  = inst_decoder.io.src1_op
    val src2_op                  = inst_decoder.io.src2_op
    io.next.alu_src1             := 0.U
    io.next.alu_src2             := 0.U
    switch(src1_op){
        is(Srcop.use_zero){io.next.alu_src1 := 0.U}
        is(Srcop.use_reg){io.next.alu_src1 := rs1_data}
        is(Srcop.use_imm){io.next.alu_src1 := imm}
        is(Srcop.use_pc){io.next.alu_src1 := regPC}
        is(Srcop.use_four){io.next.alu_src1 := 4.U}
        is(Srcop.use_csr){io.next.alu_src1 := io.csr_read.rdata}
    }
    switch(src2_op){
        is(Srcop.use_zero){io.next.alu_src2 := 0.U}
        is(Srcop.use_reg){io.next.alu_src2 := rs2_data}
        is(Srcop.use_imm){io.next.alu_src2 := imm}
        is(Srcop.use_pc){io.next.alu_src2 := regPC}
        is(Srcop.use_four){io.next.alu_src2 := 4.U}
        is(Srcop.use_csr){io.next.alu_src2 := io.csr_read.rdata}
    }
    io.next.mem_src              := rs2_data

    //CSR related
    io.csr_read.addr             := Mux(inst_decoder.io.csr_op(CSRop.special_bit) | inst_decoder.io.csr_op(CSRop.int_bit), 0.U, imm(11, 0))
    io.next.CSR_info.addr        := Mux(inst_decoder.io.csr_op(CSRop.special_bit) | inst_decoder.io.csr_op(CSRop.int_bit), 0.U, imm(11, 0))
    io.next.CSR_info.wdata       := Mux(inst_decoder.io.csr_op(CSRop.imm_bit),Cat(Fill(32 - rs1.getWidth,0.U),rs1),rs1_data)
    io.next.CSR_info.op          := Mux(valid.asBool,inst_decoder.io.csr_op, CSRop.noop)
    io.next.CSR_info.exception   := valid & (inst_decoder.io.csr_op(CSRop.int_bit) | inst_decoder.io.csr_op(CSRop.special_bit))
    
    //normal output
    io.next.PC                    := regPC
    io.next.debug.inst            := regInst
    io.next.debug.branch          := branch_taken & inst_decoder.io.branch_op(Branchop.jump_bit)//this is used to control ftrace, so only jump instruction
    io.next.debug.branch_target   := branch_ctrl.io.branch_target
    io.next.debug.it_code         := it_code

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
        val csr_op              = Output(UInt(CSRop.op_width.W))
    })


    def concatBitPat(parts: UInt*): BitPat = {
        if (parts.isEmpty) BitPat("b")
        val expected = InstType.type_width + ALUop.op_width + Regop.op_width + Memop.op_width +
        Branchop.op_width + Srcop.op_width + Srcop.op_width + Specialop.op_width + CSRop.op_width
        require(parts.map(_.getWidth).sum == expected, s"width mismatch")
        val bitStr = parts.reverse.map { p =>
            p.litOption match {
                case Some(v) =>
                val s = v.bigInteger.toString(2)
                "0" * (p.getWidth - s.length) + s
                case None => throw new Exception(s"concatBitPat: non-literal part (width=${p.getWidth})")
            }
        }.mkString
        BitPat("b" + bitStr)
    }

    val table = TruthTable(
        Map(
            //R-type
            InstCode.add     -> concatBitPat(InstType.R, ALUop.add, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.sub     -> concatBitPat(InstType.R, ALUop.sub, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.sll     -> concatBitPat(InstType.R, ALUop.sll, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.slt     -> concatBitPat(InstType.R, ALUop.slt, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.sltu    -> concatBitPat(InstType.R, ALUop.sltu,Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.xor     -> concatBitPat(InstType.R, ALUop.xor, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.srl     -> concatBitPat(InstType.R, ALUop.srl, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.sra     -> concatBitPat(InstType.R, ALUop.sra, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.or      -> concatBitPat(InstType.R, ALUop.or , Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.and     -> concatBitPat(InstType.R, ALUop.and, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),

            //I-type
            InstCode.jalr    -> concatBitPat(InstType.I, ALUop.add, Regop.w_alu, Memop.noop    , Branchop.jalr, Srcop.use_pc  , Srcop.use_four,Specialop.noop, CSRop.noop),

            InstCode.addi    -> concatBitPat(InstType.I, ALUop.add, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.slti    -> concatBitPat(InstType.I, ALUop.slt, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.sltiu   -> concatBitPat(InstType.I, ALUop.sltu,Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.xori    -> concatBitPat(InstType.I, ALUop.xor, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.ori     -> concatBitPat(InstType.I, ALUop.or , Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.andi    -> concatBitPat(InstType.I, ALUop.and, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.slli    -> concatBitPat(InstType.I, ALUop.sll, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.srli    -> concatBitPat(InstType.I, ALUop.srl, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.srai    -> concatBitPat(InstType.I, ALUop.sra, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),

            InstCode.lb      -> concatBitPat(InstType.I, ALUop.add, Regop.w_mem, Memop.l_byte_s, Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.lh      -> concatBitPat(InstType.I, ALUop.add, Regop.w_mem, Memop.l_half_s, Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.lw      -> concatBitPat(InstType.I, ALUop.add, Regop.w_mem, Memop.l_word  , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.lbu     -> concatBitPat(InstType.I, ALUop.add, Regop.w_mem, Memop.l_byte_u, Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.lhu     -> concatBitPat(InstType.I, ALUop.add, Regop.w_mem, Memop.l_half_u, Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            
            InstCode.csrrw   -> concatBitPat(InstType.I, ALUop.add, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_csr , Srcop.use_zero,Specialop.noop, CSRop.csrrw),
            InstCode.csrrs   -> concatBitPat(InstType.I, ALUop.add, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_csr , Srcop.use_zero,Specialop.noop, CSRop.csrrs),
            InstCode.ecall   -> concatBitPat(InstType.I, ALUop.add, Regop.noop , Memop.noop    , Branchop.noop, Srcop.use_zero, Srcop.use_zero,Specialop.noop, CSRop.ecall),
            InstCode.ebreak  -> concatBitPat(InstType.I, ALUop.add, Regop.noop , Memop.noop    , Branchop.noop, Srcop.use_zero, Srcop.use_zero,Specialop.noop, CSRop.ebreak),
            InstCode.mret    -> concatBitPat(InstType.I, ALUop.add, Regop.noop , Memop.noop    , Branchop.noop, Srcop.use_zero, Srcop.use_zero,Specialop.noop, CSRop.mret),
            InstCode.fencei  -> concatBitPat(InstType.I, ALUop.add, Regop.noop , Memop.noop    , Branchop.fencei,Srcop.use_zero,Srcop.use_zero,Specialop.noop, CSRop.noop),
            //S-type
            InstCode.sb      -> concatBitPat(InstType.S, ALUop.add, Regop.noop , Memop.s_byte  , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.sh      -> concatBitPat(InstType.S, ALUop.add, Regop.noop , Memop.s_half  , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            InstCode.sw      -> concatBitPat(InstType.S, ALUop.add, Regop.noop , Memop.s_word  , Branchop.noop, Srcop.use_reg , Srcop.use_imm ,Specialop.noop, CSRop.noop),
            //B-type
            InstCode.beq     -> concatBitPat(InstType.B, ALUop.add, Regop.noop , Memop.noop    , Branchop.beq , Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.bne     -> concatBitPat(InstType.B, ALUop.add, Regop.noop , Memop.noop    , Branchop.bne , Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.blt     -> concatBitPat(InstType.B, ALUop.add, Regop.noop , Memop.noop    , Branchop.blt , Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.bge     -> concatBitPat(InstType.B, ALUop.add, Regop.noop , Memop.noop    , Branchop.bge , Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.bltu    -> concatBitPat(InstType.B, ALUop.add, Regop.noop , Memop.noop    , Branchop.bltu, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            InstCode.bgeu    -> concatBitPat(InstType.B, ALUop.add, Regop.noop , Memop.noop    , Branchop.bgeu, Srcop.use_reg , Srcop.use_reg ,Specialop.noop, CSRop.noop),
            //U-type
            InstCode.lui     -> concatBitPat(InstType.U, ALUop.add, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_imm , Srcop.use_zero,Specialop.noop, CSRop.noop),
            InstCode.auipc   -> concatBitPat(InstType.U, ALUop.add, Regop.w_alu, Memop.noop    , Branchop.noop, Srcop.use_imm , Srcop.use_pc  ,Specialop.noop, CSRop.noop),
            //J-type
            InstCode.jal     -> concatBitPat(InstType.J, ALUop.add, Regop.w_alu, Memop.noop    , Branchop.jal , Srcop.use_pc  , Srcop.use_four ,Specialop.noop, CSRop.noop)
        ),

        concatBitPat(InstType.Invalid, ALUop.add, Regop.noop, Memop.noop, Branchop.noop, Srcop.use_zero, Srcop.use_zero, Specialop.noop, CSRop.inv_inst)

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
    start                         = start + Srcop.op_width
    io.special_op                 := decoded(start + Specialop.op_width - 1, start) 
    start                         = start + Specialop.op_width
    io.csr_op                     := decoded(start + CSRop.op_width - 1, start)
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
        is(Branchop.fencei){io.take_branch  := true.B}
    }
    io.branch_target := Mux(io.branch_op === Branchop.jalr,(io.src1 + io.imm) & (~1.U(32.W)),
                        Mux(io.branch_op === Branchop.fencei, io.pc + 4.U, io.pc + io.imm))
}