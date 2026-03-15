// DESCRIPTION: Verilator: Verilog example module
//
// This file ONLY is placed under the Creative Commons Public Domain.
// SPDX-FileCopyrightText: 2017 Wilson Snyder
// SPDX-License-Identifier: CC0-1.0
//======================================================================

// Include common routines
// Include model header, generated from Verilating "top.v"
#include "Vtop.h"
#include <nvboard.h>

#include <verilated.h>
#include <verilated_fst_c.h>

static TOP_NAME dut;
void nvboard_bind_all_pins(TOP_NAME* top);
void single_cycle() {
  dut.clk = 0; dut.eval();
  dut.clk = 1; dut.eval();
}

void reset(int n) {
  dut.rst = 1;
  while (n -- > 0) single_cycle();
  dut.rst = 0;
}
int main() {
    // int time = 0,stoptime = 0;

    
    // Verilated::traceEverOn(true);
    nvboard_bind_all_pins(&dut);
    nvboard_init();
    
    // VerilatedFstC* tfp = new VerilatedFstC;
    // dut.trace(tfp, 99);
    // tfp->open("dump.fst");
    reset(10);
    while (1) {
        nvboard_update();
        single_cycle();
        dut.eval();
        // tfp->dump(time);
        
    }

    // Final model cleanup
    // tfp->close();  
    dut.final();

    // Return good completion status
    return 0;
}
