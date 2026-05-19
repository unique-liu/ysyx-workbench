import chisel3._

class CPUtop extends Module{
    val io = IO(new Bundle{
        // val interrupt                        = Input (Bool())
        // val master_awready                   = Input (Bool())
        // val master_awvalid                   = Output(Bool())
        // val master_awaddr                    = Output(UInt(32.W))
        // val master_awid                      = Output(UInt(8.W))
        // val master_awlen                     = Output(UInt(4.W))
        // val master_awsize                    = Output(UInt(3.W))
        // val master_awburst                   = Output(UInt(2.W))
        // val master_wready                    = Input (Bool())
        // val master_wvalid                    = Output(Bool())
        // val master_wdata                     = Output(UInt(32.W))
        // val master_wstrb                     = Output(UInt(4.W))
        // val master_wlast                     = Output(Bool())
        // val master_bready                    = Output(Bool())
        // val master_bvalid                    = Input (Bool())
        // val master_bresp                     = Input (UInt(2.W))
        // val master_bid                       = Input (UInt(8.W))
        // val master_arready                   = Input (Bool())
        // val master_arvalid                   = Output(Bool())
        // val master_araddr                    = Output(UInt(32.W))
        // val master_arid                      = Output(UInt(8.W))
        // val master_arlen                     = Output(UInt(4.W))
        // val master_arsize                    = Output(UInt(3.W))
        // val master_arburst                   = Output(UInt(2.W))
        // val master_rready                    = Output(Bool())
        // val master_rvalid                    = Input (Bool())
        // val master_rresp                     = Input (UInt(2.W))
        // val master_rdata                     = Input (UInt(32.W))
        // val master_rlast                     = Input (Bool())
        // val master_rid                       = Input (UInt(8.W))
        
        // val slave_awready                   = Output(Bool())
        // val slave_awvalid                   = Input (Bool())
        // val slave_awaddr                    = Input (UInt(32.W))
        // val slave_awid                      = Input (UInt(8.W))
        // val slave_awlen                     = Input (UInt(4.W))
        // val slave_awsize                    = Input (UInt(3.W))
        // val slave_awburst                   = Input (UInt(2.W))
        // val slave_wready                    = Output(Bool())
        // val slave_wvalid                    = Input (Bool())
        // val slave_wdata                     = Input (UInt(32.W))
        // val slave_wstrb                     = Input (UInt(4.W))
        // val slave_wlast                     = Input (Bool())
        // val slave_bready                    = Input (Bool())
        // val slave_bvalid                    = Output(Bool())
        // val slave_bresp                     = Output(UInt(2.W))
        // val slave_bid                       = Output(UInt(8.W))
        // val slave_arready                   = Output(Bool())
        // val slave_arvalid                   = Input (Bool())
        // val slave_araddr                    = Input (UInt(32.W))
        // val slave_arid                      = Input (UInt(8.W))
        // val slave_arlen                     = Input (UInt(4.W))
        // val slave_arsize                    = Input (UInt(3.W))
        // val slave_arburst                   = Input (UInt(2.W))
        // val slave_rready                    = Input (Bool())
        // val slave_rvalid                    = Output(Bool())
        // val slave_rresp                     = Output(UInt(2.W))
        // val slave_rdata                     = Output(UInt(32.W))
        // val slave_rlast                     = Output(Bool())
        // val slave_rid                       = Output(UInt(8.W))
    })

    val u_regfile                       = Module(new Regfile())
    
    val u_i_switch                      = Module(new SRAM_AXI())
    val u_d_switch                      = Module(new SRAM_AXI())
    val u_arbiter                       = Module(new AXI_Arbiter())
    val u_clint                         = Module(new CLINT())
    val u_mem                           = Module(new Mem_AXI())

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

    u_arbiter.io.ifu_PC                 := u_ifu.io.sram_PC
    u_arbiter.io.ifu_axi                <> u_i_switch.io.axi
    u_arbiter.io.lsu_PC                 := u_lsu.io.sram_PC
    u_arbiter.io.lsu_axi                <> u_d_switch.io.axi

