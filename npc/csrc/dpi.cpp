#include <stdio.h>
#include <stdint.h>
#include <debug.h>
#include <mem.h>
#include <isa.h>


extern "C" void halt_system(char is_error) {
  if (is_error) {
    printf("\033[31mHalt with error\033[0m\n");
    npc_state.type = NPC_ERROR;
  } else {
    printf("\033[32mHalt correctly\033[0m\n");
    npc_state.type = NPC_HALT;
  }
}
extern "C" int mem_read(int raddr, int pc) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  int paddr = raddr & ~0x3u;
  // int real_addr = paddr - MEM_BASE;
  // int return_data = 0;
  // if (real_addr + 3< MEM_SIZE_BYTES && real_addr >= 0) {
  //   return_data = mem[real_addr] | (mem[real_addr + 1] << 8) | (mem[real_addr + 2] << 16) | (mem[real_addr + 3] << 24);
  //   DEBUG_PRINT(mem_read,T, "Reading from address 0x%08x get 0x%08x\n", raddr, return_data);
  //   return return_data;
  // } else {
  //   printf("read out of bounds at address 0x%08x\n", raddr);
  //   halt_system(1);
  //   return 0;
  // }
  DEBUG_PRINT(cpu, T, "pc: 0x%08x", pc);
  return paddr_read(paddr, 4);

}
extern "C" void mem_write(int waddr, int wdata, char wmask,int pc) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  int paddr = waddr & ~0x3u;
  // int real_addr = paddr - MEM_BASE;
  // if (real_addr < 0 || real_addr + 3>= MEM_SIZE_BYTES) {
  //   printf("write out of bounds at address 0x%08x\n", waddr);
  //   halt_system(1);
  //   return;
  // }

  // if (wmask & 1) {
  //   mem[real_addr] = wdata & 0xFF;
  // }
  // if ((wmask >> 1) &1 ) {
  //   mem[real_addr + 1] = (wdata >> 8) & 0xFF;
  // }
  // if ((wmask >> 2) & 1) {
  //   mem[real_addr + 2] = (wdata >> 16) & 0xFF;
  // }
  // if ((wmask >> 3) & 1) {
  //   mem[real_addr + 3] = (wdata >> 24) & 0xFF;
  // }
  DEBUG_PRINT(cpu, T, "pc: 0x%08x", pc);
  paddr_write(paddr, 4, wdata, wmask);
}

extern "C" void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
extern "C" void sync_cpu(int pc, int inst,int submit, int rd, int wdata, int wen,word_t target,int rs1,int branch) {
  // 同步函数, 同步提交指令到cpu
  if (submit==0) {
    cpu.logbuf[0] = '\0';
    return;
  }

  cpu.pc = pc;
  if (wen && rd != 0) {
    #ifdef CONFIG_RTRACE
    DEBUG_PRINT(rtrace,T, "0x%08x: %s = 0x%08x\n", pc, regs[rd], wdata);
    #endif
    cpu.gpr[rd] = wdata;
  }
  cpu.inst = inst;

  #ifdef CONFIG_FTRACE
  if (branch) {
    ftrace_enter(pc, target,rd ,rs1);
  }
  #endif
  
  int len = sprintf(cpu.logbuf, "0x%08x: 0x%08x ", pc, inst);
  disassemble(cpu.logbuf + len, sizeof(cpu.logbuf) - len, cpu.pc, (uint8_t *)&cpu.inst, 4);

}