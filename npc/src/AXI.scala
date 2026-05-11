import chisel3._
import chisel3.util._
import os.read

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
    val clear_aww_bit   = 5
    val set_ready_bit   = 6
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
    //state machine
    val idle :: have_req_w :: have_req_r :: wait_resp :: wait_ret :: Nil = Enum(5)
    val op            = Wire(UInt(SRAM_AXIop.width.W))
    val fsm           = RegInit(idle)
    val is_read       = RegInit(false.B)
    op                := SRAM_AXIop.no_op
    switch(fsm){
        is(idle){
            when(io.sram.req_ren){// have read request
                fsm := Mux(io.axi.arready,wait_resp,have_req_r)
                op  := SRAM_AXIop.save_info | SRAM_AXIop.set_arv
                is_read := true.B
            }.elsewhen(io.sram.req_wen){// have write request
                fsm := Mux(io.axi.awready && io.axi.wready,wait_resp,have_req_w)
                op  := SRAM_AXIop.save_info | SRAM_AXIop.set_awv | SRAM_AXIop.set_wv | Mux(io.axi.awvalid && io.axi.awready, SRAM_AXIop.clear_aww, SRAM_AXIop.no_op)
                is_read := false.B
            }
        }
        is(have_req_w){
            when((aw_sent | io.axi.awready) && (w_sent | io.axi.wready)){
                fsm := wait_resp
                op  := SRAM_AXIop.set_awv | SRAM_AXIop.set_wv | SRAM_AXIop.clear_aww
            }.otherwise{
                op  := SRAM_AXIop.set_awv | SRAM_AXIop.set_wv
            }
        }
        is(have_req_r){
            when(io.axi.arready){
                fsm := wait_resp
                op  := SRAM_AXIop.set_arv
            }
        }
        is(wait_resp){
            when((io.axi.rvalid || io.axi.bvalid)){
                when(io.sram.req_ren && io.sram.ret_ready){// have read request
                    fsm := Mux(io.axi.arready,wait_resp,have_req_r)
                    op  := SRAM_AXIop.save_ret | SRAM_AXIop.set_ready | SRAM_AXIop.save_info | SRAM_AXIop.set_arv
                    is_read := true.B
                }.elsewhen(io.sram.req_wen && io.sram.ret_ready){// have write request
                    fsm := Mux(io.axi.awready && io.axi.wready,wait_resp,have_req_w)
                    op  := SRAM_AXIop.save_ret | SRAM_AXIop.set_ready | SRAM_AXIop.save_info | SRAM_AXIop.set_awv | SRAM_AXIop.set_wv
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
    val reg_PC        = RegInit(0.U(32.W))
    val reg_addr      = RegInit(0.U(32.W))
    val reg_wdata     = RegInit(0.U(32.W))
    val reg_wmask     = RegInit(0.U(4.W))
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
    val aw_sent       = RegInit(false.B)
    val w_sent        = RegInit(false.B) 
    val ready         = RegInit(false.B)
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