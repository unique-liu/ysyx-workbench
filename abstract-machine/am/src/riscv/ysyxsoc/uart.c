#include <am.h>
#include "../riscv.h"
#define SERIAL_ADDR 0x10000000L
#define RX SERIAL_ADDR + 0//R receiver FIFO output
#define TX SERIAL_ADDR + 0//W Transmitter FIFO input
#define DLB1 SERIAL_ADDR + 0//RW Divisor Latch Low Byte
#define DLB2 SERIAL_ADDR + 1//RW Divisor Latch High Byte
#define IER SERIAL_ADDR + 1//RW Enable/Mask interrupt
#define IIR SERIAL_ADDR + 2//R Get interrupt information
#define FCR SERIAL_ADDR + 2//W Control FIFO
#define LCR SERIAL_ADDR + 3//RW Control connection
#define MCR SERIAL_ADDR + 4//W Control modem
#define LSR SERIAL_ADDR + 5 //R Status information
#define MSR SERIAL_ADDR + 6 //R Modem Status

__attribute__((section(".entry")))
void init_uart(){
  // 初始化串口，设置波特率等
  // outb(IER, 0x00); // 禁止中断
  outb(LCR, 0x80); // 设置波特率分频器访问
  outb(DLB1, 0x01); // 波特率分频器低字节 (115200 baud)
  outb(DLB2, 0x00); // 波特率分频器高字节
  outb(LCR, 0x03); // 设置数据位为8，停止位为1，无奇偶校验，并且禁用波特率分频器访问
}

void out_ch(char ch) {
  while((inb(LSR) & 0x20) == 0); // 等待发送缓冲区空
  outb(TX, (uint8_t)(ch));
}
char in_ch(){
    return inb(LSR) & 0x1 ? inb(RX) : 0xff; // 判断接收缓冲区是否有数据
}