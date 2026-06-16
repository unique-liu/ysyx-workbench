import chisel3._
import chisel3.util._
import scala.annotation.meta.field
import os.write

class RegMultifield (field_widths: Seq[Int],width:Int){
    def init(initval: Seq[BigInt]): UInt = {
        require(field_widths.sum == width, "Sum must equal width")
        require(field_widths.length == initval.length, "Length must match")
        val initBits = Cat(initval.zip(field_widths).map { case (v, w) => v.U(w.W) }.reverse)
        RegInit(initBits)
    }

    def read(reg: UInt, field_idx: Int): UInt = {
        require(field_idx < field_widths.length, "Field index out of range")
        val offset = field_widths.take(field_idx).sum
        reg(offset + field_widths(field_idx) - 1, offset)
    }

    def write_m(reg: UInt, wdata: UInt, mask: UInt): UInt = {
        require(mask.getWidth == width, "Mask width must match register width")
        (reg & ~mask) | (wdata & mask)
    }
    
    // def write_f(reg: UInt, fid_and_wdata: Seq[(Int, UInt)]): UInt = {
    //     for (i <- fid_and_wdata.indices) {
    //         val (field_idx, wdata) = fid_and_wdata(i)
    //         require(field_idx < field_widths.length, s"Field index ${field_idx} out of range")
    //         require(wdata.getWidth == field_widths(field_idx), s"Data width for field ${field_idx} must be ${field_widths(field_idx)}")
    //     }
    //     val total_wdata = Wire(UInt(width.W))
    //     val mask = Wire(UInt(width.W))
    //     //generate total_wdata and mask
    //     total_wdata := 0.U
    //     mask := 0.U
    //     for (i <- fid_and_wdata.indices) {
    //         val (field_idx, wdata) = fid_and_wdata(i)
    //         val offset = field_widths.take(field_idx).sum
    //         total_wdata := total_wdata | (wdata << offset)
    //         mask := mask | (Fill(wdata.getWidth, 1.U) << offset)
    //     }

    //     this.write_m(reg, total_wdata, mask)
    // }
    def write_f(reg: UInt, fid_and_wdata: Seq[(Int, UInt)]): UInt = {
        for (i <- fid_and_wdata.indices) {
            val (field_idx, wdata) = fid_and_wdata(i)
            require(field_idx < field_widths.length, s"Field index ${field_idx} out of range")
            require(wdata.getWidth == field_widths(field_idx), s"Data width for field ${field_idx} must be ${field_widths(field_idx)}")
        }

        // 关键：用 Scala 变量先计算出最终值，不产生组合环
        var final_wdata = 0.U(width.W)
        var final_mask = 0.U(width.W)

        for ((field_idx, wdata) <- fid_and_wdata) {
            val offset = field_widths.take(field_idx).sum
            final_wdata = final_wdata | (wdata << offset)  // Scala 变量赋值，不是电路信号！
            final_mask = final_mask | (Fill(wdata.getWidth, 1.U) << offset)
        }

        // 最后只给 Wire 赋值 1 次，无环路
        val total_wdata = Wire(UInt(width.W))
        val mask = Wire(UInt(width.W))
        total_wdata := final_wdata
        mask := final_mask

        this.write_m(reg, total_wdata, mask)
    }
}

class mstatusDEF{
    val field_widths    = Seq(1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 1, 1, 1, 1, 1, 1, 8, 1)
    val init_val        = Seq(BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0), BigInt(0))
    val total_width     = 32
    val WPRI0           = 0
    val SIE             = 1
    val WPRI1           = 2
    val MIE             = 3
    val WPRI2           = 4
    val SPIE            = 5
    val UBE             = 6
    val MPIE            = 7
    val SPP             = 8
    val VS              = 9
    val MPP             = 10
    val FS              = 11
    val XS              = 12
    val MPRV            = 13
    val SUM             = 14
    val MXR             = 15
    val TVM             = 16
    val TW              = 17
    val TSR             = 18
    val WPRI3           = 19
    val SD              = 20
}

class mtvecDEF{
    val field_widths    = Seq(2, 30)
    val init_val        = Seq(BigInt(0), BigInt(0))
    val total_width     = 32
    val mode            = 0
    val base            = 1
}

class mepcDEF{
    val field_widths    = Seq(32)
    val init_val        = Seq(BigInt(0))
    val total_width     = 32
    val mepc            = 0
}

class mcauseDEF{
    val field_widths    = Seq(1, 31)
    val init_val        = Seq(BigInt(0), BigInt(0))
    val total_width     = 32
    val interrupt       = 0
    val exception_code   = 1
}

class mcycleDEF{
    val field_widths    = Seq(32,32)
    val init_val        = Seq(BigInt(0), BigInt(0))
    val total_width     = 64
    val mcycle           = 0
    val mcycleh          = 1
}

class mvendoridDEF{
    val field_widths    = Seq(32)
    val init_val        = Seq(BigInt("79737978", 16))
    val total_width     = 32
    val mvendorid       = 0
}

class marchidDEF{
    val field_widths    = Seq(32)
    val init_val        = Seq(BigInt("018D7E68", 16))//报名号100022545  学号ysyx_26050152
    val total_width     = 32
    val marchid         = 0
}

