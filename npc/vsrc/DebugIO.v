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
    import "DPI-C" function  void sync_cpu(input int pc,input int inst,input int submit, input int rd, input int wdata, input int wen,input int target,input int rs1,input int jump);

    always @(posedge io_clock) begin
        if (io_submit) begin
            sync_cpu(io_pc, io_inst, {31'b0,io_submit}, {27'b0,io_rd}, io_wdata, {31'b0,io_wen}, io_target, {27'b0,io_rs1}, {31'b0,io_branch});
        end
    end

endmodule
