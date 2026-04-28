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
    // DPI-C 导入声明与之前相同
    import "DPI-C" function int mem_read(input int addr,input int pc);
    import "DPI-C" function void mem_write(input int addr, input int data, input byte mask,input int pc);

    // reg [31:0] rdata_reg;
    // assign io_rdata = rdata_reg;

    // always @(posedge io_clock) begin
    //     if (io_ren) begin
    //         rdata_reg <= mem_read(io_raddr, io_PC);
    //     end else begin
    //         rdata_reg <= 32'h0;
    //     end
    // end

    // --------------- 【关键改动】----------------
    // 第1步：时钟沿 采样 地址和使能
    reg [31:0] raddr_reg;
    reg        ren_reg;
    reg [31:0] pc_reg;

    always @(posedge io_clock) begin
        raddr_reg <= io_raddr;
        ren_reg   <= io_ren;
        pc_reg    <= io_PC;
    end

    // 第2步：组合逻辑读取（用采样后的地址）
    reg [31:0] rdata_reg;
    always @(*) begin
        if (ren_reg)
            rdata_reg = mem_read(raddr_reg, pc_reg);
        else
            rdata_reg = 32'h0;
    end

    assign io_rdata = rdata_reg;
    // -------------------------------------------



    always @(posedge io_clock) begin
        if (io_wen) mem_write(io_waddr, io_wdata, {4'b0,io_wmask}, io_PC);
    end
endmodule
