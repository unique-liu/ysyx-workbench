#include <exec.h>

rstate_t npc_state;
VerilatedContext* contextp;
VCPUtop* top;
VerilatedFstC* tfp;
int halt;
int error;

void exceute_once(){
    top->clock = 0; top->eval();tfp->dump((vluint64_t)npc_state.time);npc_state.time++;
    top->clock = 1; top->eval();tfp->dump((vluint64_t)npc_state.time);npc_state.time++;
}

void reset(int n){
    top->reset = 1; 
    for (int i = 0; i < n; i++) {
        exceute_once();
    }
    top->reset = 0;
}

void exctuter(){
    reset(10);
    while (npc_state.type == NPC_RUNNING) {
        exceute_once();
        if (npc_state.time >= MAX_TIME) {
            npc_state.type = NPC_TIMEOUT;
        }
        if (error) {
            npc_state.type = NPC_ERROR;
        }
        if(halt){
            npc_state.type = NPC_HALT;
        }
    }
}