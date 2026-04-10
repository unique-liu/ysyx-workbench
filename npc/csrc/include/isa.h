#ifndef ISA_H
#define ISA_H

#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>
#include <assert.h>


typedef struct {
  int gpr[32];
  int pc;
  int inst;
  char logbuf[128];
}CPU_state_t;

extern const char *regs[];
extern CPU_state_t cpu;

static inline int check_reg_idx(int idx) {
  assert(idx >= 0 && idx < 32);
  return idx;
}

#define gpr(idx) (cpu.gpr[check_reg_idx(idx)])

static inline const char* reg_name(int idx) {
  extern const char* regs[];
  return regs[check_reg_idx(idx)];
}

void isa_reg_display();
int isa_reg_str2val(const char *s, bool *success);
bool isa_difftest_checkregs(CPU_state_t *ref_r, int pc);
#endif // ISA_H