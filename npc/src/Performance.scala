import chisel3._

object PT {// Performance Type
  val perf_width = 8

  val inv           = 0.U(8.W)// invalid
  val it            = 1.U(8.W)// instruction type
    val it_inv      = 0.U(8.W)// code: invalid
    val it_c        = 1.U(8.W)// code: calculate
    val it_b        = 2.U(8.W)// code: branch
    val it_s        = 3.U(8.W)// code: store
    val it_l        = 4.U(8.W)// code: load
    val it_csr      = 5.U(8.W)// code: csr
    val it_crt      = 6.U(8.W)// code: control
  val ini           = 2.U(8.W)//ifu_no_inst
    val ini_inv      = 0.U(8.W)// code: invalid
    val ini_w_lsu    = 1.U(8.W)// code: wait lsu
    val ini_w_err    = 2.U(8.W)// code: wait error
    val ini_w_mem    = 3.U(8.W)// code: wait memory
  val lmd           = 3.U(8.W)//lsu_mem_delay
    val lmd_inv      = 0.U(8.W)// code: invalid
    val lmd_w_ifu   = 1.U(8.W)// code: wait ifu
    val lmd_w_err   = 2.U(8.W)// code: wait error
    val lmd_w_mem   = 3.U(8.W)// code: wait memory
    val lmd_total   = 4.U(8.W)// code: total mem cycle
  val icache        = 4.U(8.W)//icache
    val icache_hit   = "b00000001".U(8.W)// code: hit
    val icache_miss  = "b00000010".U(8.W)// code: miss
    val icache_refill= "b00000100".U(8.W)// code: miss refill cycle
    val icache_cycle = "b00001000".U(8.W)// code: normal access cycle
}



class PerformanceIO extends ExtModule{
    val io = IO(new Bundle {
        val clock       = Input (Bool())
        val valid       = Input (Bool())
        val perf_type   = Input (UInt(8.W))
        val code        = Input (UInt(8.W))
    })

}
class perf(perf_type: UInt, nocode: Int = 0) extends Module {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val code  = if (nocode == 0) Input(UInt(8.W)) else Input(UInt(0.W)) // 宽度0表示不存在
  })
  val perf_io = Module(new PerformanceIO)
  perf_io.io.clock := clock.asBool
  perf_io.io.valid := io.valid
  perf_io.io.perf_type := perf_type
  if (nocode == 0) {
    perf_io.io.code := io.code
  } else {
    perf_io.io.code := 0.U
  }
  // 将 perf_io 的输出连接到模块输出（如果需要）
  // io.out := perf_io.io.out
}