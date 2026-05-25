import chisel3._
import chisel3.util._

object D_MEM{
    val addr_begin  = "h80000000".U(32.W)
    val addr_end    = "h80ffffff".U(32.W) 
    
}
object D_UART{
    val idx         = 0.U(32.W)
    val addr_begin  = "h10000000".U(32.W)
    val addr_end    = "h10000fff".U(32.W)
}
object D_CLINT{
    val idx         = 1.U(32.W)
    val addr_begin  = "h02000000".U(32.W)
    val addr_end    = "h0200bfff".U(32.W)
}

class DeviceIO extends ExtModule {
  val io = IO(new Bundle {
    val clock   = Input (Bool())
    val idx     = Input (UInt(32.W))
    val PC      = Input (UInt(32.W))
    val ren     = Input (Bool())
    val raddr   = Input (UInt(32.W))
    val rdata   = Output(UInt(32.W))

    val wen     = Input (Bool())
    val waddr   = Input (UInt(32.W))
    val wdata   = Input (UInt(32.W))
    val wmask   = Input (UInt(4.W))
  })
}

class DiffSkip extends ExtModule {
  val io = IO(new Bundle {
    val clock   = Input (Bool())
    val en      = Input (Bool())
    val PC      = Input (UInt(32.W))
    val addr    = Input (UInt(32.W))
    val idx     = Input (UInt(32.W))
  })
}



class CLINT extends Module {
    val io = IO(new Bundle {
        val rPC     = Input (UInt(32.W))
        val wPC     = Input (UInt(32.W))
        val axi     = Flipped(new AXI4)
    })
    // declarations
    val c_idle :: c_write :: c_read :: Nil = Enum(3)
    val c_fsm       = RegInit(c_idle) 
    val mtime       = RegInit(0.U(64.W))
    // val u_diffskip  = Module(new DiffSkip())
    // axi slave
    val axi_slave   = Module(new AXI_Slave())
    val r_data      = RegInit(0.U(32.W))
    axi_slave.io.rPC := io.rPC
    axi_slave.io.wPC := io.wPC
    axi_slave.io.axi <> io.axi
    axi_slave.io.r_resp.data := r_data
    axi_slave.io.r_resp.resp := 0.U(2.W) // OKAY
    axi_slave.io.w_resp.resp := 0.U(2.W) // OKAY
    axi_slave.io.r_resp.valid := c_fsm === c_read
    axi_slave.io.w_resp.valid := c_fsm === c_write

    // CLINT logic 
    mtime := mtime + 1.U
    // diff skip
    // u_diffskip.io.clock     := clock.asBool
    // u_diffskip.io.idx       := D_CLINT.idx
    // u_diffskip.io.en        := 0.U(1.W) 
    // u_diffskip.io.PC        := 0.U(32.W)
    // u_diffskip.io.addr      := 0.U(32.W)
    switch(c_fsm){
        is(c_idle){
            when(axi_slave.io.r_req.valid){
                c_fsm               := c_read
                r_data              := Mux(axi_slave.io.r_req.addr(7,0)===0xf8.U(8.W), mtime(31,0), mtime(63,32))
                // u_diffskip.io.en    := 1.U
                // u_diffskip.io.PC    := axi_slave.io.rPC_out
                // u_diffskip.io.addr  := axi_slave.io.r_req.addr
            }.elsewhen(axi_slave.io.w_req.valid){
                c_fsm := c_write
                // u_diffskip.io.en    := 1.U
                // u_diffskip.io.PC    := axi_slave.io.wPC_out
                // u_diffskip.io.addr  := axi_slave.io.w_req.addr
            }
        }
        is(c_read){
            when(axi_slave.io.r_resp.ready){
                r_data  := 0.U(32.W) // clear r_data after read
                c_fsm   := c_idle
            }
        }
        is(c_write){
            when(axi_slave.io.w_resp.ready){
                c_fsm := c_idle
            }
        }
    }
  
}