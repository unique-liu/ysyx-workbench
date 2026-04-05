#ifndef ISA_H
#define ISA_H

#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include <string.h>


typedef struct {
  int gpr[32];
  int pc;
  int inst;
  char logbuf[128];
}CPU_state_t;

extern const char *regs[];
extern CPU_state_t cpu;

void isa_reg_display();
int isa_reg_str2val(const char *s, bool *success);
#endif // ISA_H