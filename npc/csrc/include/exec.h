#ifndef EXEC_H
#define EXEC_H

#include "VCPUtop.h"
#include <verilated.h>
#include "verilated_fst_c.h"
#include <common.h>

#define MAX_TIME 10000000
#define RESET_TIME 10

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
    TRACE_AUTO
} trace_t;
typedef enum{
    DIFF_OFF,
    DIFF_ON,
} diff_t;
typedef struct{
    rstate_types_t type;//current state of npc
    int halt_pc;//the pc of the instruction (not accurate) that cause the halt, may be 0 if the halt is not caused by an instruction
    int halt_ret;//0: success, 1: error, only valid when type is NPC_HALT
    long long inst_count;//submitted instruction count
    long long time;//clk up and down equals 2 time units
    int inst_submit;
    trace_t trace_on;//whether to print trace log, default 1, can be set by shooting mode
    diff_t difftest_on;//whether to do difftest, default 0, can be set by shooting mode
    long long time_limit;//the maximum time allowed for execution, default MAX_TIME, can be set by arguments
} rstate_t;

extern rstate_t npc_state;
extern VerilatedContext* contextp;
extern VCPUtop* top;
extern VerilatedFstC* tfp;

void reset(int n);
void execute(int n);

#endif // EXEC_H