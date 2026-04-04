#ifndef EXEC_H
#define EXEC_H

#include "VCPUtop.h"
#include <verilated.h>
#include "verilated_fst_c.h"

#define MAX_TIME 100000

typedef enum{
    NPC_INVALID,
    NPC_RUNNING,
    NPC_HALT,//stop by instruction
    NPC_STOP,//stop by other reasons, e.g. interupt
    NPC_TIMEOUT,//stop by timeout
    NPC_ERROR
} rstate_types_t;
typedef struct{
    rstate_types_t type;
    long long inst_count;
    long long time;
} rstate_t;

extern rstate_t npc_state;
extern VerilatedContext* contextp;
extern VCPUtop* top;
extern VerilatedFstC* tfp;

void exctuter();

#endif // EXEC_H