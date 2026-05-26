import chisel3._
import chisel3.util._
import Memop.l_byte_s

class LSU extends Module{
    val io = IO(new Bundle{
        val before = new Bundle{
            val valid           = Input (Bool())
            val ready           = Output(Bool())
            val PC              = Input (UInt(32.W))
            val alu_result      = Input (UInt(32.W))
            val reg_op          = Input (UInt(Regop.op_width.W))
            val reg_rd          = Input (UInt(5.W))
            val mem_op          = Input (UInt(Memop.op_width.W))
            val mem_src         = Input (UInt(32.W))
            val debug = new Bundle{
                val inst            = Input (UInt(32.W))
                val branch          = Input (Bool())
                val branch_target   = Input (UInt(32.W))
            }
            val CSR_info        = Input (new CSR_info)
        }
        val next = new Bundle{
            val valid           = Output(Bool())
            val ready           = Input (Bool())
            val PC              = Output(UInt(32.W))
            val alu_result      = Output(UInt(32.W))
            val reg_op          = Output(UInt(Regop.op_width.W))
            val reg_rd          = Output(UInt(5.W))
            val mem_result      = Output(UInt(32.W))
            val debug = new Bundle{
                val inst            = Output(UInt(32.W))
                val branch          = Output(Bool())
                val branch_target   = Output(UInt(32.W))
            }
            val CSR_info        = Output(new CSR_info)
            val exception       = Input (Bool())
        }
        val sram_PC              = Output(UInt(32.W))
        val sram = new SRAM
        val forward = new Bundle{
            val reg_wdata       = Output(UInt(32.W))
            val reg_rd          = Output(UInt(5.W))
            val reg_useable     = Output(Bool())
        }
        val flush           = Input(Bool())
    })
    //dclarations
        //state machine
        val idle :: send_addr :: wait_mem :: ready :: wait_error_mem :: Nil = Enum(5)
        val lsus                    = RegInit(idle)
        val op                      = Wire(UInt(LSUop.width.W))
        //latching signals
        val reg_PC                  = Reg(UInt(32.W))
        val reg_alu_result          = Reg(UInt(32.W))
        val reg_reg_op              = Reg(UInt(Regop.op_width.W))
        val reg_reg_rd              = Reg(UInt(5.W))
        val reg_mem_op              = Reg(UInt(Memop.op_width.W))
        val reg_mem_src             = Reg(UInt(32.W))
        val reg_mem_mask            = Reg(UInt(4.W))
        val reg_debug               = Reg(new debug)
        val reg_CSR_info            = Reg(new CSR_info)
        //preparing mask
        val alu_result_2 = io.before.alu_result(1,0)
        val mem_mask                = Wire(UInt(4.W))
        //sram
        val ret_rdata               = RegInit(0.U(32.W))
        val ret_resp                = RegInit(0.U(2.W))
        //output
        val mem_out_aligned         = Wire(UInt(32.W))
        //CSR
        val have_exception          = Wire(Bool())
        
    //fluiding control signals
    val valid                   = RegInit(0.U(1.W))
    val will_out                = Wire(Bool())
    val will_in                 = Wire(Bool())
    when(io.flush){
        valid                   := 0.U(1.W)
    }.elsewhen(will_in){
        valid                   := 1.U(1.W)
    }.elsewhen(will_out){
        valid                   := 0.U(1.W)
    }

    will_out                    := io.next.ready & io.next.valid
    will_in                     := io.before.valid & io.before.ready
    io.before.ready             := !valid | will_out
    io.next.valid               := valid & ((lsus === ready))

    //state machine
    op                          := LSUop.no_op
    switch(lsus){
        is(idle){
            when(will_in && (io.before.mem_op =/= Memop.noop)){
                lsus                := send_addr
                op                  := LSUop.no_op
            }.elsewhen(will_in && (io.before.mem_op === Memop.noop)){
                lsus                := ready
                op                  := LSUop.no_op
            }
        }
        is(send_addr){
            when(io.flush){
                lsus                := idle
                op                  := LSUop.no_op
            }.elsewhen(io.sram.req_ready){
                lsus                := wait_mem
                op                  := LSUop.no_op
            }
        }
        is(wait_mem){
            when(io.flush && !io.sram.ret_valid){
                lsus                := wait_error_mem
                op                  := LSUop.no_op
            }.elsewhen(io.flush && io.sram.ret_valid){
                lsus                := idle
                op                  := LSUop.no_op
            }.elsewhen(io.sram.ret_valid){
                lsus                := ready
                op                  := LSUop.save_ret
            }
        }
        is(ready){
            when(io.flush){
                lsus                := idle
                op                  := LSUop.no_op
            }.elsewhen(will_in && (io.before.mem_op =/= Memop.noop)){
                lsus                := send_addr
                op                  := LSUop.no_op
            }.elsewhen(will_in && (io.before.mem_op === Memop.noop)){
                lsus                := ready
                op                  := LSUop.no_op
            }.elsewhen(will_out){
                lsus                := idle
                op                  := LSUop.no_op
            }
        }
        is(wait_error_mem){
            when(io.sram.ret_valid){
                lsus                := idle
            }
        }
    }

