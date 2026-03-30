import chisel3._
import chisel3.ExtModule


class SpecialIO extends ExtModule {
  val io = IO(new Bundle {
    val halt  = Input (Bool())
    val error = Input (Bool())
  })

}