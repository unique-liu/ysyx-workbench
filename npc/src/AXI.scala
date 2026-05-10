import chisel3._
import chisel3.util.Enum

class AXI4Lite extends Bundle {
  val awvalid = Output(Bool())
  val awready = Input(Bool())
  val awaddr  = Output(UInt(32.W))

  val wvalid  = Output(Bool())
  val wready  = Input(Bool())
  val wdata   = Output(UInt(32.W))
  val wstrb   = Output(UInt(4.W))

  val arvalid = Output(Bool())
  val arready = Input(Bool())
  val araddr  = Output(UInt(32.W))

  val rvalid  = Input(Bool())
  val rready  = Output(Bool())
  val rdata   = Input(UInt(32.W))
  val rresp   = Input(UInt(2.W))

  val bvalid  = Input(Bool())
  val bready  = Output(Bool())
  val bresp   = Input(UInt(2.W))
}

object AXI_FSM{
    val width = 2;
    val aw_idle :: aw_wait :: Nil = Enum(2)//idle: no aw request, wait: get aw request, wait for work done
    val w_idle :: w_wait :: Nil  = Enum(2)
    val ar_idle :: ar_wait :: Nil  = Enum(2)
    val r_idle :: r_wait :: r_resp :: Nil  = Enum(3)//idle: no ar request, wait: have ar request, wait for rdata, resp: have rdata, wait for master to accept 
    val b_idle :: b_wait :: b_resp :: Nil  = Enum(3)

}