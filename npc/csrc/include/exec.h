#ifndef EXEC_H
#define EXEC_H

#include <common.h>
#include INCLUDE_TOP
#include <verilated.h>
#include "verilated_fst_c.h"


#define MAX_TIME 10000000
#define MAX_TIME_NOINST 100000
#define RESET_TIME 50

typedef enum{
    NPC_INVALID,
    NPC_RUNNING,
    NPC_HALT,//stop by instruction
    NPC_STOP,//stop by other reasons, e.g. interupt or user command q
    NPC_TIMEOUT,//stop by timeout
    NPC_WAITING,//waiting for external events, e.g. user commands
    NPC_ERROR
} rstate_types_t;
typedef enum{
    TRACE_ON,
    TRACE_OFF,
    TRACE_AUTO,
    TRACE_CLOCK,
} trace_t;
typedef enum{
    DIFF_OFF,
    DIFF_ON,
} diff_t;
typedef enum{
    REPORT_OFF,
    REPORT_ON,
} report_t;
typedef struct{
    rstate_types_t type;//current state of npc
    int halt_pc;//the pc of the instruction (not accurate) that cause the halt, may be 0 if the halt is not caused by an instruction
    int halt_ret;//0: success, 1: error, only valid when type is NPC_HALT
    long long real_time;// us calculate real time duration of execution
    long long inst_count;//submitted instruction count
    long long time;//clk up and down equals 2 time units
    int inst_submit;
    trace_t trace_on;//whether to print trace log, default 1, can be set by shooting mode
    long long trace_clock;//trace on when npc_state.time >= trace_clock
    diff_t difftest_on;//whether to do difftest, default 0, can be set by shooting mode
    long long time_limit;//the maximum time allowed for execution, default MAX_TIME, can be set by arguments
    report_t report_on;//whether to report performance, default 0, can be set by shooting mode
} rstate_t;

extern rstate_t npc_state;
extern VerilatedContext* contextp;
extern VCPUtop* top;
extern VerilatedFstC* tfp;

void reset(int n);
void execute(int n);

#endif // EXEC_H