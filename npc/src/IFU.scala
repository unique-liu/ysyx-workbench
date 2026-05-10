import chisel3._
import chisel3.util._

class IFU (initPC:Int=0)extends Module{
    val io = IO(new Bundle{
        val before = new Bundle{
            val valid           = Input (Bool())
            val ready           = Output(Bool())
        }
        val next = new Bundle{
            val valid           = Output(Bool())
            val ready           = Input (Bool())

            val PC              = Output(UInt(32.W))
            val inst            = Output(UInt(32.W))
            val branchPC        = Input (UInt(32.W))
            val ifbranch        = Input (Bool())
        }
        val axi_PC              = Output(UInt(32.W))
        val axi = new AXI4Lite
        val csr_flush = new Bundle{
            val flush           = Input (Bool())
            val target          = Input (UInt(32.W))
        }
    })
    //declare
        //state machine
    val idle :: wait_inst :: ready :: wait_error_inst :: Nil = Enum(4)
    val ifus                    = RegInit(idle)
    val op                      = Wire(UInt(IFUop.width.W))
        //PC and inst
    val regPC                   = RegInit(initPC.U(32.W))
    val reginst                 = RegInit(0.U(32.W))
    val nextPC                  = Wire(UInt(32.W))
    val changePC                = Wire(Bool())

    //fluiding control signals
    val valid                   = RegInit(0.U(1.W))
    val will_out                = Wire(Bool())
    val will_in                 = Wire(Bool())
    when(will_in){
        valid                   := 1.U(1.W)
    }.elsewhen(will_out){
        valid                   := 0.U(1.W)
    }
    will_out                    := io.next.ready & io.next.valid
    will_in                     := io.before.valid & io.before.ready
    io.before.ready             := !valid | will_out
    io.next.valid               := (ifus === ready) | (ifus === wait_inst & !changePC & io.axi.rvalid)

    //state machine
    // val idle :: wait_inst :: ready :: wait_error_inst :: Nil = Enum(4)
    // val ifus                    = RegInit(idle)
    // val op                      = Wire(UInt(IFUop.width.W))
    op                          := IFUop.no_op
    switch(ifus){
        is(idle){
            when(io.axi.arready){
                ifus                := wait_inst
                op                  := Mux(changePC, IFUop.save_pc, IFUop.no_op)
            }
        }
        is(wait_inst){
            when(changePC && !io.axi.rvalid){
                ifus                := wait_error_inst
                op                  := IFUop.save_pc
            }.elsewhen(changePC && io.axi.rvalid){
                ifus                := idle
                op                  := IFUop.save_pc
            }.elsewhen(io.axi.rvalid && !will_out){
                ifus                := ready
                op                  := IFUop.save_inst
            }.elsewhen(io.axi.rvalid && will_out){
                ifus                := idle
                op                  := IFUop.save_inst | IFUop.save_pc
            }
        }
        is(ready){
            when(will_out | changePC){
                ifus                := idle
                op                  := IFUop.save_pc
            }
        }
        is(wait_error_inst){
            when(io.axi.rvalid){
                ifus                := idle
            }
        }
    }


    //PC and inst
    // val regPC                   = RegInit(initPC.U(32.W))
    // val reginst                 = RegInit(0.U(32.W))
    // val nextPC                  = Wire(UInt(32.W))
    // val changePC                = Wire(Bool())
    changePC                    := io.csr_flush.flush || io.next.ifbranch
    when(io.csr_flush.flush){
        nextPC                  := io.csr_flush.target
    }.elsewhen(io.next.ifbranch){
        nextPC                  := io.next.branchPC
    }.otherwise{
        nextPC                  := regPC + 4.U
    }
    when(op(IFUop.save_pc_b)){
        regPC                   := nextPC
    }
    when(op(IFUop.save_inst_b)){
        reginst                 := io.axi.rdata
    }

    io.next.PC                  := regPC
    io.next.inst                := Mux((ifus === ready), reginst, io.axi.rdata)
    
    //axi
    io.axi_PC                   := 0.U(32.W) //to show this is IF
    io.axi.arvalid              := (ifus === idle)
    io.axi.araddr               := Mux(changePC, nextPC, regPC)
    io.axi.awvalid              := false.B  //no use
    io.axi.awaddr               := 0.U      //no use
    io.axi.wvalid               := false.B  //no use
    io.axi.wdata                := 0.U      //no use
    io.axi.wstrb                := 0.U      //no use
    io.axi.rready               := (ifus === wait_inst) | (ifus === wait_error_inst)
    io.axi.bready               := false.B  //no use

}