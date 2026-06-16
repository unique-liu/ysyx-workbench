import chisel3._
import chisel3.util._

class AXI4 extends Bundle {// complete AXI4
    val aw = new Bundle{
        val valid   = Output(Bool())
        val ready   = Input(Bool())
        val addr    = Output(UInt(32.W))
        val id      = Output(UInt(4.W))//+
        val len     = Output(UInt(8.W))//+
        val size    = Output(UInt(3.W))//+
        val burst   = Output(UInt(2.W))//+
    }
    val w = new Bundle{
        val valid   = Output(Bool())
        val ready   = Input(Bool())
        val data    = Output(UInt(32.W))
        val strb    = Output(UInt(4.W))
        val last    = Output(Bool())//+
    }
    val ar = new Bundle{
        val valid   = Output(Bool())
        val ready   = Input(Bool())
        val addr    = Output(UInt(32.W))
        val id      = Output(UInt(4.W))//+
        val len     = Output(UInt(8.W))//+
        val size    = Output(UInt(3.W))//+
        val burst   = Output(UInt(2.W))//+
    }
    val r = new Bundle{
        val valid   = Input(Bool())
        val ready   = Output(Bool())
        val resp    = Input(UInt(2.W))
        val data    = Input(UInt(32.W))
        val last    = Input(Bool())//+
        val id      = Input(UInt(4.W))//+
    }
    val b = new Bundle{
        val valid   = Input(Bool())
        val ready   = Output(Bool())
        val resp    = Input(UInt(2.W))
        val id      = Input(UInt(4.W))//+
    }

}
object AXI_RESP{
    val OKAY  = 0.U(2.W)//normal access success
    val EXOKAY= 1.U(2.W)//exclusive access success  
    val SLVERR= 2.U(2.W)//slave error, e.g. access to non-exist address
    val DECERR= 3.U(2.W)//decode error, e.g. not matching address
}
object AXI_BURST{
    val FIXED = 0.U(2.W)//address is fixed, e.g. for peripheral
    val INCR  = 1.U(2.W)//address is incremented, e.g. for memory
    val WRAP  = 2.U(2.W)//address is incremented and wrap around when reach the boundary, e.g. for cache line access
}
object AXI_CONN{
    def get_void():AXI4={
        val void = Wire(new AXI4)
        void.aw.valid   := false.B
        void.aw.ready   := false.B
        void.aw.addr    := 0.U(32.W)
        void.aw.id      := 0.U(4.W)
        void.aw.len     := 0.U(8.W)
        void.aw.size    := 0.U(3.W)
        void.aw.burst   := 0.U(2.W)
        void.w.valid    := false.B
        void.w.ready    := false.B
        void.w.data     := 0.U(32.W)
        void.w.strb     := 0.U(4.W)
        void.w.last     := false.B
        void.ar.valid   := false.B
        void.ar.ready   := false.B
        void.ar.addr    := 0.U(32.W)
        void.ar.id      := 0.U(4.W)
        void.ar.len     := 0.U(8.W)
        void.ar.size    := 0.U(3.W)
        void.ar.burst   := 0.U(2.W)
        void.r.valid    := false.B
        void.r.ready    := false.B
        void.r.resp     := 0.U(2.W)
        void.r.data     := 0.U(32.W)
        void.r.last     := false.B
        void.r.id       := 0.U(4.W)
        void.b.valid    := false.B
        void.b.ready    := false.B
        void.b.resp     := 0.U(2.W)
        void.b.id       := 0.U(4.W)
        void
    }
    def slave_get_master_ar(slave:AXI4,master:AXI4):Unit={
        slave.ar.valid   := master.ar.valid
        slave.ar.addr    := master.ar.addr
        slave.ar.id      := master.ar.id
        slave.ar.len     := master.ar.len
        slave.ar.size    := master.ar.size
        slave.ar.burst   := master.ar.burst
    }
    def slave_get_master_r(slave:AXI4,master:AXI4):Unit={
        slave.r.ready    := master.r.ready
    }
    def slave_get_master_aww(slave:AXI4,master:AXI4):Unit={
        slave.aw.valid   := master.aw.valid
        slave.aw.addr    := master.aw.addr
        slave.aw.id      := master.aw.id
        slave.aw.len     := master.aw.len
        slave.aw.size    := master.aw.size
        slave.aw.burst   := master.aw.burst
        slave.w.valid    := master.w.valid
        slave.w.data     := master.w.data
        slave.w.strb     := master.w.strb
        slave.w.last     := master.w.last
    }
    def slave_get_master_b(slave:AXI4,master:AXI4):Unit={
        slave.b.ready    := master.b.ready
    }
    def master_get_slave_ar(master:AXI4, slave:AXI4):Unit={
        master.ar.ready  := slave.ar.ready
    }
    def master_get_slave_r(master:AXI4, slave:AXI4):Unit={
        master.r.valid   := slave.r.valid
        master.r.data    := slave.r.data
        master.r.resp    := slave.r.resp
        master.r.last    := slave.r.last
        master.r.id      := slave.r.id
    }
    def master_get_slave_aww(master:AXI4, slave:AXI4):Unit={
        master.aw.ready  := slave.aw.ready
        master.w.ready   := slave.w.ready
    }
    def master_get_slave_b(master:AXI4, slave:AXI4):Unit={
        master.b.valid   := slave.b.valid
        master.b.resp    := slave.b.resp
        master.b.id      := slave.b.id
    }
}

