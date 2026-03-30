#include <stdio.h>
#include <stdint.h>
extern int halt;
extern "C" int pmem_read(int raddr) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  return *(int *)(uintptr_t)(raddr & ~0x3u);
}
extern "C" void pmem_write(int waddr, int wdata, char wmask) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  int *paddr = (int *)(uintptr_t)(waddr & ~0x3u);
  int old_data = *paddr;
  int new_data = 0;
  if (wmask & 1) {
    new_data |= wdata & 0xFF;
  } else {
    new_data |= old_data & 0xFF;
  }
  if ((wmask >> 1) &1 ) {
    new_data |= wdata & 0xFF00;
  } else {
    new_data |= old_data & 0xFF00;
  }
  if ((wmask >> 2) & 1) {
    new_data |= wdata & 0xFF0000;
  } else {
    new_data |= old_data & 0xFF0000;
  }
  if ((wmask >> 3) & 1) {
    new_data |= wdata & 0xFF000000;
  } else {
    new_data |= old_data & 0xFF000000;
  }
  *paddr = new_data;
}
extern "C" void halt_system(char is_error) {
  if (is_error) {
    printf("Halt with error\n");
  } else {
    printf("Halt without error\n");
  }
  halt = 1;
}