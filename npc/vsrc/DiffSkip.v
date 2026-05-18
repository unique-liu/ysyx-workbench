module DiffSkip (
    input           io_clock,
    input           io_en,
    input  [31:0]   io_PC,
    input  [31:0]   io_addr,
    input  [31:0]   io_idx
);
    import "DPI-C" function void diff_skip_device(input int pc, input int addr, input int idx);
    always @(posedge io_clock) begin
        if (io_en) diff_skip_device(io_PC, io_addr, io_idx);
    end
    
endmodule