class SRAM extends Bundle {
    val req_ren     = Output(Bool())
    val req_wen     = Output(Bool())
    val req_ready   = Input(Bool())
    val addr        = Output(UInt(32.W))
    val wdata       = Output(UInt(32.W))
    val wmask       = Output(UInt(4.W))
    val size        = Output(UInt(3.W))

    val ret_valid   = Input(Bool())
    val ret_ready   = Output(Bool())
    val rdata       = Input(UInt(32.W))
    val resp        = Input(UInt(2.W))
}

class ReadReq extends Bundle {
    val valid       = Output(Bool())  
    val addr        = Output(UInt(32.W))
}
class ReadResp extends Bundle {
    val valid       = Input(Bool())
    val ready       = Output(Bool())
    val data        = Input(UInt(32.W))
    val resp        = Input(UInt(2.W))   // AXI4-Lite 响应：0: OKAY, 2: SLVERR, etc.
}
class WriteReq extends Bundle {
    val valid       = Output(Bool())
    val addr        = Output(UInt(32.W))
    val data        = Output(UInt(32.W))
    val strb        = Output(UInt(4.W))
}
class WriteResp extends Bundle {
    val valid       = Input(Bool())
    val ready       = Output(Bool())
    val resp        = Input(UInt(2.W))
}

object AXI_FSM{
    val width = 2;
    val aw_idle :: aw_wait :: Nil = Enum(2)//idle: no aw request, wait: get aw request, wait for work done
    val w_idle :: w_wait :: Nil  = Enum(2)
    val ar_idle :: ar_wait :: Nil  = Enum(2)
    val r_idle :: r_wait :: r_resp :: Nil  = Enum(3)//idle: no ar request, wait: have ar request, wait for rdata, resp: have rdata, wait for master to accept 
    val b_idle :: b_wait :: b_resp :: Nil  = Enum(3)

}

object SRAM_AXIop{
    val width           = 7
    val save_info_bit   = 0
    val save_ret_bit    = 1
    val set_awv_bit     = 2
    val set_wv_bit      = 3
    val set_arv_bit     = 4
    val set_ready_bit   = 5
    val clear_aww_bit   = 6
    
    val no_op           = "b0000000".U(width.W)
    val save_info       = "b0000001".U(width.W)
    val save_ret        = "b0000010".U(width.W)
    val set_awv         = "b0000100".U(width.W)
    val set_wv          = "b0001000".U(width.W)
    val set_arv         = "b0010000".U(width.W)
    val set_ready       = "b0100000".U(width.W)//set rready and bready when have response, wait for master to accept
    val clear_aww       = "b1000000".U(width.W)//clear awvalid and wvalid after send aw and w, wait for response

}

