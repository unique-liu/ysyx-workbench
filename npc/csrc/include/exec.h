#ifndef EXEC_H
#define EXEC_H

#include "VCPUtop.h"
#include <verilated.h>
#include "verilated_fst_c.h"
#include <sdb.h>
#include <config.h>
#include <isa.h>
#include <trace.h>

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
typedef struct{
    rstate_types_t type;
    int halt_pc;
    int halt_ret;
    long long inst_count;
    long long time;
    int inst_submit;
} rstate_t;

extern rstate_t npc_state;
extern VerilatedContext* contextp;
extern VCPUtop* top;
extern VerilatedFstC* tfp;

void reset(int n);
void execute(int n);

#endif // EXEC_H