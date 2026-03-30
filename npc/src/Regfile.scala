import chisel3._

class Regfile (addr_width:Int=5,data_width:Int=32) extends Module {
    val io = IO(new Bundle{
            val read    = new Bundle{
                val raddr1  = Input(UInt(addr_width.W))
                val raddr2  = Input(UInt(addr_width.W))
                val rdata1  = Output(UInt(data_width.W))
                val rdata2  = Output(UInt(data_width.W))
            }
            val write   = new Bundle{
                val waddr   = Input(UInt(addr_width.W))
                val wdata   = Input(UInt(data_width.W))
                val wen     = Input(Bool())
            }
        })
        
    val regs = RegInit(VecInit(Seq.fill(1 << addr_width)(0.U(data_width.W))))
    when(io.write.wen && io.write.waddr =/= 0.U){
        regs(io.write.waddr) := io.write.wdata 
    }
    io.read.rdata1 := regs(io.read.raddr1)
    io.read.rdata2 := regs(io.read.raddr2)
}