import chisel3._
import chisel3.ExtModule


class MemIO extends ExtModule {
  val io = IO(new Bundle {
    val clock = Input (Bool())
    val pc    = Input (UInt(32.W))
    val ren   = Input (Bool())
    val raddr = Input (UInt(32.W))
    val rdata = Output(UInt(32.W))

    val wen   = Input (Bool())
    val waddr = Input (UInt(32.W))
    val wdata = Input (UInt(32.W))
    val wmask = Input (UInt(4.W))
  })

}