class SRAM_AXI(id:Int=0) extends Module{//SRAM to AXI4 bridge
    val io = IO(new Bundle{
        val PC              = Input(UInt(32.W))
        val sram            = Flipped(new SRAM)
        val axi_PC          = Output(UInt(32.W))
        val axi             = new AXI4
    })   
    //declarations
        //state machine
        val idle :: have_req_w :: have_req_r :: wait_resp :: wait_ret :: Nil = Enum(5)
        val op            = Wire(UInt(SRAM_AXIop.width.W))
        val fsm           = RegInit(idle)
        val is_read       = RegInit(false.B)
        //info
        val reg_PC        = RegInit(0.U(32.W))
        val reg_addr      = RegInit(0.U(32.W))
        val reg_wdata     = RegInit(0.U(32.W))
        val reg_wmask     = RegInit(0.U(4.W))
        val reg_size      = RegInit(0.U(3.W))
        //axi
        val aw_sent       = RegInit(false.B)
        val w_sent        = RegInit(false.B) 
        val ready         = RegInit(false.B)


    //state machine
    op                := SRAM_AXIop.no_op
    // switch(fsm){
    //     is(idle){
    //         when(io.sram.req_ren){// have read request
    //             fsm := Mux(io.axi.ar.ready,wait_resp,have_req_r)
    //             op  := SRAM_AXIop.save_info | SRAM_AXIop.set_arv | Mux(io.axi.ar.ready, SRAM_AXIop.set_ready, SRAM_AXIop.no_op)
    //             is_read := true.B
    //         }.elsewhen(io.sram.req_wen){// have write request
    //             fsm := Mux(io.axi.aw.ready && io.axi.w.ready,wait_resp,have_req_w)
    //             op  := SRAM_AXIop.save_info | SRAM_AXIop.set_awv | SRAM_AXIop.set_wv | Mux(io.axi.aw.ready && io.axi.w.ready, SRAM_AXIop.clear_aww | SRAM_AXIop.set_ready, SRAM_AXIop.no_op)
    //             is_read := false.B
    //         }
    //     }
    //     is(have_req_w){
    //         when((aw_sent | io.axi.aw.ready) && (w_sent | io.axi.w.ready)){
    //             fsm := wait_resp
    //             op  := SRAM_AXIop.set_awv | SRAM_AXIop.set_wv | SRAM_AXIop.clear_aww | SRAM_AXIop.set_ready
    //         }.otherwise{
    //             op  := SRAM_AXIop.set_awv | SRAM_AXIop.set_wv
    //         }
    //     }
    //     is(have_req_r){
    //         when(io.axi.ar.ready){
    //             fsm := wait_resp
    //             op  := SRAM_AXIop.set_arv | SRAM_AXIop.set_ready
    //         }.otherwise{
    //             op  := SRAM_AXIop.set_arv
    //         }
    //     }
    //     is(wait_resp){
    //         when((io.axi.r.valid || io.axi.b.valid)){
    //             when(io.sram.req_ren && io.sram.ret_ready){// have read request
    //                 fsm := Mux(io.axi.ar.ready,wait_resp,have_req_r)
    //                 op  := SRAM_AXIop.save_ret | SRAM_AXIop.save_info | SRAM_AXIop.set_arv
    //                 is_read := true.B
    //             }.elsewhen(io.sram.req_wen && io.sram.ret_ready){// have write request
    //                 fsm := Mux(io.axi.aw.ready && io.axi.w.ready,wait_resp,have_req_w)
    //                 op  := SRAM_AXIop.save_ret | SRAM_AXIop.save_info | SRAM_AXIop.set_awv | SRAM_AXIop.set_wv
    //                 is_read := false.B
    //             }.otherwise{
    //                 fsm := Mux(io.sram.ret_ready,idle,wait_ret)
    //                 op  := SRAM_AXIop.save_ret | Mux(io.sram.ret_ready,SRAM_AXIop.set_ready,SRAM_AXIop.no_op)
    //             }
    //         }
    //     }
    //     is(wait_ret){
    //         when(io.sram.ret_ready){
    //             fsm := idle
    //             op  := SRAM_AXIop.set_ready
    //         }
    //     }
    // }

