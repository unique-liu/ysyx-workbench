import chisel3._
import chisel3.util._
object Cacheop{
    val width           = 7
    val save_info_bit   = 0
    val save_ret_bit    = 1
    val set_awv_bit     = 2
    val set_wv_bit      = 3
    val set_arv_bit     = 4
    val set_ready_bit   = 5
    val clear_aww_bit   = 6
    
    val no_op           = "b0000000".U(width.W)
    val save_info       = "b0000001".U(width.W)//save addr, wdata, wmask, size 
    val save_ret        = "b0000010".U(width.W)//in cache this is no use
    val set_awv         = "b0000100".U(width.W)//in cache this is no use
    val set_wv          = "b0001000".U(width.W)//in cache this is no use
    val set_arv         = "b0010000".U(width.W)//in cache this is no use
    val set_ready       = "b0100000".U(width.W)//in cache this is no use
    val clear_aww       = "b1000000".U(width.W)//clear awvalid and wvalid after send aw and w, wait for response

}

//offset_bits: use [offset_bits-1:0] of addr to find data, effect the line size, can not be less than 2 because a word is 4 bytes
//index_bits: use [index_bits+offset_bits-1:offset_bits] of addr to find the group, effect the number of groups can be 0 for fully associative
//            use [31:index_bits+offset] of addr as tag
//lines_per_group: effect the number of lines in a group
class Cache(axi_id:Int=0,offset_bits:Int=4,index_bits:Int=4,lines_per_group:Int=2) extends Module{//SRAM to AXI4 bridge
    val io = IO(new Bundle{
        val PC              = Input(UInt(32.W))
        val sram            = Flipped(new SRAM)
        val axi_PC          = Output(UInt(32.W))
        val axi             = new AXI4
    })   
    //prepare args
    val tag_bits = 32 - index_bits - offset_bits
    val line_size = 1 << offset_bits
    val num_groups = if(index_bits == 0) 1 else 1 << index_bits
    assert(offset_bits >= 2, "offset_bits must be at least 2")
    assert(lines_per_group >= 1, "lines_per_group must be at least 1")
    //create mem
    val tvd_len = (tag_bits + 2) * lines_per_group//valid bit + dirty bit + tag
    val mem_tvd = SyncReadMem(num_groups, UInt(tvd_len.W))
    val data_len = line_size * 8 // 8 bits per byte
    val mem_datas = Seq.fill(lines_per_group)(SyncReadMem(num_groups, UInt(data_len.W)))

    //declarations
        //state machine
        val idle :: look_up :: send_wreq :: send_rreq :: get_ret :: Nil = Enum(4)
        val op                  = Wire(UInt(Cacheop.width.W))
        val fsm                 = RegInit(idle)
        val is_read             = RegInit(false.B)
        //info  
        val reg_PC              = RegInit(0.U(32.W))
        val reg_addr            = RegInit(0.U(32.W))
        val reg_wdata           = RegInit(0.U(32.W))
        val reg_wmask           = RegInit(0.U(4.W))
        val reg_size            = RegInit(0.U(3.W))
        //axi   
        val aw_sent             = RegInit(false.B)
        val w_sent              = RegInit(false.B) 
        val rreq_sent           = Wire(Bool())
        val wreq_sent           = Wire(Bool())
        //cache
        val hit                 = RegInit(false.B)
        val write_back          = RegInit(false.B)
        val refill_cnt          = RegInit(0.U((line_size/4).W))//count how many words have been refilled, used in burst write

    //state machine
    op                      := Cacheop.no_op
    switch(fsm){
        is(idle){// wait for req
            when(io.sram.req_valid){
                op          := Cacheop.no_op
                fsm         := look_up
            }
        }
        is(look_up){// look up cache, if hit then return data or write data 
            when(hit && io.sram.ret_ready){
                op          := Cacheop.no_op
                fsm         := idle
            }.elsewhen(!hit && write_back){
                op          := Cacheop.no_op
                fsm         := send_wreq
            }.elsewhen(!hit && !write_back){
                op          := Cacheop.no_op
                fsm         := send_rreq
            }
        }
        is(send_wreq){ // send dirty line back to memory
            when(wreq_sent){
                op          := Cacheop.no_op
                fsm         := send_rreq
            }
        }
        is(send_rreq){ // send read request to axi
            when(rreq_sent || wreq_sent){
                op          := Cacheop.no_op
                fsm         := get_ret
            }
        }
        is(get_ret){ // write data(if is write, then axi data will be modified) to cache, and return data
            when(io.axi.r.valid && io.axi.r.last){
                op          := Cacheop.no_op
                fsm         := idle
            }.otherwise{
                op          := Cacheop.no_op
            }
        }
    }
    //
    
    //axi
    io.axi_PC                       := 
    io.axi.aw.valid                 := 
    io.axi.aw.addr                  := 
    io.axi.aw.id                    := 
    io.axi.aw.len                   := 
    io.axi.aw.size                  := 
    io.axi.aw.burst                 := 

    io.axi.w.valid                  := 
    io.axi.w.data                   := 
    io.axi.w.strb                   := 
    io.axi.w.last                   := 

    io.axi.b.ready                  := 

    io.axi.ar.valid                 := 
    io.axi.ar.addr                  := 
    io.axi.ar.id                    := 
    io.axi.ar.len                   := 
    io.axi.ar.size                  := 
    io.axi.ar.burst                 := 

    io.axi.r.ready                  := 

    //sram
    io.sram.req_ready               := 
    io.sram.ret_valid               := 
    io.sram.rdata                   := 
    io.sram.resp                    := 

}