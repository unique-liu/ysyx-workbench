import chisel3._

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
        val memio = new Bundle{
            val clock           = Output(Bool())
            val PC              = Output(UInt(32.W))
            val ren             = Output(Bool())
            val raddr           = Output(UInt(32.W))
            val rdata           = Input (UInt(32.W))

            val wen             = Output(Bool())
            val waddr           = Output(UInt(32.W))
            val wdata           = Output(UInt(32.W))
            val wmask           = Output(UInt(4.W))
        }
    })
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
    io.next.valid               := valid

    val regPC                   = RegInit(initPC.U(32.W))
    val reginst                 = RegInit(0.U(32.W))
    val reg_mismatch            = RegInit(false.B)
    val nextPC                  = Wire(UInt(32.W))
    when(io.next.ifbranch){
        nextPC                  := io.next.branchPC
    }.otherwise{
        nextPC                  := regPC + 4.U
    }
    when(will_in){
        regPC                   := nextPC
    }
    when(!will_out){
        reginst                 := io.memio.rdata
        reg_mismatch            := true.B
    }

    io.next.PC                  := regPC
    io.next.inst                := Mux(reg_mismatch, reginst, io.memio.rdata)
    
    io.memio.clock              := clock.asBool
    io.memio.PC                 := regPC
    io.memio.ren                := !reset.asBool
    io.memio.raddr              := nextPC
    io.memio.wen                := 0.B
    io.memio.waddr              := 0.U
    io.memio.wdata              := 0.U
    io.memio.wmask              := 0.U

}