    //use a simpler fsm to avoid loop
    switch(fsm){
        is(idle){
            when(io.sram.req_ren){// have read request
                fsm := have_req_r
                op  := SRAM_AXIop.save_info 
                is_read := true.B
            }.elsewhen(io.sram.req_wen){// have write request
                fsm := have_req_w
                op  := SRAM_AXIop.save_info
                is_read := false.B
            }
        }
        is(have_req_w){
            when((aw_sent | io.axi.aw.ready) && (w_sent | io.axi.w.ready)){
                fsm := wait_resp
                op  := SRAM_AXIop.set_awv | SRAM_AXIop.set_wv | SRAM_AXIop.clear_aww | SRAM_AXIop.set_ready
            }.otherwise{
                op  := SRAM_AXIop.set_awv | SRAM_AXIop.set_wv
            }
        }
        is(have_req_r){
            when(io.axi.ar.ready){
                fsm := wait_resp
                op  := SRAM_AXIop.set_arv | SRAM_AXIop.set_ready
            }.otherwise{
                op  := SRAM_AXIop.set_arv
            }
        }
        is(wait_resp){
            when((io.axi.r.valid || io.axi.b.valid)){
                fsm := Mux(io.sram.ret_ready,idle,wait_ret)
                op  := SRAM_AXIop.save_ret | Mux(io.sram.ret_ready,SRAM_AXIop.set_ready,SRAM_AXIop.no_op)
            }
        }
        is(wait_ret){
            when(io.sram.ret_ready){
                fsm := idle
                op  := SRAM_AXIop.set_ready
            }
        }
    }

    //info
    when(op(SRAM_AXIop.save_info_bit)){
        reg_PC        := io.PC
        reg_addr      := io.sram.addr
        reg_wdata     := io.sram.wdata
        reg_wmask     := io.sram.wmask
        reg_size      := io.sram.size
    }

    val reg_rdata     = RegInit(0.U(32.W))
    val reg_resp      = RegInit(0.U(2.W))
    val reg_bresp      = RegInit(0.U(2.W))
    when(op(SRAM_AXIop.save_ret_bit)){
        reg_rdata     := io.axi.r.data
        reg_resp      := io.axi.r.resp
        reg_bresp     := io.axi.b.resp
    }

    //axi
    io.axi_PC                    := reg_PC//Mux(op(SRAM_AXIop.save_info_bit), io.PC, reg_PC)
    io.axi.aw.valid              := fsm === have_req_w && !aw_sent //&& op(SRAM_AXIop.set_awv_bit) 
    io.axi.aw.addr               := reg_addr//Mux(op(SRAM_AXIop.save_info_bit), io.sram.addr, reg_addr)
    io.axi.aw.id                 := id.U(4.W)
    io.axi.aw.len                := 0.U(8.W)
    io.axi.aw.size               := reg_size//Mux(op(SRAM_AXIop.save_info_bit), io.sram.size, reg_size)
    io.axi.aw.burst              := AXI_BURST.INCR

    io.axi.w.valid               := fsm === have_req_w && !w_sent //op(SRAM_AXIop.set_wv_bit) && 
    io.axi.w.data                := reg_wdata//Mux(op(SRAM_AXIop.save_info_bit), io.sram.wdata, reg_wdata)
    io.axi.w.strb                := reg_wmask//Mux(op(SRAM_AXIop.save_info_bit), io.sram.wmask, reg_wmask)
    io.axi.w.last                := true.B

    io.axi.b.ready               := ready & !is_read

    io.axi.ar.valid              := fsm === have_req_r//op(SRAM_AXIop.set_arv_bit)
    io.axi.ar.addr               := reg_addr//Mux(op(SRAM_AXIop.save_info_bit), io.sram.addr, reg_addr)
    io.axi.ar.id                 := id.U(4.W)
    io.axi.ar.len                := 0.U(8.W)
    io.axi.ar.size               := reg_size//Mux(op(SRAM_AXIop.save_info_bit), io.sram.size, reg_size)
    io.axi.ar.burst              := AXI_BURST.INCR

