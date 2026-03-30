#include <verilated.h>
#include <verilated_vcd_c.h>
#include "VCPUtop.h"   // Verilator 生成的顶层类，名称基于模块名

vluint64_t main_time = 0;   // 仿真时间
double sc_time_stamp() { return main_time; }  // 用于 VCD 时间戳

int main(int argc, char **argv) {
    Verilated::commandArgs(argc, argv);

    // 实例化顶层模块
    VCPUtop *top = new VCPUtop;

    // 开启波形跟踪（可选）
    VerilatedVcdC *tfp = nullptr;
    if (Verilated::traceEverOn()) {
        tfp = new VerilatedVcdC;
        top->trace(tfp, 99);   // 跟踪深度 99
        tfp->open("waveform.vcd");
    }

    // 复位和时钟信号（假设顶层有 clk 和 rst_n 输入）
    // 如果顶层没有这些信号，可根据实际修改
    top->clk = 0;
    top->rst_n = 0;

    // 运行仿真直到一定时间
    while (main_time < 1000) {  // 仿真 1000 个时间单位
        // 时钟翻转
        top->clk = (main_time % 10) < 5 ? 0 : 1;  // 周期 10 时间单位

        // 复位释放（在 20 个时间单位后）
        if (main_time == 20) top->rst_n = 1;

        // 更新组合逻辑
        top->eval();

        // 记录波形
        if (tfp) tfp->dump(main_time);

        main_time++;
    }

    // 清理
    if (tfp) tfp->close();
    delete top;
    return 0;
}