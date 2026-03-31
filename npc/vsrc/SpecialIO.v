module SpecialIO (
    input         io_halt,
    input         io_error
);

    // DPI-C 导入声明与之前相同
    import "DPI-C" function void halt_system(input byte is_error);
    always @(*) begin
        if (io_halt) halt_system({7'b0,io_error});
    end

endmodule