    io.axi.r.ready               := ready & is_read

    when(op(SRAM_AXIop.clear_aww_bit)){
        aw_sent := false.B
    }.elsewhen(io.axi.aw.valid && io.axi.aw.ready){
        aw_sent := true.B
    }
    when(op(SRAM_AXIop.clear_aww_bit)){
        w_sent := false.B
    }.elsewhen(io.axi.w.valid && io.axi.w.ready){
        w_sent := true.B
    }
    when(op(SRAM_AXIop.set_ready_bit)){
        ready := true.B
    }.elsewhen(op(SRAM_AXIop.save_ret_bit)){
        ready := false.B
    }

    //sram
    io.sram.req_ready           := (fsm === idle) | op(SRAM_AXIop.set_ready_bit)
    io.sram.ret_valid           := (fsm === wait_ret) | op(SRAM_AXIop.save_ret_bit)
    io.sram.rdata               := Mux(fsm === wait_ret,reg_rdata,io.axi.r.data)
    io.sram.resp                := Mux(fsm === wait_ret,Mux(is_read,reg_resp,reg_bresp), Mux(is_read, io.axi.r.resp, io.axi.b.resp))

}


class AXI_Slave extends Module{// need to support burst read for icache in no_soc mode
    val io = IO(new Bundle{
        val rPC         = Input (UInt(32.W))
        val wPC         = Input (UInt(32.W))
        val axi         = Flipped(new AXI4)

        val rPC_out     = Output (UInt(32.W))
        val r_req       = new ReadReq
        val r_resp      = new ReadResp
        val wPC_out     = Output (UInt(32.W))
        val w_req       = new WriteReq
        val w_resp      = new WriteResp
    })
    // declarations
    // aw
    val aw_fsm      = RegInit(AXI_FSM.aw_idle)
    val aw_addr     = RegInit(0.U(32.W))
    val aw_id       = RegInit(0.U(4.W))
    val aw_PC       = RegInit(0.U(32.W))// for debug
    // w
    val w_fsm       = RegInit(AXI_FSM.w_idle)
    val w_data      = RegInit(0.U(32.W))
    val w_strb      = RegInit(0.U(4.W))
    // ar
    val ar_fsm      = RegInit(AXI_FSM.ar_idle)
    val ar_addr     = RegInit(0.U(32.W))
    val ar_id       = RegInit(0.U(4.W))
    val ar_PC       = RegInit(0.U(32.W))// for debug
    // r
    val r_fsm       = RegInit(AXI_FSM.r_idle)
    val r_data      = Wire(UInt(32.W))
    val r_resp      = Wire(UInt(2.W))
    val read_done   = Wire(Bool())
    // b
    val b_fsm       = RegInit(AXI_FSM.b_idle)
    val b_resp      = Wire(UInt(2.W))
    val write_done  = Wire(Bool())
  // aw
  switch(aw_fsm){
    is(AXI_FSM.aw_idle){
      when(io.axi.aw.valid){//ready is ensured by its assignment
        aw_fsm := AXI_FSM.aw_wait
        aw_addr := io.axi.aw.addr
        aw_id := io.axi.aw.id
        aw_PC := io.wPC
      }
    }
    is(AXI_FSM.aw_wait){
      when(write_done){//wait for work done
        aw_fsm := AXI_FSM.aw_idle
        aw_addr := 0.U(32.W)
        aw_id := 0.U(4.W)
        aw_PC := 0.U(32.W)
      }
    }
  }
  io.axi.aw.ready := (aw_fsm === AXI_FSM.aw_idle)

