#ifndef DIFFTEST_H
#define DIFFTEST_H

#include <common.h>
enum { DIFFTEST_TO_DUT, DIFFTEST_TO_REF };

void difftest_skip_ref();
void difftest_skip_dut(int nr_ref, int nr_dut);
void init_difftest(char *ref_so_file, long img_size, int port);
void difftest_step(int pc, int npc);
void chech_mem(int addr, size_t n);
#endif // DIFFTEST_H