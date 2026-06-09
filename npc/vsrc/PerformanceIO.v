module PerformanceIO (
    input         io_clock,
    input         io_valid,
    input  [7:0]  io_perf_type,
    input  [7:0]  io_code
);

    // DPI-C 导入声明与之前相同
    import "DPI-C" function void perf(input byte perf_type,input byte code);
    always @(posedge io_clock) begin
        if (io_valid) perf(io_perf_type, io_code);
    end

endmodule
