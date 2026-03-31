#include <stdio.h>
#include <stdint.h>
#include "debug.h"
#define MEM_SIZE_BYTES (64 * 1024 * 1024)

extern uint8_t mem[MEM_SIZE_BYTES];
extern int halt;
extern int error;
extern "C" void halt_system(char is_error) {
  if (is_error) {
    printf("Halt with error\n");
    error = 1;
  } else {
    printf("Halt without error\n");
  }
  halt = 1;
}
extern "C" int mem_read(int raddr) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  int paddr = raddr & ~0x3u;
  int return_data = 0;
  if (paddr + 3< MEM_SIZE_BYTES && paddr >= 0) {
    return_data = mem[paddr] | (mem[paddr + 1] << 8) | (mem[paddr + 2] << 16) | (mem[paddr + 3] << 24);
    DEBUG_PRINT(mem_read, "Reading from address 0x%08x get 0x%08x\n", raddr, return_data);
    return return_data;
  } else {
    printf("read out of bounds at address 0x%08x\n", raddr);
    halt_system(1);
    return 0;
  }
}
extern "C" void mem_write(int waddr, int wdata, char wmask) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  int paddr = waddr & ~0x3u;
  if (paddr < 0 || paddr + 3>= MEM_SIZE_BYTES) {
    printf("write out of bounds at address 0x%08x\n", waddr);
    halt_system(1);
    return;
  }

  if (wmask & 1) {
    mem[paddr] = wdata & 0xFF;
  }
  if ((wmask >> 1) &1 ) {
    mem[paddr + 1] = (wdata >> 8) & 0xFF;
  }
  if ((wmask >> 2) & 1) {
    mem[paddr + 2] = (wdata >> 16) & 0xFF;
  }
  if ((wmask >> 3) & 1) {
    mem[paddr + 3] = (wdata >> 24) & 0xFF;
  }
}
