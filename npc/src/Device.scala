import chisel3._
import chisel3.util._

object D_MEM{
    val addr_begin  = "h80000000".U(32.W)
    val addr_end    = "h80ffffff".U(32.W) 
}
object D_UART{
    val addr_begin  = "h10000000".U(32.W)
    val addr_end    = "h10000fff".U(32.W)
}
object D_CLINT{
    val addr_begin  = "h02000000".U(32.W)
    val addr_end    = "h0200bfff".U(32.W)
}

// class UART extends Module {
//     val io = IO(new Bundle {
//         val rPC    = Input (UInt(32.W))
//         val wPC    = Input (UInt(32.W))
//         val axi = Flipped(new AXI4Lite)
//     })

// }