    //latching signals
    when(will_in){
        reg_PC                  := io.before.PC
        reg_alu_result          := io.before.alu_result
        reg_reg_op              := io.before.reg_op  
        reg_reg_rd              := io.before.reg_rd
        reg_mem_op              := io.before.mem_op
        reg_mem_src             := io.before.mem_src
        reg_mem_mask            := mem_mask
        reg_debug               := io.before.debug
        reg_CSR_info            := io.before.CSR_info
    }


    //preparing wmask
    mem_mask                    := 0.U
    // switch(io.before.mem_op(Memop.half_bit,Memop.byte_bit)){
    //     is("b01".U){mem_mask := "b0001".U}
    //     is("b10".U){mem_mask := "b0011".U}
    //     is("b11".U){mem_mask := "b1111".U}
    // }//sram supports unaligned access, so no need to prepare different mask for different address offset
    switch(io.before.mem_op(Memop.half_bit,Memop.byte_bit)){
        is("b01".U){
            switch(alu_result_2){
                is("b00".U){mem_mask := "b0001".U}
                is("b01".U){mem_mask := "b0010".U}
                is("b10".U){mem_mask := "b0100".U}
                is("b11".U){mem_mask := "b1000".U}
            }
        }
        is("b10".U){mem_mask := Mux(alu_result_2 === "b00".U, "b0011".U, "b1100".U)}
        is("b11".U){mem_mask := "b1111".U}
    }

    //sram
    io.sram_PC                  := reg_PC
    io.sram.req_ren             := (lsus === send_addr) & reg_mem_op(Memop.load_bit) & !have_exception & !io.flush
    io.sram.req_wen             := (lsus === send_addr) & !reg_mem_op(Memop.load_bit) & (reg_mem_op =/= Memop.noop) & !have_exception & !io.flush
    io.sram.addr                := Cat(reg_alu_result(31,2),0.U(2.W))
    io.sram.wdata               := 0.U(32.W)//reg_mem_src
    switch(reg_mem_op(Memop.half_bit,Memop.byte_bit)){
        is("b01".U){io.sram.wdata := Cat(reg_mem_src(7,0),reg_mem_src(7,0),reg_mem_src(7,0),reg_mem_src(7,0))}
        is("b10".U){io.sram.wdata := Cat(reg_mem_src(15,0),reg_mem_src(15,0))}
        is("b11".U){io.sram.wdata := reg_mem_src}
    }
    io.sram.wmask               := reg_mem_mask
    io.sram.ret_ready := (lsus === wait_mem) | (lsus === wait_error_mem)

    when(op(LSUop.save_ret_bit)){
        ret_rdata               := io.sram.rdata
        ret_resp                := io.sram.resp
    }

    //output
    io.next.PC                  := reg_PC
    io.next.alu_result          := reg_alu_result
    io.next.reg_op              := reg_reg_op
    io.next.reg_rd              := reg_reg_rd
    io.next.debug               := reg_debug
    io.next.CSR_info            := reg_CSR_info

    mem_out_aligned             := Mux(op(LSUop.save_ret_bit),io.sram.rdata,ret_rdata) >> Cat(reg_alu_result(1,0),0.U(3.W))
    // mem_out_aligned             := Mux(op(LSUop.save_ret_bit),io.sram.rdata,ret_rdata)//memory support unaligned, so no need to shift
    io.next.mem_result          := 0.U(32.W)
    switch(reg_mem_op){
        is(Memop.l_byte_u){io.next.mem_result      := Cat(0.U(24.W),mem_out_aligned(7,0))}
        is(Memop.l_byte_s){io.next.mem_result      := Cat(Fill(24,mem_out_aligned(7)),mem_out_aligned(7,0))}
        is(Memop.l_half_u){io.next.mem_result      := Cat(0.U(16.W),mem_out_aligned(15,0))}
        is(Memop.l_half_s){io.next.mem_result      := Cat(Fill(16,mem_out_aligned(15)),mem_out_aligned(15,0))}
        is(Memop.l_word  ){io.next.mem_result      := mem_out_aligned}
    }

    //forwarding
    io.forward.reg_wdata        := Mux(reg_reg_op(Regop.mem_bit),io.next.mem_result,reg_alu_result)
    io.forward.reg_rd           := Mux(reg_reg_op(Regop.write_bit) && (valid === 1.U),reg_reg_rd,0.U(5.W))
    io.forward.reg_useable      := io.next.valid

    //CSR
    have_exception              := io.next.exception | reg_CSR_info.exception //LSU or WBU can raise exception, so do not real access memory
}