  //w
  switch(w_fsm){
    is(AXI_FSM.w_idle){
      when(io.axi.w.valid){
        w_fsm := AXI_FSM.w_wait
        w_data := io.axi.w.data
        w_strb := io.axi.w.strb
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
  io.axi.w.ready := (w_fsm === AXI_FSM.w_idle)

  //ar
  switch(ar_fsm){
    is(AXI_FSM.ar_idle){
      when(io.axi.ar.valid){
        ar_fsm := AXI_FSM.ar_wait   
        ar_addr := io.axi.ar.addr
        ar_id := io.axi.ar.id
        ar_PC := io.rPC
      }
    }
    is(AXI_FSM.ar_wait){
      when(read_done){//wait for rdata
        ar_fsm := AXI_FSM.ar_idle
        ar_addr := 0.U(32.W)
        ar_id := 0.U(4.W)
        ar_PC := 0.U(32.W)
      }
    }
  }
  io.axi.ar.ready := (ar_fsm === AXI_FSM.ar_idle)

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
      when(io.axi.r.ready){
        r_fsm := AXI_FSM.r_idle
      }
    }
  }
  io.axi.r.valid := (r_fsm === AXI_FSM.r_resp)
  io.axi.r.data := r_data
  io.axi.r.resp := r_resp
  io.axi.r.id   := ar_id
  io.axi.r.last := true.B
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
      when(io.axi.b.ready){
        b_fsm := AXI_FSM.b_idle
      }
    }
  }
  io.axi.b.valid := (b_fsm === AXI_FSM.b_resp)
  io.axi.b.resp  := b_resp
  io.axi.b.id    := aw_id
  //communication with outside
  io.rPC_out        := ar_PC
  io.wPC_out        := aw_PC
  io.r_req.valid    := (ar_fsm === AXI_FSM.ar_wait)
  io.r_req.addr     := ar_addr
  io.w_req.valid    := (aw_fsm === AXI_FSM.aw_wait) && (w_fsm === AXI_FSM.w_wait)
  io.w_req.addr     := aw_addr
  io.w_req.data     := w_data
  io.w_req.strb     := w_strb

  //outside will preserve data until give ready signal
  read_done         := io.r_resp.valid 
  r_data            := io.r_resp.data
  r_resp            := io.r_resp.resp
  io.r_resp.ready   := io.axi.r.ready && io.axi.r.valid
  write_done        := io.w_resp.valid    
  b_resp            := io.w_resp.resp
  io.w_resp.ready   := io.axi.b.ready && io.axi.b.valid

}

class AXI_Arbiter extends Module{
    val io = IO(new Bundle{
        val ifu_PC      = Input(UInt(32.W))
        val ifu_axi     = Flipped(new AXI4)
        val lsu_PC      = Input(UInt(32.W))
        val lsu_axi     = Flipped(new AXI4)

        val out_rPC     = Output(UInt(32.W))
        val out_wPC     = Output(UInt(32.W))
        val out_axi     = new AXI4
        val clint_PC    = Output(UInt(32.W))
        val clint_axi   = new AXI4
    })
    def addr_in_range(addr:UInt, begin:UInt):Bool = {
        val mask = "hFF000000".U(32.W)
        (addr & mask) === (begin & mask)
    }
    //void
    val void_axi = AXI_CONN.get_void()
    // CLINT check
    val is_clint_ifu = false.B
    val is_clint_lsu = addr_in_range(io.lsu_axi.ar.addr, D_CLINT.addr_begin)

    //out read channel: switch between ifu_axi and lsu_axi
    val r_idle :: r_ifu :: r_lsu :: Nil = Enum(3)
    val r_fsm = RegInit(r_idle)
    val share_lsu = RegInit(0.U(2.W))// let lsu access when too many ifu requests, to avoid starvation
    val have_ifu_req = io.ifu_axi.ar.valid && !is_clint_ifu
    val have_lsu_req = io.lsu_axi.ar.valid && !is_clint_lsu
    val lsu_read_imediately = (share_lsu === 3.U && have_ifu_req)

    //if ifu read too much, let lsu read immediately to avoid starvation
    when(io.ifu_axi.ar.valid && io.ifu_axi.ar.ready && io.lsu_axi.ar.valid && !io.lsu_axi.ar.ready){
        share_lsu := share_lsu + 1.U(2.W)
    }.elsewhen(io.lsu_axi.ar.valid && io.lsu_axi.ar.ready){
        share_lsu := 0.U(2.W)
    }


