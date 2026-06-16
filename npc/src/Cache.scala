import chisel3._
import chisel3.util._
object Cacheop{
    val width           = 5
    val save_info_bit   = 0
    val save_replace_bit= 1
    val save_refill_bit = 2
    val refill_mem_bit  = 3
    val uc_return_bit   = 4
    
    val no_op           = "b00000".U(width.W)
    val save_info       = "b00001".U(width.W)//save addr, wdata, wmask, size 
    val save_replace    = "b00010".U(width.W)
    val save_refill     = "b00100".U(width.W)
    val refill_mem      = "b01000".U(width.W)//in cache this is no use
    val uc_return       = "b10000".U(width.W)
}

// 定义缓存行数据结构（包含 valid, tag, data）
class CacheLine(val tag_bits: Int, val lineWords: Int) extends Bundle {
  val valid = Bool()
  val tag   = UInt(tag_bits.W)
  val data  = Vec(lineWords, UInt(32.W))  // 假设每字 32 位
}


//offset_bits: use [offset_bits-1:0] of addr to find data, effect the line size,must larger than 2 because a word is 4 bytes
//index_bits: use [index_bits+offset_bits-1:offset_bits] of addr to find the group, effect the number of groups can be 0 for fully associative
//            use [31:index_bits+offset] of addr as tag
//group_bits: have 2^group_bits lines in a group
class iCache(axi_id:Int=0,offset_bits:Int=4,index_bits:Int=4,group_bits:Int=1) extends Module{//SRAM to AXI4 bridge
    val io = IO(new Bundle{
        val PC              = Input(UInt(32.W))
        val sram            = Flipped(new SRAM)
        val axi_PC          = Output(UInt(32.W))
        val axi             = new AXI4
    })   
    //prepare args
    assert(offset_bits > 2, "offset_bits must be at least 2")
    assert(group_bits >= 1, "group_bits must be at least 1")    
    val tag_bits = 32 - index_bits - offset_bits
    val line_bytes = 1 << offset_bits
    val line_words = line_bytes / 4
    val num_groups = if(index_bits == 0) 1 else 1 << index_bits
    val lines_per_group = 1 << group_bits
    val offset_start_bit = 0
    val offset_end_bit = offset_bits - 1
    val word_offset_start_bit = 2//because a word is 4 bytes
    val word_offset_end_bit = offset_bits - 1
    val index_start_bit = offset_bits
    val index_end_bit = offset_bits + index_bits - 1
    val tag_start_bit = offset_bits + index_bits
    val tag_end_bit = 31
    //create mem
    val mem_datas = Seq.fill(lines_per_group)(SyncReadMem(num_groups, UInt((new CacheLine(tag_bits, line_words)).getWidth.W)))

    //declarations
        //state machine
        val idle :: look_up :: send_rreq :: get_ret :: uc_send_rreq :: uc_get_ret :: Nil = Enum(6)
        val op                  = Wire(UInt(Cacheop.width.W))
        val fsm                 = RegInit(idle)
        //info  
        val reg_addr            = RegInit(0.U(32.W))
        val can_cache           = Wire(Bool())//indicate if the current access can be cached
        //cache
        val read_lines          = Wire(Vec(lines_per_group, new CacheLine(tag_bits, line_words)))
        val hit_vec             = Wire(Vec(lines_per_group, Bool()))
        val hit                 = Wire(Bool())
        val hit_index           = Wire(UInt(group_bits.W))
        val hit_data            = Wire(UInt(32.W))
        val invalid_vec         = Wire(Vec(lines_per_group, Bool()))
        val have_invalid        = Wire(Bool())
        val count_down_vec      = RegInit(1.U(lines_per_group.W))//use count down: replace 3 -> 2 -> 1 -> 0 -> 3 ...
        val replace_vec         = RegInit(0.U(lines_per_group.W))
        val refill_cnt          = RegInit(0.U((offset_bits - 2).W))//count how many words have been refilled, used in burst write
        val refill_buf          = Reg(new CacheLine(tag_bits, line_words))
        val refill_wire         = Wire(new CacheLine(tag_bits, line_words))

    //state machine
    op                      := Cacheop.no_op
    switch(fsm){
        is(idle){// wait for req
            when(io.sram.req_ren && can_cache){
                op          := Cacheop.save_info
                fsm         := look_up
            }.elsewhen(io.sram.req_ren && !can_cache){
                op          := Cacheop.save_info
                fsm         := uc_send_rreq
            }
        }
        is(look_up){// look up cache, if hit then return data or write data 
            when(hit && io.sram.ret_ready){
                op          := Cacheop.no_op
                fsm         := idle
            }.elsewhen(!hit){
                op          := Cacheop.save_replace
                fsm         := send_rreq
            }
        }
        is(send_rreq){ // send read request to axi
            when(io.axi.ar.ready){
                op          := Cacheop.no_op
                fsm         := get_ret
            }
        }
        is(get_ret){ // write data(if is write, then axi data will be modified) to cache, and return data
            when(io.axi.r.valid && io.axi.r.last){
                op          := Cacheop.save_refill | Cacheop.refill_mem
                fsm         := idle
                refill_cnt  := 0.U
            }.elsewhen(io.axi.r.valid){
                op          := Cacheop.save_refill
                refill_cnt  := refill_cnt + 1.U
            }.otherwise{
                op          := Cacheop.no_op
            }
        }
        is(uc_send_rreq){ // send read request to axi when uncached access
            when(io.axi.ar.ready){
                op          := Cacheop.no_op
                fsm         := uc_get_ret
            }
        }
        is(uc_get_ret){ // return data when uncached access
            when(io.axi.r.valid){
                op          := Cacheop.uc_return
                fsm         := idle
            }
        }
    }

