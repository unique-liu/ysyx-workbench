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
    // aw
    val aw_fsm = RegInit(AXI_FSM.aw_idle)
    val aw_addr = RegInit(0.U(32.W))
    val aw_PC = RegInit(0.U(32.W))// for debug
    // w
    val w_fsm = RegInit(AXI_FSM.w_idle)
    val w_data = RegInit(0.U(32.W))
    val w_strb = RegInit(0.U(4.W))
    // ar
    val ar_fsm = RegInit(AXI_FSM.ar_idle)
    val ar_addr = RegInit(0.U(32.W))
    val ar_PC = RegInit(0.U(32.W))// for debug
    // r
    val r_fsm = RegInit(AXI_FSM.r_idle)
    val r_data = RegInit(0.U(32.W))
    val r_resp = RegInit(0.U(2.W))
    val read_done = Wire(Bool())
    // b
    val b_fsm = RegInit(AXI_FSM.b_idle)
    val b_resp = RegInit(0.U(2.W))
    val write_done = Wire(Bool())
  // aw
  switch(aw_fsm){
    is(AXI_FSM.aw_idle){
      when(io.axi.awvalid){//ready is ensured by its assignment
        aw_fsm := AXI_FSM.aw_wait
        aw_addr := io.axi.awaddr
        aw_PC := io.wPC
      }
    }
    is(AXI_FSM.aw_wait){
      when(write_done){//wait for work done
        aw_fsm := AXI_FSM.aw_idle
        aw_addr := 0.U(32.W)
        aw_PC := 0.U(32.W)
      }
    }
  }
  io.axi.awready := (aw_fsm === AXI_FSM.aw_idle)

  //w
  switch(w_fsm){
    is(AXI_FSM.w_idle){
      when(io.axi.wvalid){
        w_fsm := AXI_FSM.w_wait
        w_data := io.axi.wdata
        w_strb := io.axi.wstrb
      }
    }
    is(AXI_FSM.w_wait){
      when(write_done){//wait for work done
        w_fsm := AXI_FSM.w_idle
        w_data := 0.U(32.W)
        w_strb := 0.U(4.W)
      }
    }
  }
  io.axi.wready := (w_fsm === AXI_FSM.w_idle)

  //ar
  switch(ar_fsm){
    is(AXI_FSM.ar_idle){
      when(io.axi.arvalid){
        ar_fsm := AXI_FSM.ar_wait   
        ar_addr := io.axi.araddr
        ar_PC := io.rPC
      }
    }
    is(AXI_FSM.ar_wait){
      when(read_done){//wait for rdata
        ar_fsm := AXI_FSM.ar_idle
        ar_addr := 0.U(32.W)
        ar_PC := 0.U(32.W)
      }
    }
  }
  io.axi.arready := (ar_fsm === AXI_FSM.ar_idle)

  //r
  switch(r_fsm){
    is(AXI_FSM.r_idle){
      when(ar_fsm === AXI_FSM.ar_wait){
        r_fsm := AXI_FSM.r_wait
      }
    }
    is(AXI_FSM.r_wait){
      when(read_done){
        r_fsm := AXI_FSM.r_resp
      }
    }
    is(AXI_FSM.r_resp){
      when(io.axi.rready){
        r_fsm := AXI_FSM.r_idle
      }
    }
  }
  io.axi.rvalid := (r_fsm === AXI_FSM.r_resp)
  io.axi.rdata := r_data
  io.axi.rresp := r_resp

  //b
  switch(b_fsm){
    is(AXI_FSM.b_idle){
      when(aw_fsm === AXI_FSM.aw_wait && w_fsm === AXI_FSM.w_wait){
        b_fsm := AXI_FSM.b_wait 
      }
    }
    is(AXI_FSM.b_wait){
      when(write_done){
        b_fsm := AXI_FSM.b_resp
      }
    }
    is(AXI_FSM.b_resp){
      when(io.axi.bready){
        b_fsm := AXI_FSM.b_idle
      }
    }
  }
  io.axi.bvalid := (b_fsm === AXI_FSM.b_resp)
  io.axi.bresp := b_resp

  // memory interface logic
  val m_idle :: m_get :: m_wait :: Nil  = Enum(3)
  // delay for memio, simulating memory access latency
  val u_lfsr = Module(new LFSR(4))

  //use two memio to support one read and one write at the same time
  val mem_fsm_r = RegInit(m_idle)
  val mem_latch_r = RegInit(0.U(4.W))
  val mem_fsm_w = RegInit(m_idle)
  val mem_latch_w = RegInit(0.U(4.W))
  val mem_reading = RegInit(false.B)
  val mem_writing = RegInit(false.B)

  val u_memio_r = Module(new MemIO())
  val u_memio_w = Module(new MemIO())
  // read part
  u_memio_r.io.clock  := clock.asBool
  u_memio_r.io.PC     := ar_PC
  u_memio_r.io.ren    := (ar_fsm === AXI_FSM.ar_wait) && (mem_fsm_r === m_idle)
  u_memio_r.io.raddr  := ar_addr
  u_memio_r.io.wen    := false.B
  u_memio_r.io.waddr  := 0.U(32.W)
  u_memio_r.io.wdata  := 0.U(32.W)
  u_memio_r.io.wmask  := 0.U(4.W)

  read_done := false.B
  switch(mem_fsm_r){
    is(m_idle){
      when(ar_fsm === AXI_FSM.ar_wait){
        mem_fsm_r := m_get
        mem_reading := 1.B
      }
    }
    is(m_get){
      r_data := u_memio_r.io.rdata
      r_resp := 0.U(2.W) //TODO: set response
      mem_latch_r := u_lfsr.io.out
      mem_fsm_r := m_wait
    }
    is(m_wait){
      when(mem_latch_r === 0.U){
        read_done := 1.B
        mem_reading := false.B
        mem_fsm_r := m_idle
      }.otherwise{
        mem_latch_r := mem_latch_r - 1.U
      }
    }
  }
  //write part
  u_memio_w.io.clock  := clock.asBool
  u_memio_w.io.PC     := aw_PC
  u_memio_w.io.ren    := false.B
  u_memio_w.io.raddr  := 0.U(32.W)
  u_memio_w.io.wen    := (aw_fsm === AXI_FSM.aw_wait) && (w_fsm === AXI_FSM.w_wait) && (mem_fsm_w === m_idle)
  u_memio_w.io.waddr  := aw_addr
  u_memio_w.io.wdata  := w_data
  u_memio_w.io.wmask  := w_strb

  write_done := false.B
  switch(mem_fsm_w){
    is(m_idle){
      when((aw_fsm === AXI_FSM.aw_wait) && (w_fsm === AXI_FSM.w_wait)){
        mem_fsm_w := m_get
        mem_writing := 1.B
      }
    }
    is(m_get){
      b_resp := 0.U(2.W) //TODO: set response
      mem_latch_w := u_lfsr.io.out
      mem_fsm_w := m_wait
    }
    is(m_wait){
      when(mem_latch_w === 0.U){
        write_done := mem_writing
        mem_writing := false.B
        mem_fsm_w := m_idle
      }.otherwise{
        mem_latch_w := mem_latch_w - 1.U
      }
    }
  }

}