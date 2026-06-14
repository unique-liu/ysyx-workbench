import chisel3._

class EXU extends Module{
    val io = IO(new Bundle{
        val before = new Bundle{
            val valid           = Input (Bool())
            val ready           = Output(Bool())
            val PC              = Input (UInt(32.W))
            val alu_src1        = Input (UInt(32.W))
            val alu_src2        = Input (UInt(32.W))
            val alu_op          = Input (UInt(ALUop.op_width.W))
            val reg_op          = Input (UInt(Regop.op_width.W))
            val reg_rd          = Input (UInt(5.W))
            val mem_op          = Input (UInt(Memop.op_width.W))
            val mem_src         = Input (UInt(32.W))
            val debug           = Input (new debug)
            val CSR_info        = Input (new CSR_info)
        }
        val next = new Bundle{
            val valid           = Output(Bool())
            val ready           = Input (Bool())
            val PC              = Output(UInt(32.W))
            val alu_result      = Output(UInt(32.W))
            val reg_op          = Output(UInt(Regop.op_width.W))
            val reg_rd          = Output(UInt(5.W))
            val mem_op          = Output(UInt(Memop.op_width.W))
            val mem_src         = Output(UInt(32.W))
            val debug           = Output(new debug)
            val CSR_info        = Output(new CSR_info)
        }
        val forward = new Bundle{
            val reg_wdata       = Output(UInt(32.W))
            val reg_rd          = Output(UInt(5.W))
            val reg_useable     = Output(Bool())
        }
        val flush           = Input(Bool())
    })
    //fluiding control signals
    val valid                   = RegInit(0.U(1.W))
    val will_out                = Wire(Bool())
    val will_in                 = Wire(Bool())
    when(io.flush){
        valid                   := 0.U(1.W)
    }.elsewhen(will_in){
        valid                   := 1.U(1.W)
    }.elsewhen(will_out){
        valid                   := 0.U(1.W)
    }

    will_out                    := io.next.ready & io.next.valid
    will_in                     := io.before.valid & io.before.ready
    io.before.ready             := !valid | will_out
    io.next.valid               := valid

    //latching signals
    val reg_PC                  = Reg(UInt(32.W))
    val reg_alu_src1            = Reg(UInt(32.W))
    val reg_alu_src2            = Reg(UInt(32.W))
    val reg_alu_op              = Reg(UInt(ALUop.op_width.W))
    val reg_reg_op              = Reg(UInt(Regop.op_width.W))
    val reg_reg_rd              = Reg(UInt(5.W))
    val reg_mem_op              = Reg(UInt(Memop.op_width.W))
    val reg_mem_src             = Reg(UInt(32.W))
    val reg_debug               = Reg(new debug)
    val reg_CSR_info            = Reg(new CSR_info)
    when(will_in){
        reg_PC                  := io.before.PC
        reg_alu_src1            := io.before.alu_src1
        reg_alu_src2            := io.before.alu_src2
        reg_alu_op              := io.before.alu_op  
        reg_reg_op              := io.before.reg_op  
        reg_reg_rd              := io.before.reg_rd
        reg_mem_op              := io.before.mem_op  
        reg_mem_src             := io.before.mem_src
        reg_debug               := io.before.debug
        reg_CSR_info            := io.before.CSR_info
    }

    //calculate
    val alu_out                 = Wire(UInt(32.W))
    val u_alu                   = Module(new ALU(32))
    u_alu.io.alu_src1           := reg_alu_src1
    u_alu.io.alu_src2           := reg_alu_src2
    u_alu.io.alu_op             := reg_alu_op  
    alu_out                     := u_alu.io.alu_out

    //output
    io.next.PC                  := reg_PC
    io.next.alu_result          := alu_out
    io.next.reg_op              := reg_reg_op
    io.next.reg_rd              := reg_reg_rd
    io.next.mem_op              := reg_mem_op
    io.next.mem_src             := reg_mem_src
    io.next.debug               := reg_debug
    io.next.CSR_info            := reg_CSR_info

    //forwarding
    io.forward.reg_wdata        := alu_out
    io.forward.reg_rd           := Mux(reg_reg_op(Regop.write_bit) && (valid === 1.U),reg_reg_rd,0.U(5.W))
    io.forward.reg_useable      := !reg_reg_op(Regop.mem_bit) & valid

    
    if(Config.perf_on){
        //perf-it
        val perf_it                 = Module(new perf(PT.it))
        perf_it.io.valid            := valid
        perf_it.io.code             := reg_debug.it_code
    }
}