    io.out_rPC := 0.U(32.W)
    AXI_CONN.slave_get_master_ar(io.out_axi, void_axi)
    AXI_CONN.slave_get_master_r(io.out_axi, void_axi)
    switch(r_fsm){//TODO: can seperate ar and r channel, so that can switch to other master when waiting for rdata
        is(r_idle){
            when(have_ifu_req && !lsu_read_imediately){
                r_fsm := r_ifu
            }.elsewhen(have_lsu_req){
                r_fsm := r_lsu
            }
        }
        is(r_ifu){
            io.out_rPC := io.ifu_PC
            AXI_CONN.slave_get_master_ar(io.out_axi, io.ifu_axi)
            AXI_CONN.slave_get_master_r(io.out_axi, io.ifu_axi)
            when(io.ifu_axi.r.ready && io.ifu_axi.r.valid && io.ifu_axi.r.last){
                when(have_ifu_req && !lsu_read_imediately){
                    r_fsm := r_ifu
                }.elsewhen(have_lsu_req){
                    r_fsm := r_lsu
                }.otherwise{
                    r_fsm := r_idle
                }
            }
        }
        is(r_lsu){
            io.out_rPC := io.lsu_PC
            AXI_CONN.slave_get_master_ar(io.out_axi, io.lsu_axi)
            AXI_CONN.slave_get_master_r(io.out_axi, io.lsu_axi)
            when(io.lsu_axi.r.ready && io.lsu_axi.r.valid && io.lsu_axi.r.last){
                when(have_ifu_req && !lsu_read_imediately){
                    r_fsm := r_ifu
                }.elsewhen(have_lsu_req){
                    r_fsm := r_lsu
                }.otherwise{
                    r_fsm := r_idle
                }
            }
        }
    }

    //out write channel: 
    val w_idle :: w_lsu :: Nil = Enum(2)
    val w_fsm = RegInit(w_idle)

    io.out_wPC := 0.U(32.W)
    AXI_CONN.slave_get_master_aww(io.out_axi, void_axi)
    AXI_CONN.slave_get_master_b(io.out_axi, void_axi)
    switch(w_fsm){
        is(w_idle){
            when(io.lsu_axi.aw.valid && !is_clint_lsu){
                w_fsm := w_lsu
            }
        }
        is(w_lsu){
            io.out_wPC := io.lsu_PC
            AXI_CONN.slave_get_master_aww(io.out_axi, io.lsu_axi)
            AXI_CONN.slave_get_master_b(io.out_axi, io.lsu_axi)
            when(io.lsu_axi.b.valid && io.lsu_axi.b.ready){
                when(io.lsu_axi.aw.valid && !is_clint_lsu){
                    w_fsm := w_lsu
                }.otherwise{
                    w_fsm := w_idle
                }
            }
        }
    }

    //clint:
    val c_idle :: c_w :: c_r :: Nil = Enum(3)
    val c_fsm = RegInit(c_idle)

    io.clint_PC := 0.U(32.W)
    AXI_CONN.slave_get_master_ar(io.clint_axi, void_axi)
    AXI_CONN.slave_get_master_r(io.clint_axi, void_axi)
    AXI_CONN.slave_get_master_aww(io.clint_axi, void_axi)
    AXI_CONN.slave_get_master_b(io.clint_axi, void_axi)
    switch(c_fsm){
        is(c_idle){
            when(is_clint_lsu && io.lsu_axi.ar.valid){
                c_fsm := c_r
            }.elsewhen(is_clint_lsu && io.lsu_axi.aw.valid){
                c_fsm := c_w
            }
        }
        is(c_r){
            io.clint_PC := io.lsu_PC
            AXI_CONN.slave_get_master_ar(io.clint_axi, io.lsu_axi)
            AXI_CONN.slave_get_master_r(io.clint_axi, io.lsu_axi)
            when(io.lsu_axi.r.valid && io.lsu_axi.r.ready && io.lsu_axi.r.last){
                c_fsm := c_idle
            }
        }
        is(c_w){
            io.clint_PC := io.lsu_PC
            AXI_CONN.slave_get_master_aww(io.clint_axi, io.lsu_axi)
            AXI_CONN.slave_get_master_b(io.clint_axi, io.lsu_axi)
            when(io.lsu_axi.b.valid && io.lsu_axi.b.ready){
                c_fsm := c_idle
            }
        }
    }
    
