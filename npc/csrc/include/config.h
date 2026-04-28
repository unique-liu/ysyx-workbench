#ifndef CONFIG_H
#define CONFIG_H

#define DEBUG // 开启调试模式，启用日志记录等功能
#define CONFIG_DEVICE // 开启设备模拟功能
#define CONFIG_ITRACE // 开启指令跟踪功能
#define CONFIG_MTRACE // 开启内存访问日志记录功能
#define CONFIG_FTRACE // 开启函数调用跟踪功能 
#define CONFIG_DTRACE // 开启设备模拟功能
#define CONFIG_RTRACE // 开启寄存器访问日志记录功能
// #define CONFIG_WATCHPOINT // 开启监视点功能
// #define CONFIG_DIFFTEST// 开启差分测试功能
#define CONFIG_AUTOTRACE // 开启自动跟踪功能
#define CONFIG_AUTOTRACE_PERIOD 10000 // 自动跟踪的周期，单位为指令数

#define CONFIG_MSIZE 0x8000004
#define CONFIG_MBASE 0x80000000
#define CONFIG_PC_RESET_OFFSET 0 

#define CONFIG_FST //开启波形文件生成


#endif // CONFIG_H