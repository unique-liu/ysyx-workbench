#ifndef MEM_H
#define MEM_H

#include <stdint.h>
#define MEM_SIZE_BYTES (64 * 1024 * 1024)
#define MEM_BASE 0x80000000


extern uint8_t mem[MEM_SIZE_BYTES];


#endif