    //ifu
        //read
    when(r_fsm === r_ifu){  
        AXI_CONN.master_get_slave_r(io.ifu_axi,io.out_axi)
        AXI_CONN.master_get_slave_ar(io.ifu_axi, io.out_axi)
    }.otherwise{
        AXI_CONN.master_get_slave_r(io.ifu_axi,void_axi)
        AXI_CONN.master_get_slave_ar(io.ifu_axi, void_axi)
    }
        //write
    AXI_CONN.master_get_slave_aww(io.ifu_axi,void_axi)
    AXI_CONN.master_get_slave_b(io.ifu_axi,void_axi)

    //lsu
        //read
    when(r_fsm === r_lsu){
        AXI_CONN.master_get_slave_r(io.lsu_axi,io.out_axi)
        AXI_CONN.master_get_slave_ar(io.lsu_axi, io.out_axi)
    }.elsewhen(c_fsm === c_r){
        AXI_CONN.master_get_slave_r(io.lsu_axi,io.clint_axi)
        AXI_CONN.master_get_slave_ar(io.lsu_axi, io.clint_axi)
    }.otherwise{
        AXI_CONN.master_get_slave_r(io.lsu_axi,void_axi)
        AXI_CONN.master_get_slave_ar(io.lsu_axi, void_axi)
    }
        //write
    when(w_fsm === w_lsu){
        AXI_CONN.master_get_slave_aww(io.lsu_axi, io.out_axi)
        AXI_CONN.master_get_slave_b(io.lsu_axi,io.out_axi)
    }.elsewhen(c_fsm === c_w){
        AXI_CONN.master_get_slave_aww(io.lsu_axi, io.clint_axi)
        AXI_CONN.master_get_slave_b(io.lsu_axi,io.clint_axi)
    }.otherwise{
        AXI_CONN.master_get_slave_aww(io.lsu_axi, void_axi)
        AXI_CONN.master_get_slave_b(io.lsu_axi,void_axi)
    }

    //debug: only lsu can access device, so check its all accesses
    val u_diffskip          = Module(new DiffSkip())
    u_diffskip.io.clock     := clock.asBool
    u_diffskip.io.en        := io.lsu_axi.aw.valid && io.lsu_axi.aw.ready || io.lsu_axi.ar.valid && io.lsu_axi.ar.ready
    u_diffskip.io.addr      := Mux(io.lsu_axi.aw.valid && io.lsu_axi.aw.ready, io.lsu_axi.aw.addr, io.lsu_axi.ar.addr)
    u_diffskip.io.PC        := io.lsu_PC
    u_diffskip.io.idx       := Mux(io.lsu_axi.aw.valid && io.lsu_axi.aw.ready, 11.U, 10.U) //10: read  11: write

    u_diffskip.io.ret       := io.lsu_axi.r.valid && io.lsu_axi.r.ready || io.lsu_axi.b.valid && io.lsu_axi.b.ready
    u_diffskip.io.rdata     := Mux(io.lsu_axi.r.valid && io.lsu_axi.r.ready, io.lsu_axi.r.data, 0.U)
    u_diffskip.io.ridx      := Mux(io.lsu_axi.r.valid && io.lsu_axi.r.ready, 10.U, 11.U) //10: read  11: write

    if(Config.perf_on){
        //perf-ini
        val perf_ini            = Module(new perf(PT.ini))
        perf_ini.io.valid       := have_ifu_req && r_fsm === r_lsu
        perf_ini.io.code        := PT.ini_w_lsu
        //perf-lmd
        val perf_lmd            = Module(new perf(PT.lmd))
        perf_lmd.io.valid       := have_lsu_req && r_fsm === r_ifu
        perf_lmd.io.code        := PT.lmd_w_ifu
    }
}