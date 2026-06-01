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



void init_uart(){
  // 初始化串口，设置波特率等
  // outb(SERIAL_ADDR + 1, 0x00); // 禁止中断
  outb(SERIAL_ADDR + 3, 0x80); // 设置波特率分频器访问
  outb(SERIAL_ADDR + 0, 0x01); // 波特率分频器低字节 (115200 baud)
  outb(SERIAL_ADDR + 1, 0x00); // 波特率分频器高字节
  outb(SERIAL_ADDR + 3, 0x03); // 设置数据位为8，停止位为1，无奇偶校验，并且禁用波特率分频器访问
}

void putch(char ch) {
  while((inb(SERIAL_ADDR+5)&0x20) == 0); // 等待发送缓冲区空
  outb(SERIAL_ADDR, (uint8_t)ch);
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

void _trm_init() {
  //load data and set bss
  memcpy(&_data_sa_start, &_data_ma_start, &_data_ma_end - &_data_ma_start);
  memset(&_bss_sa_start, 0, &_bss_sa_end - &_bss_sa_start);
  //init uart
  init_uart();
  //print mvendorid and marchid 
  // print_myid();
  // print_myid();

  
  int ret = main(mainargs);
  halt(ret);
}
