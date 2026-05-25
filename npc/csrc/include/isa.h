#ifndef ISA_H
#define ISA_H

#include <common.h>

#define INIT_PC 0x20000000

typedef struct {
  int gpr[32];
  int pc;
  int inst;
  char logbuf[128];
}CPU_state_t;

extern const char *regs[];
extern CPU_state_t cpu;
extern CPU_state_t diff_cpu;

static inline int check_reg_idx(int idx) {
  assert(idx >= 0 && idx < 32);
  return idx;
}

#define gpr(idx) (cpu.gpr[check_reg_idx(idx)])
#define diff_gpr(idx) (diff_cpu.gpr[check_reg_idx(idx)])

static inline const char* reg_name(int idx) {
  extern const char* regs[];
  return regs[check_reg_idx(idx)];
}

void isa_reg_display(CPU_state_t *r);
int isa_reg_str2val(const char *s, bool *success);
bool isa_difftest_checkregs(CPU_state_t *ref_r, int pc);
void isa_reg_copy(CPU_state_t *dest, CPU_state_t *src);
#endif // ISA_H