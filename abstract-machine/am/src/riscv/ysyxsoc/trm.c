#include <am.h>
#include <klib-macros.h>
#include <klib.h>
#include "../riscv.h"
// #define __RTTHREAD__
extern char _heap_start;
extern char _text_ma_start,_text_ma_end,_text_sa_start,_text_sa_end;
extern char _bootloader_ma_start,_bootloader_ma_end,_bootloader_sa_start,_bootloader_sa_end;
extern char _data_ma_start,_data_ma_end,_data_sa_start,_data_sa_end;
extern char _bss_sa_start,_bss_sa_end;
#ifdef __RTTHREAD__
extern char _edata_ma_start,_edata_ma_end,_edata_sa_start,_edata_sa_end;
extern char _ebss_sa_start,_ebss_sa_end;
#endif
void init_uart();
void out_ch(char ch);

int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE 0x2000000//sdram size
#define PMEM_END  ((uintptr_t)&_heap_start + PMEM_SIZE)

Area heap = RANGE(&_bss_sa_end, PMEM_END);// 堆区从 bss 段结束开始，到物理内存末尾
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

#define putch_inline(ch) do { \
  while((inb(0x10000000+5)&0x20) == 0); /* 等待发送缓冲区空 */ \
  outb(0x10000000, (uint8_t)(ch)); \
} while(0)

void putch(char ch) {
  out_ch(ch);
}

void halt(int code) {
  asm volatile("mv a0, %0; ebreak" : :"r"(code));
  __builtin_unreachable();
}

void print_myid(){
  // 打印 mvendorid 和 marchid
  uint32_t mvendorid, marchid;
  asm volatile("csrr %0, mvendorid" : "=r"(mvendorid));
  asm volatile("csrr %0, marchid" : "=r"(marchid));
  printf("mvendorid: 0x%08x\n", mvendorid);
  printf("marchid: 0x%08x\n", marchid);
}

void print_loadinfo(){
  printf("text:0x%08x-0x%08x -> 0x%08x-0x%08x\n", &_text_ma_start, &_text_ma_end, &_text_sa_start, &_text_sa_end);
  printf("bootloader:0x%08x-0x%08x -> 0x%08x-0x%08x\n", &_bootloader_ma_start, &_bootloader_ma_end, &_bootloader_sa_start, &_bootloader_sa_end);
  printf("data:0x%08x-0x%08x -> 0x%08x-0x%08x\n", &_data_ma_start, &_data_ma_end, &_data_sa_start, &_data_sa_end);
  printf("bss: void -> 0x%08x-0x%08x\n", &_bss_sa_start, &_bss_sa_end);
  #ifdef __RTTHREAD__
  printf("edata:0x%08x-0x%08x -> 0x%08x-0x%08x\n", &_edata_ma_start, &_edata_ma_end, &_edata_sa_start, &_edata_sa_end);
  printf("ebss: void -> 0x%08x-0x%08x\n", &_ebss_sa_start, &_ebss_sa_end);
  #endif
}

__attribute__((section(".entry")))
void first_loader() {
  init_uart();
  putch_inline('1');
  char *src = &_bootloader_ma_start;
  char *dst = &_bootloader_sa_start;
  while (dst < &_bootloader_sa_end) {
    *dst++ = *src++;
  }
  putch_inline('1');
}

__attribute__((section(".bootloader")))
void second_loader() {
  putch_inline('2');
  char *src = &_text_ma_start;
  char *dst = &_text_sa_start;
  putch_inline('t');
  while (dst < &_text_sa_end) {
    *dst++ = *src++;
  }
  src = &_data_ma_start;
  dst = &_data_sa_start;
  putch_inline('d');
  while (dst < &_data_sa_end) {
    *dst++ = *src++;
  }
  // char *bss = &_bss_sa_start;
  // putch_inline('b');
  // while (bss < &_bss_sa_end) {
  //   *bss++ = 0;
  // }

  #ifdef __RTTHREAD__
  src = &_edata_ma_start;
  dst = &_edata_sa_start;
  putch('D');
  while (dst < &_edata_sa_end) {
    *dst++ = *src++;
  }
  // bss = &_ebss_sa_start;
  // putch('B');
  // while (bss < &_ebss_sa_end) {
  //   *bss++ = 0;
  // }
  #endif
  putch_inline('2');
  asm volatile ("j _trm_init");
}

// __attribute__((section(".bootloader")))
void _trm_init() {
  putch('\n');
  //print mvendorid and marchid 
  print_myid();
  // print load info
  // print_loadinfo();
  
  int ret = main(mainargs);
  halt(ret);
}
