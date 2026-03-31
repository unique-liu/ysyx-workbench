import chisel3._
import chisel3.util._
import Memop.l_byte_s

class LSU extends Module{
    val io = IO(new Bundle{
        val before = new Bundle{
            val valid           = Input (Bool())
            val ready           = Output(Bool())
            val alu_result      = Input (UInt(32.W))
            val reg_op          = Input (UInt(Regop.op_width.W))
            val reg_rd          = Input (UInt(5.W))
            val mem_op          = Input (UInt(Memop.op_width.W))
            val mem_src         = Input (UInt(32.W))
        }
        val next = new Bundle{
            val valid           = Output(Bool())
            val ready           = Input (Bool())
            val alu_result      = Output(UInt(32.W))
            val reg_op          = Output(UInt(Regop.op_width.W))
            val reg_rd          = Output(UInt(5.W))
            val mem_result      = Output(UInt(32.W))
        }
        val memio = new Bundle{
            val clock           = Output(Bool())
            val ren             = Output(Bool())
            val raddr           = Output(UInt(32.W))
            val rdata           = Input (UInt(32.W))

            val wen             = Output(Bool())
            val waddr           = Output(UInt(32.W))
            val wdata           = Output(UInt(32.W))
            val wmask           = Output(UInt(4.W))
        }
    })
    //dclarations
    val mem_mask                = Wire(UInt(4.W))
    //fluiding control signals
    val valid                   = RegInit(0.U(1.W))
    val will_out                = Wire(Bool())
    val will_in                 = Wire(Bool())
    when(will_in){
        valid                   := 1.U(1.W)
    }.elsewhen(will_out){
        valid                   := 0.U(1.W)
    }
    will_out                    := io.next.ready & io.next.valid
    will_in                     := io.before.valid & io.before.ready
    io.before.ready             := !valid | will_out
    io.next.valid               := valid

    //latching signals
    val reg_alu_result          = Reg(UInt(32.W))
    val reg_reg_op              = Reg(UInt(Regop.op_width.W))
    val reg_reg_rd              = Reg(UInt(5.W))
    val reg_mem_op              = Reg(UInt(Memop.op_width.W))
    val reg_mem_mask            = Reg(UInt(4.W))
    when(will_in){
        reg_alu_result          := io.before.alu_result
        reg_reg_op              := io.before.reg_op  
        reg_reg_rd              := io.before.reg_rd
        reg_mem_op              := io.before.mem_op
        reg_mem_mask            := mem_mask
    }

    //memio
    mem_mask                    := 0.U
    switch(io.before.mem_op(Memop.half_bit,Memop.byte_bit)){
        is("b01".U){mem_mask := "b0001".U}
        is("b10".U){mem_mask := "b0011".U}
        is("b11".U){mem_mask := "b1111".U}
    }
    io.memio.clock              := clock.asBool
    io.memio.ren                := reg_mem_op(Memop.load_bit) & will_in
    io.memio.raddr              := reg_alu_result
    io.memio.wen                := ~reg_mem_op(Memop.load_bit) & (reg_mem_op =/= Memop.noop) & will_in
    io.memio.waddr              := reg_alu_result
    io.memio.wdata              := io.before.mem_src
    io.memio.wmask              := mem_mask

    //output
    io.next.alu_result          := reg_alu_result
    io.next.reg_op              := reg_reg_op
    io.next.reg_rd              := reg_reg_rd

    val mem_out_aligned         = io.memio.rdata >> Cat(reg_alu_result(1,0),0.U(3.W))
    io.next.mem_result          := 0.U
    switch(reg_mem_op){
        is(Memop.l_byte_u){io.next.mem_result      := Cat(0.U(24.W),mem_out_aligned(7,0))}
        is(Memop.l_byte_s){io.next.mem_result      := Cat(Fill(24,mem_out_aligned(7)),mem_out_aligned(7,0))}
        is(Memop.l_half_u){io.next.mem_result      := Cat(0.U(16.W),mem_out_aligned(15,0))}
        is(Memop.l_half_s){io.next.mem_result      := Cat(Fill(16,mem_out_aligned(15)),mem_out_aligned(15,0))}
        is(Memop.l_word  ){io.next.mem_result      := mem_out_aligned}
    }
}