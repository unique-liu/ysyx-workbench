import chisel3._
import chisel3.util.BitPat

object CSRCode{
    val code_width  = 12
    val mcycle      = 0xB00.U(code_width.W)
    val mcycleh     = 0xB80.U(code_width.W)

    val mvendorid   = 0xF11.U(code_width.W)
    val marchid     = 0xF12.U(code_width.W)

    val mstatus     = 0x300.U(code_width.W)
    val mtvec       = 0x305.U(code_width.W)
    val mepc        = 0x341.U(code_width.W)
    val mcause      = 0x342.U(code_width.W)
}
object CSRPRIV{
    val U           = 0
    val S           = 1
    val M           = 3
}
object CSRMcauseCode{
    val ecall_from_u = 8.U
    val ecall_from_s = 9.U
    val ecall_from_m = 11.U
}