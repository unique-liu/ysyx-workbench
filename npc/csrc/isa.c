#include <isa.h>
#include <exec.h>
#include <mem.h>
#include <difftest.h>
#include <stdio.h>
#include <string.h>

CPU_state_t cpu;
CPU_state_t diff_cpu;

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

void isa_reg_display(CPU_state_t *r) {
  printf("----- register info -----\n");
  printf("pc:\t0x%08x\n", r->pc);
  for(int i = 0; i < 32; i++){
    printf("%s:0x%08x\t", regs[i], r->gpr[i]);
    if(i % 4 == 3) printf("\n");
  }
  printf("----- register info end -----\n");
}

int isa_reg_str2val(const char *s, bool *success) {
  if (strcmp(s, "0") == 0) {
    *success = true;
    return 0;
  }
  for(int i = 1; i < 32; i++){
    if(strcmp(s, regs[i]) == 0){
      *success = true;
      return cpu.gpr[i];
    }
  }
  if(strcmp(s, "pc") == 0){
    *success = true;
    return cpu.pc;
  }
  if(strcmp(s, "ic") == 0){
    *success = true;
    return npc_state.inst_count;
  }
  if(strcmp(s, "tc") == 0){
    *success = true;
    return npc_state.time;
  }
  *success = false;
  return 0;
}

static inline bool difftest_check_reg(const char *name, vaddr_t pc, word_t ref, word_t dut) {
  if (ref != dut) {
    DEBUG_PRINT(diff,T,"%s is different after executing instruction at pc = " FMT_WORD
        ", right = " FMT_WORD ", wrong = " FMT_WORD ", diff = " FMT_WORD"\n",
        name, pc, ref, dut, ref ^ dut);
    return false;
  }
  return true;
}

bool isa_difftest_checkregs(CPU_state_t *ref_r, vaddr_t pc) {
  if (!difftest_check_reg("pc", pc, ref_r->pc, diff_cpu.pc)) {
    return false;
  }
  for (int i = 0; i < 32; i++) {
    if (!difftest_check_reg(reg_name(i), pc, ref_r->gpr[i], diff_gpr(i)))
      return false;
  }
  return true;
}

void isa_reg_copy(CPU_state_t *dest, CPU_state_t *src) {
  dest->pc = src->pc;
  for (int i = 0; i < 32; i++) {
    dest->gpr[i] = src->gpr[i];
  }
}