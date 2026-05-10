import chisel3._
import chisel3.util._
import chisel3.util.random.{ GaloisLFSR, XOR }

class LFSR (width: Int) extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(width.W))
  })

  // Create a Galois LFSR with a specific polynomial
  val lfsr = GaloisLFSR(32, Set(32, 22, 2, 1)) // Example taps for a 32-bit LFSR

  // Connect the output to the LFSR value
  io.out := lfsr(width - 1, 0) // Take the lower 'width' bits of the LFSR
}