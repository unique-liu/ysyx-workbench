import chisel3._

class debug extends Bundle{
    val inst            = UInt(32.W)
    val branch          = Bool()
    val branch_target   = UInt(32.W)
}

class DebugIO extends ExtModule{
    val io = IO(new Bundle {
        val clock = Input (Bool())
        val pc    = Input (UInt(32.W))
        val inst  = Input (UInt(32.W))
        val submit= dontTouch(Input (Bool()))
        val rd    = Input (UInt(5.W))
        val wdata = Input (UInt(32.W))
        val wen   = Input (Bool())
        val target= Input (UInt(32.W))
        val rs1   = Input (UInt(5.W))
        val branch= Input (Bool())
    })

}