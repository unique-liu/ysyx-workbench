#ifndef MEM_H
#define MEM_H

#include <stdint.h>
#include <exec.h>
#include <debug.h>
#define MEM_SIZE_BYTES (64 * 1024 * 1024)
#define MEM_BASE 0x80000000

typedef int vaddr_t ;

extern uint8_t mem[MEM_SIZE_BYTES];

int paddr_read(int addr, int len);
void paddr_write(int addr, int len, int wdata, char wmask);

#endif