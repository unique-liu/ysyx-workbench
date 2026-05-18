import chisel3._
import chisel3.util._
import chisel3.ExtModule


class MemIO extends ExtModule {
  val io = IO(new Bundle {
    val clock = Input (Bool())
    val PC    = Input (UInt(32.W))
    val ren   = Input (Bool())
    val raddr = Input (UInt(32.W))
    val rdata = Output(UInt(32.W))

    val wen   = Input (Bool())
    val waddr = Input (UInt(32.W))
    val wdata = Input (UInt(32.W))
    val wmask = Input (UInt(4.W))
  })

}

class Mem_AXI extends Module {
  val io = IO(new Bundle {
    // Debug interface
    val rPC    = Input (UInt(32.W))
    val wPC    = Input (UInt(32.W))
    // AXI4-Lite interface
    val axi = Flipped(new AXI4Lite)
  })
  // declarations
  val r_data = RegInit(0.U(32.W))
  val r_resp = RegInit(0.U(2.W))
  val w_resp = RegInit(0.U(2.W))
  // axi slave
  val axi_slave = Module(new AXI_Slave())
  axi_slave.io.rPC := io.rPC
  axi_slave.io.wPC := io.wPC
  axi_slave.io.axi <> io.axi

  // memory interface logic
  val m_idle :: m_get :: m_wait :: Nil  = Enum(3)
  // delay for memio, simulating memory access latency
  val u_lfsr = Module(new LFSR(4))

  //use two memio to support one read and one write at the same time
  val mem_fsm_r = RegInit(m_idle)
  val mem_latch_r = RegInit(0.U(2.W))
  val mem_fsm_w = RegInit(m_idle)
  val mem_latch_w = RegInit(0.U(2.W))

  val u_memio_r = Module(new MemIO())
  val u_memio_w = Module(new MemIO())
  // read part
  u_memio_r.io.clock  := clock.asBool
  u_memio_r.io.PC     := axi_slave.io.rPC_out
  u_memio_r.io.ren    := axi_slave.io.r_req.valid && (mem_fsm_r === m_idle)
  u_memio_r.io.raddr  := axi_slave.io.r_req.addr
  u_memio_r.io.wen    := false.B
  u_memio_r.io.waddr  := 0.U(32.W)
  u_memio_r.io.wdata  := 0.U(32.W)
  u_memio_r.io.wmask  := 0.U(4.W)

  axi_slave.io.r_resp.valid  := false.B
  axi_slave.io.r_resp.data   := 0.U(32.W)
  axi_slave.io.r_resp.resp   := 0.U(2.W)
  switch(mem_fsm_r){
    is(m_idle){
      when(axi_slave.io.r_req.valid){
        mem_fsm_r := m_get
      }
    }
    is(m_get){
      r_data := u_memio_r.io.rdata
      r_resp := 0.U(2.W) //TODO: set response
      mem_latch_r := u_lfsr.io.out(1,0)
      mem_fsm_r := m_wait
    }
    is(m_wait){
      when(mem_latch_r === 0.U){
        axi_slave.io.r_resp.valid  := 1.B
        axi_slave.io.r_resp.data   := r_data
        axi_slave.io.r_resp.resp   := r_resp
        mem_fsm_r := Mux(axi_slave.io.r_resp.ready,m_idle,m_wait)
      }.otherwise{
        mem_latch_r := mem_latch_r - 1.U
      }
    }
  }
  //write part
  u_memio_w.io.clock  := clock.asBool
  u_memio_w.io.PC     := axi_slave.io.wPC_out
  u_memio_w.io.ren    := false.B
  u_memio_w.io.raddr  := 0.U(32.W)
  u_memio_w.io.wen    := axi_slave.io.w_req.valid && (mem_fsm_w === m_idle)
  u_memio_w.io.waddr  := axi_slave.io.w_req.addr
  u_memio_w.io.wdata  := axi_slave.io.w_req.data
  u_memio_w.io.wmask  := axi_slave.io.w_req.strb

  axi_slave.io.w_resp.valid  := false.B
  axi_slave.io.w_resp.resp   := 0.U(2.W)
  switch(mem_fsm_w){
    is(m_idle){
      when(axi_slave.io.w_req.valid){
        mem_fsm_w := m_get
      }
    }
    is(m_get){
      w_resp := 0.U(2.W) //TODO: set response
      mem_latch_w := u_lfsr.io.out(1,0)
      mem_fsm_w := m_wait
    }
    is(m_wait){
      when(mem_latch_w === 0.U){
        axi_slave.io.w_resp.valid  := 1.B
        axi_slave.io.w_resp.resp   := w_resp
        mem_fsm_w := Mux(axi_slave.io.w_resp.ready,m_idle,m_wait)
      }.otherwise{
        mem_latch_w := mem_latch_w - 1.U
      }
    }
  }
}