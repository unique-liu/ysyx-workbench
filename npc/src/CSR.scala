import chisel3._
import chisel3.util._

class CSR_info extends Bundle{
    val addr        = UInt(12.W)
    val wdata       = UInt(32.W)
    val op          = UInt(CSRop.op_width.W)
    val exception   = Bool()
}
class CSR extends Module{
    val io = IO(new Bundle{
        val CSR     = new Bundle{
            val valid           = Input (Bool())
            val PC              = Input (UInt(32.W))
            val info            = Input (new CSR_info)
        }
        val csr_read    = new Bundle{
            val addr    = Input (UInt(12.W))
            val rdata   = Output(UInt(32.W))
        }
        val csr_flush   = new Bundle{
            val flush       = Output(Bool())
            val target      = Output(UInt(32.W))
        }
    })
    val valid = io.CSR.valid & (io.CSR.info.op =/= CSRop.noop)
    val privilege = RegInit(CSRPRIV.M.U(2.W))//current privilege level, default M-mode
    val mcause_val = Wire(UInt(32.W))


    val mstatus = new Bundle{
        val d = new mstatusDEF                      //define
        val f = new RegMultifield(d.field_widths,d.total_width)   //function
        val r = f.init(d.init_val)    //register
    }
    val mtvec = new Bundle{
        val d = new mtvecDEF
        val f = new RegMultifield(d.field_widths,d.total_width)
        val r = f.init(d.init_val)
    }
    val mepc = new Bundle{
        val d = new mepcDEF
        val f = new RegMultifield(d.field_widths,d.total_width)
        val r = f.init(d.init_val)
    }
    val mcause = new Bundle{
        val d = new mcauseDEF
        val f = new RegMultifield(d.field_widths,d.total_width)
        val r = f.init(d.init_val)
    }
    val mcycle = new Bundle{
        val d = new mcycleDEF
        val f = new RegMultifield(d.field_widths,d.total_width)
        val r = f.init(d.init_val)
    }
    val mvendorid = new Bundle{
        val d = new mvendoridDEF
        val f = new RegMultifield(d.field_widths,d.total_width)
        val r = f.init(d.init_val)
    }
    val marchid = new Bundle{
        val d = new marchidDEF
        val f = new RegMultifield(d.field_widths,d.total_width)
        val r = f.init(d.init_val)
    }

    //read
    io.csr_read.rdata := 0.U
    switch(io.csr_read.addr){
        is(CSRCode.mstatus  ){io.csr_read.rdata := mstatus.r}
        is(CSRCode.mtvec    ){io.csr_read.rdata := mtvec.r}
        is(CSRCode.mepc     ){io.csr_read.rdata := mepc.r}
        is(CSRCode.mcause   ){io.csr_read.rdata := mcause.r}
        is(CSRCode.mcycle   ){io.csr_read.rdata := mcycle.r(31,0)}
        is(CSRCode.mcycleh  ){io.csr_read.rdata := mcycle.r(63,32)}
        is(CSRCode.mvendorid){io.csr_read.rdata := mvendorid.r}
        is(CSRCode.marchid  ){io.csr_read.rdata := marchid.r}
    }

    //write
    //mstatus
    when(valid & (io.CSR.info.addr === CSRCode.mstatus)){
        mstatus.r := io.CSR.info.wdata
    }.elsewhen(valid & (io.CSR.info.op === CSRop.ecall)){
        mstatus.r := mstatus.f.write_f(mstatus.r, Seq(
            (mstatus.d.MIE, 0.U),//clear MIE
            (mstatus.d.MPIE, mstatus.f.read(mstatus.r, mstatus.d.MIE)),//set MPIE to old MIE
            (mstatus.d.MPP, privilege) //set MPP to current privilege
        ))
    }.elsewhen(valid & (io.CSR.info.op === CSRop.mret)){
        mstatus.r := mstatus.f.write_f(mstatus.r, Seq(
            (mstatus.d.MIE, mstatus.f.read(mstatus.r, mstatus.d.MPIE)),//set MIE to old MPIE
            (mstatus.d.MPIE, 1.U),//set MPIE
            (mstatus.d.MPP, CSRPRIV.M.U(2.W)) //set MPP to M-mode
        ))
        privilege := mstatus.f.read(mstatus.r, mstatus.d.MPP) //set privilege to old MPP
    }
    //mtvec
    when(valid & (io.CSR.info.addr === CSRCode.mtvec)){
        mtvec.r := io.CSR.info.wdata
    }
    //mepc
    when(valid & (io.CSR.info.addr === CSRCode.mepc)){
        mepc.r := io.CSR.info.wdata
    }.elsewhen(valid & (io.CSR.info.op === CSRop.ecall)){
        mepc.r := io.CSR.PC
    }
    //mcause
    when(valid & (io.CSR.info.addr === CSRCode.mcause)){
        mcause.r := io.CSR.info.wdata
    }.elsewhen(valid & (io.CSR.info.op === CSRop.ecall)){
        mcause.r := mcause_val
    }
    //mcycle   really writeable? 
    when(valid & (io.CSR.info.addr === CSRCode.mcycle)){
        mcycle.r := mcycle.f.write_f(mcycle.r, Seq(
            (mcycle.d.mcycle, io.CSR.info.wdata)
        ))
    }.elsewhen(valid & (io.CSR.info.addr === CSRCode.mcycleh)){
        mcycle.r := mcycle.f.write_f(mcycle.r, Seq(
            (mcycle.d.mcycleh, io.CSR.info.wdata)
        ))
    }.otherwise(
        mcycle.r := mcycle.r + 1.U
    )

    //interupt and exception handling
    mcause_val := 0.U
    when(io.CSR.info.op === CSRop.ecall){
        mcause_val :=   Mux(privilege === CSRPRIV.U.U, CSRMcauseCode.ecall_from_u, 
                        Mux(privilege === CSRPRIV.S.U, CSRMcauseCode.ecall_from_s, 
                                                       CSRMcauseCode.ecall_from_m))
    }

    io.csr_flush.flush := valid & (io.CSR.info.op(CSRop.int_bit))  
    io.csr_flush.target := Mux(io.CSR.info.op === CSRop.ecall, mtvec.r, mepc.r)

    //special instructions handling
    val u_specialio                 = Module(new SpecialIO)
    u_specialio.io.halt             := io.CSR.valid & io.CSR.info.op(CSRop.special_bit) 
    u_specialio.io.error            := io.CSR.info.op(0)

}

