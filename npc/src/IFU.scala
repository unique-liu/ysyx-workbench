import chisel3._

class IFU (initPC:Int=0)extends Module{
    val io = IO(new Bundle{
        val next = new Bundle{
            val valid           = Output(Bool())
            val ready           = Input (Bool())

            val PC              = Output(UInt(32.W))
            val inst            = Output(UInt(32.W))
            val branchPC        = Input (UInt(32.W))
            val ifbranch        = Input (Bool())
        }
        val mem = new Bundle{
            val PC              = Output(UInt(32.W))
            val inst            = Input (UInt(32.W))
        }
    })
    val valid                   = RegInit(1.U(1.W))
    val will_out                = Bool()
    will_out                    := io.next.ready & io.next.valid

    val regPC                   = RegInit(initPC.U(32.W))

    when(io.next.ifbranch){
        regPC                   := io.next.branchPC
    }.otherwise{
        regPC                   := regPC + 4.U
    }

    io.next.PC                  := regPC
    io.mem.PC                   := regPC
    io.mem.inst                 := io.mem.inst

}