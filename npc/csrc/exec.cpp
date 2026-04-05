#include <exec.h>

rstate_t npc_state;
VerilatedContext* contextp;
VCPUtop* top;
VerilatedFstC* tfp;

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

static void trace_and_difftest() {
    #ifdef CONFIG_ITRACE
    if (cpu.logbuf[0] != '\0') {
        itrace_record(cpu.logbuf);
    }
    #endif

//   if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(_this->logbuf)); }
//   IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));

    #ifdef CONFIG_WATCHPOINT
    int trigger_wp = check_wp();
    if (trigger_wp!=0) {
        npc_state.type = NPC_STOP;
        printf("Hit %d watchpoints.\n", trigger_wp);
    }
    #endif
}

void execute(int n){
    // reset(10);
    while (npc_state.type == NPC_RUNNING) {//running loop
        exceute_once();
        trace_and_difftest();
        n--;
        if (n == 0) {
            npc_state.type = NPC_WAITING;
            break;
        }
        if (npc_state.time >= MAX_TIME) {
            npc_state.type = NPC_TIMEOUT;
            break;
        }
    }

}