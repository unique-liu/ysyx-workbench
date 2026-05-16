import chisel3._

class CPUtop extends Module{
    val io = IO(new Bundle{
        val useless                     = Input(Bool())
    })

    val u_regfile                       = Module(new Regfile())
    
    val u_i_switch                      = Module(new SRAM_AXI())
    val u_d_switch                      = Module(new SRAM_AXI())
    val u_crossbar                      = Module(new AXI_Crossbar())
    val u_mem                         = Module(new Mem_AXI())

    val u_ifu                           = Module(new IFU(initPC=0x80000000))
    val u_idu                           = Module(new IDU())
    val u_exu                           = Module(new EXU())
    val u_lsu                           = Module(new LSU())
    val u_wbu                           = Module(new WBU())
    val u_csr                           = Module(new CSR())

    //connection
    //pipeline control
    u_ifu.io.before.valid               := true.B
    u_ifu.io.next                       <> u_idu.io.before
    u_idu.io.next                       <> u_exu.io.before
    u_exu.io.next                       <> u_lsu.io.before
    u_lsu.io.next                       <> u_wbu.io.before
    u_wbu.io.next.ready                 := true.B

    //bus
    u_i_switch.io.PC                    := u_ifu.io.sram_PC
    u_ifu.io.sram                       <> u_i_switch.io.sram
    u_d_switch.io.PC                    := u_lsu.io.sram_PC
    u_lsu.io.sram                       <> u_d_switch.io.sram

    u_crossbar.io.ifu_PC                := u_ifu.io.sram_PC
    u_crossbar.io.ifu_axi               <> u_i_switch.io.axi
    u_crossbar.io.lsu_PC                := u_lsu.io.sram_PC
    u_crossbar.io.lsu_axi               <> u_d_switch.io.axi

    u_mem.io.rPC                        := u_crossbar.io.mem_rPC
    u_mem.io.wPC                        := u_crossbar.io.mem_wPC
    u_mem.io.axi                        <> u_crossbar.io.mem_axi

    //regfile interface
    u_idu.io.regfile                    <> u_regfile.io.read
    u_wbu.io.regfile                    <> u_regfile.io.write

    //forwarding interface
    u_idu.io.EXU_forward                <> u_exu.io.forward
    u_idu.io.LSU_forward                <> u_lsu.io.forward
    u_idu.io.WBU_forward                <> u_wbu.io.forward

    //CSR interface
    u_ifu.io.csr_flush                  <> u_csr.io.csr_flush
    u_idu.io.csr_read                   <> u_csr.io.csr_read
    u_idu.io.flush                      := u_csr.io.csr_flush.flush
    u_exu.io.flush                      := u_csr.io.csr_flush.flush
    u_lsu.io.flush                      := u_csr.io.csr_flush.flush
    u_wbu.io.flush                      := u_csr.io.csr_flush.flush
    u_wbu.io.CSR                        <> u_csr.io.CSR
}