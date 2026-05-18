import chisel3._
import chisel3.util._

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

class SRAM extends Bundle {
    val req_ren     = Output(Bool())
    val req_wen     = Output(Bool())
    val req_ready   = Input(Bool())
    val addr        = Output(UInt(32.W))
    val wdata       = Output(UInt(32.W))
    val wmask       = Output(UInt(4.W))

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

class SRAM_AXI extends Module{
    val io = IO(new Bundle{
        val PC              = Input(UInt(32.W))
        val sram            = Flipped(new SRAM)
        val axi_PC          = Output(UInt(32.W))
        val axi             = new AXI4Lite
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
        //axi
        val aw_sent       = RegInit(false.B)
        val w_sent        = RegInit(false.B) 
        val ready         = RegInit(false.B)


    //state machine
    op                := SRAM_AXIop.no_op
    switch(fsm){
        is(idle){
            when(io.sram.req_ren){// have read request
                fsm := Mux(io.axi.arready,wait_resp,have_req_r)
                op  := SRAM_AXIop.save_info | SRAM_AXIop.set_arv | Mux(io.axi.arready, SRAM_AXIop.set_ready, SRAM_AXIop.no_op)
                is_read := true.B
            }.elsewhen(io.sram.req_wen){// have write request
                fsm := Mux(io.axi.awready && io.axi.wready,wait_resp,have_req_w)
                op  := SRAM_AXIop.save_info | SRAM_AXIop.set_awv | SRAM_AXIop.set_wv | Mux(io.axi.awready && io.axi.wready, SRAM_AXIop.clear_aww | SRAM_AXIop.set_ready, SRAM_AXIop.no_op)
                is_read := false.B
            }
        }
        is(have_req_w){
            when((aw_sent | io.axi.awready) && (w_sent | io.axi.wready)){
                fsm := wait_resp
                op  := SRAM_AXIop.set_awv | SRAM_AXIop.set_wv | SRAM_AXIop.clear_aww | SRAM_AXIop.set_ready
            }.otherwise{
                op  := SRAM_AXIop.set_awv | SRAM_AXIop.set_wv
            }
        }
        is(have_req_r){
            when(io.axi.arready){
                fsm := wait_resp
                op  := SRAM_AXIop.set_arv | SRAM_AXIop.set_ready
            }.otherwise{
                op  := SRAM_AXIop.set_arv
            }
        }
        is(wait_resp){
            when((io.axi.rvalid || io.axi.bvalid)){
                when(io.sram.req_ren && io.sram.ret_ready){// have read request
                    fsm := Mux(io.axi.arready,wait_resp,have_req_r)
                    op  := SRAM_AXIop.save_ret | SRAM_AXIop.save_info | SRAM_AXIop.set_arv
                    is_read := true.B
                }.elsewhen(io.sram.req_wen && io.sram.ret_ready){// have write request
                    fsm := Mux(io.axi.awready && io.axi.wready,wait_resp,have_req_w)
                    op  := SRAM_AXIop.save_ret | SRAM_AXIop.save_info | SRAM_AXIop.set_awv | SRAM_AXIop.set_wv
                    is_read := false.B
                }.otherwise{
                    fsm := Mux(io.sram.ret_ready,idle,wait_ret)
                    op  := SRAM_AXIop.save_ret | Mux(io.sram.ret_ready,SRAM_AXIop.set_ready,SRAM_AXIop.no_op)
                }
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
    }

    val reg_rdata     = RegInit(0.U(32.W))
    val reg_resp      = RegInit(0.U(2.W))
    val reg_bresp      = RegInit(0.U(2.W))
    when(op(SRAM_AXIop.save_ret_bit)){
        reg_rdata     := io.axi.rdata
        reg_resp      := io.axi.rresp
        reg_bresp     := io.axi.bresp
    }

    //axi
    io.axi_PC                   := Mux(op(SRAM_AXIop.save_info_bit), io.PC, reg_PC)
    io.axi.awvalid              := op(SRAM_AXIop.set_awv_bit) && !aw_sent
    io.axi.awaddr               := Mux(op(SRAM_AXIop.save_info_bit), io.sram.addr, reg_addr)
    io.axi.wvalid               := op(SRAM_AXIop.set_wv_bit) && !w_sent
    io.axi.wdata                := Mux(op(SRAM_AXIop.save_info_bit), io.sram.wdata, reg_wdata)
    io.axi.wstrb                := Mux(op(SRAM_AXIop.save_info_bit), io.sram.wmask, reg_wmask)
    io.axi.bready               := ready & !is_read
    io.axi.arvalid              := op(SRAM_AXIop.set_arv_bit)
    io.axi.araddr               := Mux(op(SRAM_AXIop.save_info_bit), io.sram.addr, reg_addr)
    io.axi.rready               := ready & is_read

    when(op(SRAM_AXIop.clear_aww_bit)){
        aw_sent := false.B
    }.elsewhen(io.axi.awvalid && io.axi.awready){
        aw_sent := true.B
    }
    when(op(SRAM_AXIop.clear_aww_bit)){
        w_sent := false.B
    }.elsewhen(io.axi.wvalid && io.axi.wready){
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
    io.sram.rdata               := Mux(fsm === wait_ret,reg_rdata,io.axi.rdata)
    io.sram.resp                := Mux(fsm === wait_ret,reg_resp, Mux(is_read, io.axi.rresp, io.axi.bresp))

}



class AXI_Crossbar extends Module{
    val io = IO(new Bundle{
        val ifu_PC      = Input(UInt(32.W))
        val ifu_axi     = Flipped(new AXI4Lite)
        val lsu_PC      = Input(UInt(32.W))
        val lsu_axi     = Flipped(new AXI4Lite)
        val mem_rPC     = Output(UInt(32.W))
        val mem_wPC     = Output(UInt(32.W))
        val mem_axi     = new AXI4Lite
        // val uart_axi    = new AXI4Lite
        // val clint_axi   = new AXI4Lite
    })
    //fast address decoding
    def in_range(addr:UInt, begin:UInt):Bool = {
        // val match_len = 8
        // addr(31,32-match_len) === begin(31,32-match_len)

        // to test, always match
        true.B
    }
    def slave_get_master_r(slave:AXI4Lite,void:Int, master:AXI4Lite): Unit = {
        if(void == 1){
            slave.arvalid   := false.B
            slave.araddr    := 0.U(32.W)
            slave.rready    := false.B
        }else{
            slave.arvalid   := master.arvalid
            slave.araddr    := master.araddr
            slave.rready    := master.rready
        }
    }
    def slave_get_master_w(slave:AXI4Lite,void:Int, master:AXI4Lite): Unit = {
        if(void == 1){
            slave.awvalid   := false.B
            slave.awaddr    := 0.U(32.W)
            slave.wvalid    := false.B
            slave.wdata     := 0.U(32.W)
            slave.wstrb     := 0.U(4.W)
            slave.bready    := false.B
        }else{
            slave.awvalid   := master.awvalid
            slave.awaddr    := master.awaddr
            slave.wvalid    := master.wvalid
            slave.wdata     := master.wdata
            slave.wstrb     := master.wstrb
            slave.bready    := master.bready
        }
    }
    def master_get_slave_r(master:AXI4Lite, void:Int, slave:AXI4Lite): Unit = {
        if(void == 1){
            master.arready  := false.B
            master.rvalid   := false.B
            master.rdata    := 0.U(32.W)
            master.rresp    := 0.U(2.W)
        }else{
            master.arready  := slave.arready
            master.rvalid   := slave.rvalid
            master.rdata    := slave.rdata
            master.rresp    := slave.rresp
        }
    }
    def master_get_slave_w(master:AXI4Lite, void:Int, slave:AXI4Lite): Unit = {
        if(void == 1){
            master.awready  := false.B
            master.wready   := false.B
            master.bvalid   := false.B
            master.bresp    := 0.U(2.W)
        }else{
            master.awready  := slave.awready
            master.wready   := slave.wready
            master.bvalid   := slave.bvalid
            master.bresp    := slave.bresp
        }
    }

    //mem read: switch between ifu_axi and lsu_axi according to PC
    val r_idle :: r_ifu :: r_lsu :: Nil = Enum(3)
    val r_fsm = RegInit(r_idle)
    io.mem_rPC          := 0.U(32.W) 
    slave_get_master_r(io.mem_axi, 1, io.ifu_axi)
    switch(r_fsm){
        is(r_idle){
            when(io.ifu_axi.arvalid && in_range(io.ifu_axi.araddr, D_MEM.addr_begin)){
                r_fsm := r_ifu
            }.elsewhen(io.lsu_axi.arvalid && in_range(io.lsu_axi.araddr, D_MEM.addr_begin)){
                r_fsm := r_lsu
            }
        }
        is(r_ifu){
            io.mem_rPC          := io.ifu_PC
            slave_get_master_r(io.mem_axi, 0, io.ifu_axi)
            when(io.ifu_axi.rready && io.ifu_axi.rvalid){
                when(io.ifu_axi.arvalid && in_range(io.ifu_axi.araddr, D_MEM.addr_begin)){
                    r_fsm := r_ifu
                }.elsewhen(io.lsu_axi.arvalid && in_range(io.lsu_axi.araddr, D_MEM.addr_begin)){
                    r_fsm := r_lsu
                }.otherwise{
                    r_fsm := r_idle
                }
            }
        }
        is(r_lsu){
            io.mem_rPC          := io.lsu_PC
            slave_get_master_r(io.mem_axi, 0, io.lsu_axi)
            when(io.lsu_axi.rready && io.lsu_axi.rvalid){
                when(io.ifu_axi.arvalid && in_range(io.ifu_axi.araddr, D_MEM.addr_begin)){
                    r_fsm := r_ifu
                }.elsewhen(io.lsu_axi.arvalid && in_range(io.lsu_axi.araddr, D_MEM.addr_begin)){
                    r_fsm := r_lsu
                }.otherwise{
                    r_fsm := r_idle
                }
            }
        }
    }

    //mem write: simply forward to mem_axi
    val w_idle :: w_lsu :: Nil = Enum(2)
    val w_fsm = RegInit(w_idle)
    io.mem_wPC          := 0.U(32.W)
    slave_get_master_w(io.mem_axi, 1, io.lsu_axi)
    switch(w_fsm){
        is(w_idle){
            when((io.lsu_axi.awvalid) && in_range(io.lsu_axi.awaddr, D_MEM.addr_begin)){
                w_fsm := w_lsu
            }
        }
        is(w_lsu){
            io.mem_wPC          := io.lsu_PC
            slave_get_master_w(io.mem_axi, 0, io.lsu_axi)
            when(io.lsu_axi.bready && io.lsu_axi.bvalid){
                when((io.lsu_axi.awvalid) && in_range(io.lsu_axi.awaddr, D_MEM.addr_begin)){
                    w_fsm := w_lsu
                }.otherwise{
                    w_fsm := w_idle
                }
            }
        }
    }
     
    //IFU connection
    when(r_fsm === r_ifu){
        master_get_slave_r(io.ifu_axi, 0, io.mem_axi)
    }.otherwise{
        master_get_slave_r(io.ifu_axi, 1, io.mem_axi)
    }
    master_get_slave_w(io.ifu_axi, 1, io.mem_axi)
    //LSU connection
    when(r_fsm === r_lsu){
        master_get_slave_r(io.lsu_axi, 0, io.mem_axi)
    }.otherwise{
        master_get_slave_r(io.lsu_axi, 1, io.mem_axi)
    }
    when(w_fsm === w_lsu){
        master_get_slave_w(io.lsu_axi, 0, io.mem_axi)
    }.otherwise{
        master_get_slave_w(io.lsu_axi, 1, io.mem_axi)
    }

}

class AXI_Slave extends Module{
    val io = IO(new Bundle{
        val rPC         = Input (UInt(32.W))
        val wPC         = Input (UInt(32.W))
        val axi         = Flipped(new AXI4Lite)

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
    val aw_PC       = RegInit(0.U(32.W))// for debug
    // w
    val w_fsm       = RegInit(AXI_FSM.w_idle)
    val w_data      = RegInit(0.U(32.W))
    val w_strb      = RegInit(0.U(4.W))
    // ar
    val ar_fsm      = RegInit(AXI_FSM.ar_idle)
    val ar_addr     = RegInit(0.U(32.W))
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
  io.axi.bresp  := b_resp

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
  io.r_resp.ready   := io.axi.rready && io.axi.rvalid
  write_done        := io.w_resp.valid    
  b_resp            := io.w_resp.resp
  io.w_resp.ready   := io.axi.bready && io.axi.bvalid

}