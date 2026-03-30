module MemIO (
    input         io_ren,
    input  [31:0] io_raddr,
    output [31:0] io_rdata,
    input         io_wen,
    input  [31:0] io_waddr,
    input  [31:0] io_wdata,
    input  [3:0]  io_wmask
);
    // DPI-C 导入声明与之前相同
    import "DPI-C" function void mem_read(input int addr, output int data);
    import "DPI-C" function void mem_write(input int addr, input int data, input byte mask);

    reg [31:0] rdata_reg;
    assign io_rdata = rdata_reg;

    always @(*) begin
        if (io_ren) mem_read(io_raddr, rdata_reg);
        else rdata_reg = 32'h0;
    end

    always @(*) begin
        if (io_wen) mem_write(io_waddr, io_wdata, {4'b0,io_wmask});
    end
endmodule
