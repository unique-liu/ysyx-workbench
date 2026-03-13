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
static TOP_NAME dut;
void nvboard_bind_all_pins(TOP_NAME* top);

int main() {
    int time = 0;

    nvboard_bind_all_pins(&dut);
    nvboard_init();
    // Simulate until $finish

    while (time <= 100000) {
        nvboard_update();
        if(dut.a == 1 && dut.b == 1){
            time++;
        }
        dut.eval();
        
    }



    // Return good completion status
    return 0;
}
