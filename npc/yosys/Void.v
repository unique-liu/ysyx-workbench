//this file is used to save void module, which is used to replace DPI-C module for yosys sta. It should be empty and have the same port list as the DPI-C module.

module DebugIO (
    input          io_clock,
    input   [31:0] io_pc,
    input   [31:0] io_inst,
    input          io_submit,
    input   [4:0 ] io_rd,
    input   [31:0] io_wdata,
    input          io_wen,
    input   [31:0] io_target,
    input   [4:0 ] io_rs1,
    input          io_branch
);
endmodule

module DeviceIO (
    input         io_clock,
    input  [31:0] io_idx,
    input  [31:0] io_PC,
    input         io_ren,
    input  [31:0] io_raddr,
    output [31:0] io_rdata,
    input         io_wen,
    input  [31:0] io_waddr,
    input  [31:0] io_wdata,
    input  [3:0 ] io_wmask
);
endmodule

module DiffSkip (
    input           io_clock,
    input           io_en,
    input  [31:0]   io_PC,
    input  [31:0]   io_addr,
    input  [31:0]   io_idx,

    input           io_ret,
    input  [31:0]   io_ridx,
    input  [31:0]   io_rdata
);
endmodule

module MemIO (
    input         io_clock,
    input  [31:0] io_PC,
    input         io_ren,
    input  [31:0] io_raddr,
    output [31:0] io_rdata,
    input         io_wen,
    input  [31:0] io_waddr,
    input  [31:0] io_wdata,
    input  [3:0 ] io_wmask
);
endmodule

module SpecialIO (
    input         io_halt,
    input         io_error
);
endmodule

module PerformanceIO (
    input         io_clock,
    input         io_valid,
    input  [7:0]  io_perf_type,
    input  [7:0]  io_code
);


endmodule
