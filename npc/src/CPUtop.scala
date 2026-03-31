import chisel3._

class CPUtop extends Module{
    val io = IO(new Bundle{
        val useless             = Input(Bool())
    })

    val u_regfile               = Module(new Regfile())
    val u_i_mem                 = Module(new MemIO())
    val u_d_mem                 = Module(new MemIO())
    val u_ifu                   = Module(new IFU(initPC=0x7fff_fffc))
    val u_idu                   = Module(new IDU())
    val u_exu                   = Module(new EXU())
    val u_lsu                   = Module(new LSU())
    val u_wbu                   = Module(new WBU())

    u_ifu.io.before.valid           := true.B
    u_ifu.io.next                   <> u_idu.io.before
    u_idu.io.next                    <> u_exu.io.before
    u_exu.io.next                    <> u_lsu.io.before
    u_lsu.io.next                    <> u_wbu.io.before
    u_wbu.io.next.ready              := true.B

    u_ifu.io.memio                   <> u_i_mem.io
    u_lsu.io.memio                   <> u_d_mem.io

    u_idu.io.regfile                <> u_regfile.io.read
    u_wbu.io.regfile                <> u_regfile.io.write
}