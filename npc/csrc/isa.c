#include <isa.h>
#include <exec.h>

CPU_state_t cpu;

const char *regs[] = {
  "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
  "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
  "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
  "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
};

void isa_reg_display() {
  printf("----- register info -----\n");
  printf("pc:\t0x%08x\n", cpu.pc);
  for(int i = 0; i < 32; i++){
    printf("%s:0x%08x\t", regs[i], cpu.gpr[i]);
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