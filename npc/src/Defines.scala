
import chisel3._
import chisel3.util.BitPat
object ALUop{
    val op_width    = 4
    val add         = 0.U(op_width.W)
    val sub         = 1.U(op_width.W)
    val and         = 2.U(op_width.W)
    val or          = 3.U(op_width.W)
    val xor         = 4.U(op_width.W)
    val nor         = 5.U(op_width.W)
    val slt         = 6.U(op_width.W)
    //following the the bitpattern of above
    val b_add       = BitPat(add)
    val b_sub       = BitPat(sub)
    val b_and       = BitPat(and)
    val b_or        = BitPat(or)
    val b_xor       = BitPat(xor)
    val b_nor       = BitPat(nor)   
    val b_slt       = BitPat(slt)
}
object InstType{
    val type_width  = 3
    val Invalid     = 0.U(type_width.W)
    val R           = 1.U(type_width.W)
    val I           = 2.U(type_width.W)
    val S           = 3.U(type_width.W)
    val B           = 4.U(type_width.W)
    val U           = 5.U(type_width.W)
    val J           = 6.U(type_width.W)
    //following the the bitpattern of above
    val b_Invalid   = BitPat(Invalid)
    val b_R         = BitPat(R)
    val b_I         = BitPat(I)
    val b_S         = BitPat(S)
    val b_B         = BitPat(B)
    val b_U         = BitPat(U)
    val b_J         = BitPat(J)
}
object Memop{
    val op_width    = 4
    val byte_bit    = 0
    val half_bit    = 1
    val unsign_bit  = 2
    val load_bit    = 3

    val noop        = "b0000".U(op_width.W)
    val s_byte      = "b0001".U(op_width.W)
    val s_half      = "b0010".U(op_width.W)
    val s_word      = "b0011".U(op_width.W)
    val l_byte_s    = "b1001".U(op_width.W)
    val l_half_s    = "b1010".U(op_width.W)
    val l_byte_u    = "b1101".U(op_width.W)
    val l_half_u    = "b1110".U(op_width.W)
    val l_word      = "b1011".U(op_width.W)
    //following the the bitpattern of above
    val b_noop      = BitPat(noop)
    val b_s_byte    = BitPat(s_byte)
    val b_s_half    = BitPat(s_half)
    val b_s_word    = BitPat(s_word)
    val b_l_byte_s  = BitPat(l_byte_s)
    val b_l_half_s  = BitPat(l_half_s)
    val b_l_byte_u  = BitPat(l_byte_u)
    val b_l_half_u  = BitPat(l_half_u)
    val b_l_word    = BitPat(l_word)
}
object Regop{
    val op_width    = 2
    val mem_bit     = 0
    val write_bit   = 1

    val noop        = "b00".U(op_width.W)
    val w_alu       = "b10".U(op_width.W)
    val w_mem       = "b11".U(op_width.W)
    //following the the bitpattern of above
    val b_noop      = BitPat(noop)
    val b_w_alu     = BitPat(w_alu)
    val b_w_mem     = BitPat(w_mem)
}
object Branchop{
    val op_width    = 5
    val branch_bit   = 0
    val reverse_bit  = 1//1: reverse the condition of == > >=, e.g. beq -> bne
    val sign_bit     = 2//0: signed compare, 1: unsigned compare
    val cond_bit     = 3//0: === , 1: >=
    val jump_bit     = 4//1: this is a jump instruction

    val noop        = "b00000".U(op_width.W)
    val beq         = "b00001".U(op_width.W)
    val bne         = "b00011".U(op_width.W)
    val bge         = "b01001".U(op_width.W)
    val blt         = "b01011".U(op_width.W)
    val bgeu        = "b01101".U(op_width.W)
    val bltu        = "b01111".U(op_width.W)
    val jal         = "b10000".U(op_width.W)
    val jalr        = "b11000".U(op_width.W)
    //following the the bitpattern of above
    val b_noop      = BitPat(noop)
    val b_beq       = BitPat(beq)
    val b_bne       = BitPat(bne)
    val b_bge       = BitPat(bge)
    val b_blt       = BitPat(blt)
    val b_bgeu      = BitPat(bgeu)
    val b_bltu      = BitPat(bltu)
    val b_jal       = BitPat(jal)
    val b_jalr      = BitPat(jalr)
}
object Srcop{
    val op_width    = 4
    val reg_bit     = 0
    val imm_bit     = 1
    val pc_bit      = 2
    val four_bit    = 3
    val use_zero    = "b0000".U(op_width.W)
    val use_reg     = "b0001".U(op_width.W)
    val use_imm     = "b0010".U(op_width.W)
    val use_pc      = "b0100".U(op_width.W)
    val use_four    = "b1000".U(op_width.W)
    //following the the bitpattern of above
    val b_use_zero  = BitPat(use_zero)
    val b_use_reg   = BitPat(use_reg)
    val b_use_imm   = BitPat(use_imm)
    val b_use_pc    = BitPat(use_pc)
    val b_use_four  = BitPat(use_four)
}
object InstCode{
    //R-type: func7|rs2|rs1|func3|rd|opcode
    val add         = BitPat("b0000000_?????_?????_000_?????_0110011")
    //I-type: imm[11:0]|rs1|func3|rd|opcode
    val jalr        = BitPat("b????????????_?????_000_?????_1100111")

    val addi        = BitPat("b????????????_?????_000_00001_0010011")

    val lbu         = BitPat("b????????????_?????_100_?????_0000011")
    val lw          = BitPat("b????????????_?????_010_?????_0000011")
    //S-type: imm[11:5]|rs2|rs1|func3|imm[4:0]|opcode
    val sb          = BitPat("b???????_?????_?????_000_?????_0100011")
    val sw          = BitPat("b???????_?????_?????_000_?????_0100011")
    //B-type: imm[12|10:5]|rs2|rs1|func3|imm[4:1|11]|opcode
    //U-type: imm[31:12]|rd|opcode
    val lui         = BitPat("b????????????????????_?????_0110111")
    //J-type: imm[20|10:1|11|19:12]|rd|opcode
}