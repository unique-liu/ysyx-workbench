import chisel3._
import chisel3.util._
import Memop.l_byte_s

class WBU extends Module{
    val io = IO(new Bundle{
        val before = new Bundle{
            val valid           = Input (Bool())
            val ready           = Output(Bool())
            val PC              = Input (UInt(32.W))
            val alu_result      = Input (UInt(32.W))
            val reg_op          = Input (UInt(Regop.op_width.W))
            val reg_rd          = Input (UInt(5.W))
            val mem_result      = Input (UInt(32.W))
            val debug = new Bundle{
                val inst            = Input (UInt(32.W))
                val branch          = Input (Bool())
                val branch_target   = Input (UInt(32.W))
            }
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
        val forward = new Bundle{
            val reg_wdata       = Output(UInt(32.W))
            val reg_rd          = Output(UInt(5.W))
            val reg_useable     = Output(Bool())
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
    val reg_PC                  = Reg(UInt(32.W))
    val reg_alu_result          = Reg(UInt(32.W))   
    val reg_reg_op              = Reg(UInt(32.W))   
    val reg_reg_rd              = Reg(UInt(5.W))
    val reg_mem_result          = Reg(UInt(32.W))   
    val reg_debug               = Reg(new debug)

    when(will_in){
        reg_PC                  := io.before.PC
        reg_alu_result          := io.before.alu_result
        reg_reg_op              := io.before.reg_op  
        reg_reg_rd              := io.before.reg_rd
        reg_mem_result          := io.before.mem_result
        reg_debug               := io.before.debug
    }

    //regfile write back
    io.regfile.wen              := reg_reg_op(Regop.write_bit) & valid
    io.regfile.waddr            := reg_reg_rd
    io.regfile.wdata            := Mux(reg_reg_op(Regop.mem_bit),reg_mem_result,reg_alu_result)

    //forwarding
    io.forward.reg_wdata        := io.regfile.wdata
    io.forward.reg_rd           := Mux(reg_reg_op(Regop.write_bit) && (valid === 1.U),reg_reg_rd,0.U(5.W))
    io.forward.reg_useable      := valid

    //debug
    val u_debugio               = Module(new DebugIO)
    u_debugio.io.clock          := clock.asBool
    u_debugio.io.pc             := reg_PC
    u_debugio.io.inst           := reg_debug.inst
    dontTouch(u_debugio.io.submit)
    u_debugio.io.submit         := will_out 
    u_debugio.io.rd             := io.regfile.waddr
    u_debugio.io.wdata          := io.regfile.wdata
    u_debugio.io.wen            := io.regfile.wen
    u_debugio.io.target         := reg_debug.branch_target
    u_debugio.io.rs1            := reg_debug.inst(19,15)
    u_debugio.io.branch         := reg_debug.branch

}