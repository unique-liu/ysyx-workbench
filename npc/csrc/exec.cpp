#include <exec.h>
#include <difftest.h>
#include <sdb.h>
#include <isa.h>
#include <trace.h>
#include <shoot.h>

rstate_t npc_state;
VerilatedContext* contextp;
VCPUtop* top;
VerilatedFstC* tfp;
static int first_submit = 1;

void exceute_once(){
    top->clock = 0; top->eval();
    #ifdef CONFIG_FST 
    if (npc_state.trace_on==TRACE_ON) {
        tfp->dump((vluint64_t)npc_state.time);
    }
    #endif 
    npc_state.time++;
    
    top->clock = 1; top->eval();
    #ifdef CONFIG_FST 
    if (npc_state.trace_on==TRACE_ON) {
        tfp->dump((vluint64_t)npc_state.time);
    }
    #endif 
    npc_state.time++;
}

void reset(int n){
    DEBUG_PRINT(reset, T, "reset start\n");
    top->reset = 1; 
    for (int i = 0; i < n; i++) {
        exceute_once();
    }
    top->reset = 0;
    DEBUG_PRINT(reset, T, "reset end\n");
}

static void trace_and_difftest() {
    // #ifdef CONFIG_ITRACE
    // if (cpu.logbuf[0] != '\0') {
    //     itrace_record(cpu.logbuf);
    // }
    // #endif

//   if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(_this->logbuf)); }
//   IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));
    #ifdef CONFIG_DIFFTEST
    if (first_submit && npc_state.inst_submit) {
        first_submit = 0;
    }else if (npc_state.inst_submit == 1) {
        // if (first_submit) {
        //     difftest_skip_ref();
        //     printf("difftest: first instruction submit at pc = 0x%08x\n", cpu.pc);
        //     first_submit = 0;
        // }
        difftest_step(diff_cpu.pc, 0);
        npc_state.inst_submit = 0;
    }
    #endif

    #ifdef CONFIG_WATCHPOINT
    int trigger_wp = check_wp();
    if (trigger_wp!=0) {
        npc_state.type = NPC_WAITING;
        printf("Hit %d watchpoints.\n", trigger_wp);
    }
    #endif

    #ifdef CONFIG_AUTOTRACE
    if (npc_state.trace_on == TRACE_AUTO && npc_state.inst_count % CONFIG_AUTOTRACE_PERIOD == 0 && npc_state.inst_count != 0) {
        shoot();
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
            printf("\033[31mTime out\033[0m\n");
            npc_state.type = NPC_TIMEOUT;
            break;
        }
    }

}