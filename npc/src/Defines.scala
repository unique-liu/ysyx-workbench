
import chisel3._
import chisel3.util.BitPat
object ALUop{
    val op_width    = 4
    val add         = "b0000".U(op_width.W)
    val sub         = "b0001".U(op_width.W)
    val and         = "b0100".U(op_width.W)
    val or          = "b0101".U(op_width.W)
    val xor         = "b0110".U(op_width.W)
    val slt         = "b1000".U(op_width.W)
    val sltu        = "b1001".U(op_width.W)
    val sll         = "b1100".U(op_width.W)
    val srl         = "b1110".U(op_width.W)
    val sra         = "b1111".U(op_width.W)
    //following the the bitpattern of above
    val b_add       = BitPat(add)
    val b_sub       = BitPat(sub)
    val b_and       = BitPat(and)
    val b_or        = BitPat(or)
    val b_xor       = BitPat(xor)
    val b_slt       = BitPat(slt)
    val b_sltu      = BitPat(sltu)
    val b_sll       = BitPat(sll)
    val b_srl       = BitPat(srl)
    val b_sra       = BitPat(sra)
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
    val op_width    = 5
    val reg_bit     = 0
    val imm_bit     = 1
    val pc_bit      = 2
    val four_bit    = 3
    val csr_bit     = 4
    val use_zero    = "b00000".U(op_width.W)
    val use_reg     = "b00001".U(op_width.W)
    val use_imm     = "b00010".U(op_width.W)
    val use_pc      = "b00100".U(op_width.W)
    val use_four    = "b01000".U(op_width.W)
    val use_csr     = "b10000".U(op_width.W)
    //following the the bitpattern of above
    val b_use_zero  = BitPat(use_zero)
    val b_use_reg   = BitPat(use_reg)
    val b_use_imm   = BitPat(use_imm)
    val b_use_pc    = BitPat(use_pc)
    val b_use_four  = BitPat(use_four)
    val b_use_csr   = BitPat(use_csr)
}
object Specialop{
    val op_width    = 2
    val noop        = "b00".U(op_width.W)
    val halt_error  = "b01".U(op_width.W)
    val halt_normal = "b11".U(op_width.W)
    val b_noop      = BitPat(noop)
    val b_halt_error = BitPat(halt_error)
    val b_halt_normal = BitPat(halt_normal)
}
object CSRop{
    val op_width    = 5
    val imm_bit     = 2
    val int_bit     = 3
    val special_bit = 4
    val noop        = "b00000".U(op_width.W)
    val csrrw       = "b00001".U(op_width.W)
    val csrrs       = "b00010".U(op_width.W)
    val csrrc       = "b00011".U(op_width.W)
    val csrrwi      = "b00101".U(op_width.W)
    val csrrsi      = "b00110".U(op_width.W)
    val csrrci      = "b00111".U(op_width.W)
    val ecall       = "b01000".U(op_width.W)
    val mret        = "b01010".U(op_width.W)
    val ebreak      = "b10000".U(op_width.W)
    val inv_inst    = "b10001".U(op_width.W)
    //following the the bitpattern of above
    val b_noop      = BitPat(noop)
    val b_csrrw     = BitPat(csrrw)
    val b_csrrs     = BitPat(csrrs)
    val b_csrrc     = BitPat(csrrc)
    val b_csrrwi    = BitPat(csrrwi)
    val b_csrrsi    = BitPat(csrrsi)
    val b_csrrci    = BitPat(csrrci)
}
object InstCode{
    //R-type: func7|rs2|rs1|func3|rd|opcode
    val add         = BitPat("b0000000_?????_?????_000_?????_0110011")
    val sub         = BitPat("b0100000_?????_?????_000_?????_0110011")
    val sll         = BitPat("b0000000_?????_?????_001_?????_0110011")
    val slt         = BitPat("b0000000_?????_?????_010_?????_0110011")
    val sltu        = BitPat("b0000000_?????_?????_011_?????_0110011")
    val xor         = BitPat("b0000000_?????_?????_100_?????_0110011")
    val srl         = BitPat("b0000000_?????_?????_101_?????_0110011")
    val sra         = BitPat("b0100000_?????_?????_101_?????_0110011")
    val or          = BitPat("b0000000_?????_?????_110_?????_0110011")
    val and         = BitPat("b0000000_?????_?????_111_?????_0110011")

