#include <common.h>
#include <stdio.h>
#include <mem.h>
#include <device.h>
#include <isa.h>
#include <difftest.h>
#include <exec.h>
#include <trace.h>

extern "C" void halt_system(char is_error) {
  if (is_error) {
    npc_state.type = NPC_ERROR;
  } else {
    npc_state.halt_pc = cpu.pc+4;//ebreak never submit, but always have a mv inst before, which is the last inst
    npc_state.type = NPC_HALT;
  }
  bool success;
  npc_state.halt_ret = isa_reg_str2val("a0", &success);
  assert(success);
  Log("Halt with error=%d ret=%d at pc = " FMT_WORD "\n", is_error, npc_state.halt_ret, npc_state.halt_pc);
}
extern "C" int mem_read(int raddr, int pc) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  int paddr = raddr & ~0x3u;
  mem_pc_now = pc;
  int ret = paddr_read( paddr, 4);
  // int ret = memory_read( paddr, 4);
  mem_pc_now = 0;
  return ret;

}
extern "C" void mem_write(int waddr, int wdata, char wmask,int pc) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  int paddr = waddr & ~0x3u;
  mem_pc_now = pc;
  paddr_write( paddr, 4, wdata, wmask);
  // memory_write( paddr, 4, wdata, wmask);
  mem_pc_now = 0;
}

extern "C" int read_a_device(int raddr, int pc,int idx) {
  // 总是读取地址为`raddr & ~0x3u`的4字节返回
  int paddr = raddr & ~0x3u;
  mem_pc_now = pc;
  if (in_device(paddr) != idx) {
    DEBUG_PRINT(dtrace, T, "Address 0x%08x is not an address of device %d(%s)", paddr, idx, device_map[idx].name);
    npc_state.type = NPC_ERROR;
    npc_state.halt_pc = pc;
    return 0;
  }else {
    int ret = mmio_read( paddr, 4);
    mem_pc_now = 0;
    return ret;
  }
}

extern "C" void write_a_device(int waddr, int wdata, char wmask,int pc,int idx) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  int paddr = waddr & ~0x3u;
  mem_pc_now = pc;
  if (in_device(paddr) != idx) {
    DEBUG_PRINT(dtrace, T, "Address 0x%08x is not an address of device %d(%s)", paddr, idx, device_map[idx].name);
    npc_state.type = NPC_ERROR;
    npc_state.halt_pc = pc;
    return;
  }else {
    mmio_write( paddr, 4, wdata, wmask);
    mem_pc_now = 0;
    return;
  }
}

extern "C" void diff_skip_device(int pc,int addr,int idx) {
  // 总是往地址为`waddr & ~0x3u`的4字节按写掩码`wmask`写入`wdata`
  // `wmask`中每比特表示`wdata`中1个字节的掩码,
  // 如`wmask = 0x3`代表只写入最低2个字节, 内存中的其它字节保持不变
  int device_id = in_soc_device(addr);
  if (npc_state.trace_on == TRACE_ON) {
    DEBUG_PRINT(btrace, T, "bus %s at pc:0x%08x addr:0x%08x for device %d(%s)\n",idx==10?"read":"write", pc, addr, device_id, device_id!=-1?soc_device_map[device_id].name:"unknown");
  }
  #ifdef CONFIG_DIFFTEST
  if (npc_state.difftest_on == DIFF_ON && device_id != -1) {
    use_device_pc_in(pc);
  }
  #endif
}

extern "C" void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
extern "C" void sync_cpu(int pc, int inst,int submit, int rd, int wdata, int wen,word_t target,int rs1,int jump) {
  // 同步函数, 同步提交指令到cpu
  // DEBUG_PRINT(test, T, "submit = %d", submit);
  if (submit==0) {
    return;
  }
  npc_state.inst_count++;
  npc_state.inst_submit= 1;

  // 同步寄存器状态到diff_cpu: 需要让pc是当前寄存器状态对应的下一条指令的地址
  #ifdef CONFIG_DIFFTEST
  if (npc_state.difftest_on == DIFF_ON) {
    isa_reg_copy(&diff_cpu, &cpu);
    if (use_device_pc_checkout(diff_cpu.pc)) {// 如果当前pc需要设备访问检查，则跳过检查
      difftest_skip_ref();
    }
    diff_cpu.pc = pc;
  }
  
  #endif

  cpu.pc = pc;
  if (wen && rd != 0) {
    #ifdef CONFIG_RTRACE
    TRACE(rtrace, "0x%08x: %s = 0x%08x\n", pc, regs[rd], wdata);
    #endif
    cpu.gpr[rd] = wdata;
  }
  cpu.inst = inst;

  #ifdef CONFIG_FTRACE
  if (jump) {
    ftrace_enter(pc, target,rd ,rs1);
  }
  #endif
  
  int len = sprintf(cpu.logbuf, "0x%08x: 0x%08x ", pc, inst);
  disassemble(cpu.logbuf + len, sizeof(cpu.logbuf) - len, cpu.pc, (uint8_t *)&cpu.inst, 4);

  #ifdef CONFIG_ITRACE
  itrace_record(cpu.logbuf);
  #endif

}