    //Cache logic
    if (Config.use_soc){
        can_cache :=  io.sram.addr(31, 28) === 0xa.U 
    }else{
        can_cache := true.B
    }

    read_lines.zip(mem_datas).foreach{ case(line, mem) =>
        val readUInt = mem.read(io.sram.addr(index_end_bit, index_start_bit))
        line := readUInt.asTypeOf(new CacheLine(tag_bits, line_words))
    }
    for(i <- 0 until lines_per_group){
        hit_vec(i) := read_lines(i).tag === reg_addr(tag_end_bit, tag_start_bit) && read_lines(i).valid//valid bit
        invalid_vec(i) := !read_lines(i).valid//invalid when valid bit is 0
    }
    hit := hit_vec.reduce(_ || _)
    hit_index := PriorityEncoder(hit_vec)
    hit_data := read_lines(hit_index).data(reg_addr(word_offset_end_bit, word_offset_start_bit))

    have_invalid := invalid_vec.reduce(_ || _)

    when(op(Cacheop.save_replace_bit)){
        replace_vec         := Mux(have_invalid, invalid_vec.asUInt, count_down_vec)
        count_down_vec      := Mux(have_invalid, count_down_vec, Cat(count_down_vec(lines_per_group-2, 0), count_down_vec(lines_per_group-1)))
        refill_buf.valid    := true.B
        refill_buf.tag      := reg_addr(tag_end_bit, tag_start_bit)
    }

    refill_wire := refill_buf
    refill_wire.data(line_words-1) := io.axi.r.data
    when(op(Cacheop.refill_mem_bit)){  
        // mem_datas(replace_index).write(reg_addr(index_end_bit, index_start_bit), refill_wire.asUInt)
        for(i <- 0 until lines_per_group){
            when(replace_vec(i)){
                mem_datas(i).write(reg_addr(index_end_bit, index_start_bit), refill_wire.asUInt)
            }
        }
    }.elsewhen(op(Cacheop.save_refill_bit)){
        refill_buf.data(refill_cnt) := io.axi.r.data
    }

    

    //sram
    io.sram.req_ready               := (fsm === idle)
    when(op(Cacheop.save_info_bit)){
        reg_addr    := io.sram.addr 
    }

    io.sram.ret_valid               :=  (fsm === look_up) && hit || //cache hit
                                        op(Cacheop.save_refill_bit) && (refill_cnt === reg_addr(word_offset_end_bit, word_offset_start_bit)) || // cache miss
                                        op(Cacheop.uc_return_bit) // uncached access
    io.sram.rdata                   := Mux(fsm === look_up && hit, hit_data, io.axi.r.data)
    io.sram.resp                    := 0.U//in this design, we do not consider error, so always return 0
    
    //axi  this is iCache, so only read is needed
    io.axi_PC                       := Mux(fsm === uc_send_rreq, 0x0000ff00f.U, 0x000000ff.U)//use a special PC to indicate if
    io.axi.aw.valid                 := false.B
    io.axi.aw.addr                  := 0.U
    io.axi.aw.id                    := 0.U
    io.axi.aw.len                   := 0.U
    io.axi.aw.size                  := 0.U
    io.axi.aw.burst                 := 0.U

    io.axi.w.valid                  := false.B
    io.axi.w.data                   := 0.U
    io.axi.w.strb                   := 0.U
    io.axi.w.last                   := 0.U

    io.axi.b.ready                  := false.B

    io.axi.ar.valid                 := fsm === send_rreq || fsm === uc_send_rreq
    io.axi.ar.addr                  := Mux(fsm === uc_send_rreq, reg_addr, Cat(reg_addr(tag_end_bit, index_start_bit),0.U(offset_bits.W)))//align to line
    io.axi.ar.id                    := axi_id.U
    io.axi.ar.len                   := Mux(fsm === uc_send_rreq, 0.U,(line_words - 1).U)//number of beats in a burst, minus 1 because len starts from 0
    io.axi.ar.size                  := 2.U//if always read 4 bytes
    io.axi.ar.burst                 := AXI_BURST.INCR

    io.axi.r.ready                  := fsm === get_ret || fsm === uc_get_ret

    if(Config.perf_on){
        //perf-icache
        val perf_icache             = Module(new perf(PT.icache))
        val icache_hit              = hit && (fsm === look_up)
        val icache_miss             = !hit && (fsm === look_up)
        val icache_refill           = fsm === get_ret || fsm === send_rreq
        val icache_cycle            = (fsm === look_up) || (fsm === idle) && io.sram.req_ren //count cycle when look up or wait for req
        val icache_code             = Cat(icache_cycle, icache_refill, icache_miss, icache_hit)
        perf_icache.io.valid        := icache_hit || icache_miss || icache_refill || icache_cycle
        perf_icache.io.code         := icache_code
    }
}