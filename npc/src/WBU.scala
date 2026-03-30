import chisel3._
import chisel3.util._
import Memop.l_byte_s

class WBU extends Module{
    val io = IO(new Bundle{
        val before = new Bundle{
            val valid           = Input (Bool())
            val ready           = Output(Bool())
            val alu_result      = Input (UInt(32.W))
            val reg_op          = Input (UInt(Regop.op_width.W))
            val reg_rd          = Input (UInt(5.W))
            val mem_result      = Input (UInt(32.W))
        }
        val next = new Bundle{
            val valid           = Output(Bool())
            val ready           = Input (Bool())
        }
        val regfile = new Bundle{
            val waddr           = Output(UInt(5.W))
            val wdata           = Output(UInt(32.W))
            val wen             = Output(Bool())
        }
    })
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
    val reg_reg_op              = Reg(UInt(32.W))   
    val reg_reg_rd              = Reg(UInt(5.W))
    val reg_mem_result          = Reg(UInt(32.W))   

    when(will_in){
        reg_alu_result          := io.before.alu_result
        reg_reg_op              := io.before.reg_op  
        reg_reg_rd              := io.before.reg_rd
        reg_mem_result          := io.before.mem_result
    }

    //regfile write back
    io.regfile.wen              := reg_reg_op(Regop.write_bit) & valid
    io.regfile.waddr            := reg_reg_rd
    io.regfile.wdata            := Mux(reg_reg_op(Regop.mem_bit),reg_mem_result,reg_alu_result)

}