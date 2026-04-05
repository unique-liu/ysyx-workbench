#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
// Include model header, generated from Verilating "top.v"
#include <string>
#include <cstring>

#include <elf.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>

#include <init.h>
#include <mem.h>
#include <exec.h>



int main(int argc, char** argv) {
    
    

    // Construct a VerilatedContext to hold simulation time, etc.
    // VerilatedContext* const contextp = new VerilatedContext;
    // VerilatedFstC* tfp = new VerilatedFstC;
    // Pass arguments so Verilated code can see them, e.g. $value$plusargs
    // This needs to be called before you create any model
    // contextp->commandArgs(argc, argv);
    
    
    // Construct the Verilated model, from Vtop.h generated from Verilating "top.v"
    // VCPUtop* const top = new VCPUtop{contextp};


    // Verilated::traceEverOn(true);
    // top->trace(tfp, 99);
    // tfp->open("dump.fst");

    if (init_all(argc, argv) != 0) {
        fprintf(stderr, "Initialization failed.\n");
        return -1;
    }
    // for (int i = 0; i < 10; i++) {
        
    //     top->reset = 1; top->clock = 0; top->eval();tfp->dump(time);time++;
    //     top->clock = 1; top->eval();tfp->dump(time);time++;
    // }
    // top->reset = 0;
    // while (!halt && (time < MAX_TIME)) {
    //     top->clock = 0; top->eval();tfp->dump(time);time++;
    //     top->clock = 1; top->eval();tfp->dump(time);time++;
        
    // }
    

    return finish_all();

    // Final model cleanup
    // tfp->close();  
    // top->final();

    // // Destroy model
    // delete top;

    // // Return good completion status
    // if (error) {
    //     fprintf(stderr, "Simulation finished with errors.\n");
    //     return 1;
    // }else if(time >= MAX_TIME){
    //     fprintf(stderr, "Simulation finished with timeout.\n");
    //     return 1;
    // }else{
    //     fprintf(stdout, "Simulation finished successfully.\n");
    //     return 0;
    // }
}
