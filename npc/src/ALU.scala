import chisel3._
import chisel3.util._

class ALU (data_width: Int)extends Module{
    val io = IO(new Bundle{
        val alu_src1        = Input (UInt(data_width.W))
        val alu_src2        = Input (UInt(data_width.W))
        val alu_op          = Input (UInt(ALUop.op_width.W))
        val alu_out         = Output(UInt(data_width.W))
    })
    io.alu_out := 0.U
    switch(io.alu_op){
        is(ALUop.add){io.alu_out := io.alu_src1 + io.alu_src2}
        is(ALUop.sub){io.alu_out := io.alu_src1 - io.alu_src2}
        is(ALUop.and){io.alu_out := io.alu_src1 & io.alu_src2}
        is(ALUop.or){io.alu_out := io.alu_src1 | io.alu_src2}
        is(ALUop.xor){io.alu_out := io.alu_src1 ^ io.alu_src2}
        is(ALUop.nor){io.alu_out := ~(io.alu_src1 | io.alu_src2)}
        is(ALUop.slt){io.alu_out := Mux(io.alu_src1 < io.alu_src2, 1.U, 0.U)}
    }
}