    //I-type: imm[11:0]|rs1|func3|rd|opcode
    val jalr        = BitPat("b????????????_?????_000_?????_1100111")

    val addi        = BitPat("b????????????_?????_000_?????_0010011")
    val slti        = BitPat("b????????????_?????_010_?????_0010011")
    val sltiu       = BitPat("b????????????_?????_011_?????_0010011")
    val xori        = BitPat("b????????????_?????_100_?????_0010011")
    val ori         = BitPat("b????????????_?????_110_?????_0010011")
    val andi        = BitPat("b????????????_?????_111_?????_0010011")
    val slli        = BitPat("b0000000_?????_?????_001_?????_0010011")
    val srli        = BitPat("b0000000_?????_?????_101_?????_0010011")
    val srai        = BitPat("b0100000_?????_?????_101_?????_0010011")

    val lb          = BitPat("b????????????_?????_000_?????_0000011")
    val lh          = BitPat("b????????????_?????_001_?????_0000011")
    val lw          = BitPat("b????????????_?????_010_?????_0000011")
    val lbu         = BitPat("b????????????_?????_100_?????_0000011")
    val lhu         = BitPat("b????????????_?????_101_?????_0000011")

    val csrrw       = BitPat("b????????????_?????_001_?????_1110011")
    val csrrs       = BitPat("b????????????_?????_010_?????_1110011")
    val csrrc       = BitPat("b????????????_?????_011_?????_1110011")
    val csrrwi      = BitPat("b????????????_?????_101_?????_1110011")
    val csrrsi      = BitPat("b????????????_?????_110_?????_1110011")
    val csrrci      = BitPat("b????????????_?????_111_?????_1110011")
    val ecall       = BitPat("b000000000000_00000_000_00000_1110011")
    val ebreak      = BitPat("b000000000001_00000_000_00000_1110011")
    val mret        = BitPat("b001100000010_00000_000_00000_1110011")

    //S-type: imm[11:5]|rs2|rs1|func3|imm[4:0]|opcode
    val sb          = BitPat("b???????_?????_?????_000_?????_0100011")
    val sh          = BitPat("b???????_?????_?????_001_?????_0100011")
    val sw          = BitPat("b???????_?????_?????_010_?????_0100011")

    //B-type: imm[12|10:5]|rs2|rs1|func3|imm[4:1|11]|opcode
    val beq         = BitPat("b???????_?????_?????_000_?????_1100011")
    val bne         = BitPat("b???????_?????_?????_001_?????_1100011")
    val blt         = BitPat("b???????_?????_?????_100_?????_1100011")
    val bge         = BitPat("b???????_?????_?????_101_?????_1100011")
    val bltu        = BitPat("b???????_?????_?????_110_?????_1100011")
    val bgeu        = BitPat("b???????_?????_?????_111_?????_1100011")

    //U-type: imm[31:12]|rd|opcode
    val lui         = BitPat("b????????????????????_?????_0110111")
    val auipc       = BitPat("b????????????????????_?????_0010111")

    //J-type: imm[20|10:1|11|19:12]|rd|opcode
    val jal         = BitPat("b????????????????????_?????_1101111")
}

object IFUS{
    val state_width = 3
    val s_init_b    = 0
    val s_wait_b    = 1
    val s_ready_b   = 2
    val s_init      = "b001".U(state_width.W)//wait pc to be update, current pc is not valid
    val s_wait      = "b010".U(state_width.W)//pc and inst are mismatch, need to use saved inst
    val s_ready     = "b100".U(state_width.W)//pc and inst are valid, ready to output
}