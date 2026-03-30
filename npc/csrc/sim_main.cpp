#include <verilated.h>
#include "verilated_fst_c.h"
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
// Include model header, generated from Verilating "top.v"
#include "Vtop.h"
int halt = 0;
int main(int argc, char** argv) {
    // See a similar example walkthrough in the verilator manpage.

    // This is intended to be a minimal example.  Before copying this to start a
    // real project, it is better to start with a more complete example,
    // e.g. examples/c_tracing.

    // Construct a VerilatedContext to hold simulation time, etc.
    VerilatedContext* const contextp = new VerilatedContext;
    VerilatedFstC* tfp = new VerilatedFstC;
    // Pass arguments so Verilated code can see them, e.g. $value$plusargs
    // This needs to be called before you create any model
    contextp->commandArgs(argc, argv);
    
    
    // Construct the Verilated model, from Vtop.h generated from Verilating "top.v"
    Vtop* const top = new Vtop{contextp};
    int time = 0;


    Verilated::traceEverOn(true);
    top->trace(tfp, 99);
    tfp->open("dump.fst");
    // Simulate until $finish
    while (!halt) {
        time++;
        top->clk = 0; top->eval();
        top->clk = 1; top->eval();
        tfp->dump(time);
    }

    // Final model cleanup
    tfp->close();  
    top->final();

    // Destroy model
    delete top;

    // Return good completion status
    return 0;
}
