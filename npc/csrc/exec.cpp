#include <exec.h>
#include <difftest.h>
#include <sdb.h>
#include <isa.h>
#include <trace.h>
#include <shoot.h>
#include <sys/time.h>
#include <board.h>
#include <init.h>

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
    if (npc_state.difftest_on == DIFF_ON) {
        if (first_submit) {
            first_submit = 0;
        }else{
            difftest_step(diff_cpu.pc, 0);
        }
    }
    #endif

    #ifdef CONFIG_WATCHPOINT
    int trigger_wp = check_wp();
    if (trigger_wp!=0 && npc_state.type == NPC_RUNNING) {
        npc_state.type = NPC_WAITING;
        printf("Hit %d watchpoints.\n", trigger_wp);
    }
    #endif

    #ifdef CONFIG_AUTOTRACE
    if (npc_state.trace_on == TRACE_AUTO && npc_state.inst_count % CONFIG_AUTOTRACE_PERIOD == 0 && npc_state.inst_count != 0) {
        shoot();
    }
    #endif
    npc_state.inst_submit = 0;
}

void execute(int n){
    struct timeval start, end;
    int not_inst_count = 0;
    gettimeofday(&start, NULL);
    while (npc_state.type == NPC_RUNNING) {//running loop
        exceute_once();
        update_board();

        if (npc_state.trace_on == TRACE_CLOCK && npc_state.time >= npc_state.trace_clock) {
            npc_state.trace_on = TRACE_ON;
            init_fst();
            printf("time %lld reached trace clock %lld, turn on trace\n", npc_state.time, npc_state.trace_clock);
        }

        if (npc_state.inst_submit == 1) {
            trace_and_difftest();
            not_inst_count = 0;
        }
        
        n--;
        not_inst_count++;
        if (n == 0) {
            npc_state.type = NPC_WAITING;
            break;
        }
        if (npc_state.time >= npc_state.time_limit) {
            printf("\033[31mTime out\033[0m\n");
            npc_state.type = NPC_TIMEOUT;
            break;
        }
        if (not_inst_count >= MAX_TIME_NOINST) {
            printf("\033[31mNo instruction executed in the last %d cycles\033[0m\n", MAX_TIME_NOINST/2);
            npc_state.type = NPC_TIMEOUT;
            break;
        }
    }
    gettimeofday(&end, NULL);
    npc_state.real_time += (end.tv_sec - start.tv_sec) * 1000000 + (end.tv_usec - start.tv_usec);

}