    u_clint.io.rPC                      := u_arbiter.io.clint_PC
    u_clint.io.wPC                      := u_arbiter.io.clint_PC
    u_clint.io.axi                      <> u_arbiter.io.clint_axi
    //this is used to test the cpu
    u_mem.io.rPC                        := u_arbiter.io.out_rPC
    u_mem.io.wPC                        := u_arbiter.io.out_wPC
    u_mem.io.axi                        <> u_arbiter.io.out_axi

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

    //CPU out
    // u_arbiter.io.out_axi.aw.ready       := io.master_awready
    // io.master_awvalid                   := u_arbiter.io.out_axi.aw.valid
    // io.master_awaddr                    := u_arbiter.io.out_axi.aw.addr
    // io.master_awid                      := u_arbiter.io.out_axi.aw.id
    // io.master_awlen                     := u_arbiter.io.out_axi.aw.len
    // io.master_awsize                    := u_arbiter.io.out_axi.aw.size
    // io.master_awburst                   := u_arbiter.io.out_axi.aw.burst
    // u_arbiter.io.out_axi.w.ready        := io.master_wready
    // io.master_wvalid                    := u_arbiter.io.out_axi.w.valid
    // io.master_wdata                     := u_arbiter.io.out_axi.w.data
    // io.master_wstrb                     := u_arbiter.io.out_axi.w.strb
    // io.master_wlast                     := u_arbiter.io.out_axi.w.last
    // io.master_bready                    := u_arbiter.io.out_axi.b.ready
    // u_arbiter.io.out_axi.b.valid        := io.master_bvalid
    // u_arbiter.io.out_axi.b.resp         := io.master_bresp
    // u_arbiter.io.out_axi.b.id           := io.master_bid
    // u_arbiter.io.out_axi.ar.ready       := io.master_arready
    // io.master_arvalid                   := u_arbiter.io.out_axi.ar.valid
    // io.master_araddr                    := u_arbiter.io.out_axi.ar.addr
    // io.master_arid                      := u_arbiter.io.out_axi.ar.id
    // io.master_arlen                     := u_arbiter.io.out_axi.ar.len
    // io.master_arsize                    := u_arbiter.io.out_axi.ar.size
    // io.master_arburst                   := u_arbiter.io.out_axi.ar.burst
    // io.master_rready                    := u_arbiter.io.out_axi.r.ready
    // u_arbiter.io.out_axi.r.valid        := io.master_rvalid
    // u_arbiter.io.out_axi.r.resp         := io.master_rresp
    // u_arbiter.io.out_axi.r.data         := io.master_rdata
    // u_arbiter.io.out_axi.r.last         := io.master_rlast
    // u_arbiter.io.out_axi.r.id           := io.master_rid

    // io.slave_awready                    := 0.U(1.W)
    // // io.slave_awvalid                    := Input (Bool())
    // // io.slave_awaddr                     := Input (UInt(32.W))
    // // io.slave_awid                       := Input (UInt(8.W))
    // // io.slave_awlen                      := Input (UInt(4.W))
    // // io.slave_awsize                     := Input (UInt(3.W))
    // // io.slave_awburst                    := Input (UInt(2.W))
    // io.slave_wready                     := 0.U(1.W)
    // // io.slave_wvalid                     := Input (Bool())
    // // io.slave_wdata                      := Input (UInt(32.W))
    // // io.slave_wstrb                      := Input (UInt(4.W))
    // // io.slave_wlast                      := Input (Bool())
    // // io.slave_bready                     := Input (Bool())
    // io.slave_bvalid                     := 0.U(1.W)
    // io.slave_bresp                      := 0.U(2.W)
    // io.slave_bid                        := 0.U(8.W)
    // io.slave_arready                    := 0.U(1.W)
    // // io.slave_arvalid                    := Input (Bool())
    // // io.slave_araddr                     := Input (UInt(32.W))
    // // io.slave_arid                       := Input (UInt(8.W))
    // // io.slave_arlen                      := Input (UInt(4.W))
    // // io.slave_arsize                     := Input (UInt(3.W))
    // // io.slave_arburst                    := Input (UInt(2.W))
    // // io.slave_rready                     := Input (Bool())
    // io.slave_rvalid                     := 0.U(1.W)
    // io.slave_rresp                      := 0.U(2.W)
    // io.slave_rdata                      := 0.U(32.W)
    // io.slave_rlast                      := 0.U(1.W)
    // io.slave_rid                        := 0.U(8.W)
}