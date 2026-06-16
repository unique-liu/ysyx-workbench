#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include "../riscv.h"
#define SERIAL_ADDR 0x10000000

extern char _heap_start;
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

void putch(char ch) {
  outb(SERIAL_ADDR, (uint8_t)ch);
}

void print_myid(){
  // 打印 mvendorid 和 marchid
  uint32_t mvendorid, marchid;
  asm volatile("csrr %0, mvendorid" : "=r"(mvendorid));
  asm volatile("csrr %0, marchid" : "=r"(marchid));
  printf("mvendorid: 0x%08x\n", mvendorid);
  printf("marchid: %d\n", marchid);
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));
  __builtin_unreachable();
}

void _trm_init() {
  print_myid();
  int ret = main(mainargs);
  halt(ret);
}
