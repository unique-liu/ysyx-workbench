import chisel3._
import chisel3.util._

class LSU extends Module{
    val io = IO(new Bundle{
        val before = new Bundle{
            val valid           = Input (Bool())
            val ready           = Output(Bool())
            val alu_result      = Input (UInt(32.W))
            val reg_op          = Input (UInt(Regop.op_width.W))
            val mem_op          = Input (UInt(Memop.op_width.W))
            val mem_src         = Input (UInt(32.W))
        }
        val next = new Bundle{
            val valid           = Output(Bool())
            val ready           = Input (Bool())
            val alu_result      = Output(UInt(32.W))
            val reg_op          = Output(UInt(Regop.op_width.W))
            val mem_result      = Output(UInt(32.W))
        }
        val memio = new Bundle{
            val ren             = Output(Bool())
            val raddr           = Output(UInt(32.W))
            val rdata           = Input (UInt(32.W))

            val wen             = Output(Bool())
            val waddr           = Output(UInt(32.W))
            val wdata           = Output(UInt(32.W))
            val wmask           = Output(UInt(4.W))
        }
    })
    //fluiding control signals
    val valid                   = RegInit(0.U(1.W))
    val will_out                = Bool()
    val will_in                 = Bool()
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
    val reg_mem_op              = Reg(UInt(Memop.op_width.W))
    val reg_mem_mask            = Reg(UInt(4.W))
    val reg_mem_out             = Reg(UInt(32.W))
    when(will_in){
        reg_alu_result          := io.before.alu_result
        reg_reg_op              := io.before.reg_op  
        reg_mem_op              := io.before.mem_op
        reg_mem_mask            := mem_mask
        reg_mem_out             := io.memio.rdata
    }

    //memio
    val mem_mask                = Wire(UInt(4.W))
    mem_mask                    := 0.U
    switch(io.before.mem_op(Memop.half_bit,Memop.load_bit)){
        is("b01".U){mem_mask := "b0001".U}
        is("b10".U){mem_mask := "b0011".U}
        is("b11".U){mem_mask := "b1111".U}
    }
    io.memio.ren                := reg_mem_op(Memop.load_bit)
    io.memio.raddr              := reg_alu_result
    io.memio.wen                := ~reg_mem_op(Memop.load_bit) & reg_mem_op =/= Memop.noop
    io.memio.waddr              := reg_alu_result
    io.memio.wdata              := io.before.mem_src
    io.memio.wmask              := mem_mask

    //output
    io.next.alu_result          := reg_alu_result
    io.next.reg_op              := reg_reg_op
    io.next.mem_result          := reg_mem_out //need to be processed
}