#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include "../riscv.h"
#define SERIAL_ADDR 0x10000000L

extern char _heap_start;
extern char _data_ma_start,_data_ma_end,_data_sa_start,_data_sa_end;
extern char _bss_sa_start,_bss_sa_end;

int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE 0x00002000//sram
#define PMEM_END  ((uintptr_t)&_heap_start + PMEM_SIZE)

Area heap = RANGE(&_bss_sa_end, PMEM_END);// 堆区从 bss 段结束开始，到物理内存末尾
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

void putch(char ch) {
  outb(SERIAL_ADDR, (uint8_t)ch);
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));
  __builtin_unreachable();
}

void _trm_init() {
  memcpy(&_data_sa_start, &_data_ma_start, &_data_ma_end - &_data_ma_start);
  memset(&_bss_sa_start, 0, &_bss_sa_end - &_bss_sa_start);
  int ret = main(mainargs);
  halt(ret);
}
