#include <mem.h>

uint8_t mem[MEM_SIZE_BYTES];

static int in_pmem(int addr) {
    if (addr >= MEM_BASE && addr + 4 <= MEM_SIZE_BYTES + MEM_BASE - 1) {
      return 1;
    }
    return 0;
}

static void out_of_bound(int addr) {
    printf("address 0x%08x is out of bound of pmem [0x%08x, 0x%08x]\n", addr, MEM_BASE, MEM_BASE + MEM_SIZE_BYTES - 1);
    DEBUG_PRINT(error, T, "address 0x%08x is out of bound of pmem [0x%08x, 0x%08x]\n", addr, MEM_BASE, MEM_BASE + MEM_SIZE_BYTES - 1);
    npc_state.type = NPC_ERROR;
}

int pmem_read(int addr, int len) {
  int real_addr = addr - MEM_BASE;
  int ret = mem[real_addr] | (mem[real_addr + 1] << 8) | (mem[real_addr + 2] << 16) | (mem[real_addr + 3] << 24);

  return ret;
}

void pmem_write(int addr, int len, int wdata,char wmask) {
  int real_addr = addr - MEM_BASE;
if (wmask & 1) {
    mem[real_addr] = wdata & 0xFF;
  }
  if ((wmask >> 1) &1 ) {
    mem[real_addr + 1] = (wdata >> 8) & 0xFF;
  }
  if ((wmask >> 2) & 1) {
    mem[real_addr + 2] = (wdata >> 16) & 0xFF;
  }
  if ((wmask >> 3) & 1) {
    mem[real_addr + 3] = (wdata >> 24) & 0xFF;
  }
}

int mmio_read(int addr,int len){
    out_of_bound(addr);
    return 0;
}

int mmio_write(int addr,int len,int wdata,char wmask){
    out_of_bound(addr);
    return 0;
}

int paddr_read(int addr, int len) {
  int ret = 0;
  if (in_pmem(addr)) {
    #ifdef CONFIG_MTRACE
    DEBUG_PRINT(mtrace, T, "read 0x%08x with length %d", addr, len);
    #endif
    ret = pmem_read(addr, len);
    #ifdef CONFIG_MTRACE
    DEBUG_PRINT(mtrace, T, " get 0x%08x [S]\n", ret);
    #endif
    return ret;
  }
#ifdef CONFIG_DEVICE
    #ifdef CONFIG_DTRACE
    DEBUG_PRINT(dtrace, T, "read 0x%08x with length %d", addr, len);
    #endif
    ret = mmio_read(addr, len);
    #ifdef CONFIG_DTRACE
    DEBUG_PRINT(dtrace, T, " get 0x%08x [S]\n", ret);
    #endif
    return ret;
#endif
  out_of_bound(addr);
  return 0;
}

void paddr_write(int addr, int len, int wdata, char wmask) {

  if (in_pmem(addr)) { 
    #ifdef CONFIG_MTRACE
    DEBUG_PRINT(mtrace, T, "write 0x%08x with length %d", addr, len);
    #endif
    pmem_write(addr, len, wdata, wmask);
    #ifdef CONFIG_MTRACE
    DEBUG_PRINT(mtrace, T, " get 0x%08x [S]\n", wdata);
    #endif
    return; 
  }
 #ifdef CONFIG_DEVICE
    #ifdef CONFIG_DTRACE
    DEBUG_PRINT(dtrace, T, "write 0x%08x with length %d save 0x%08x", addr, len, wdata);
    #endif
    mmio_write(addr, len, wdata, wmask); 
    #ifdef CONFIG_DTRACE
    DEBUG_PRINT(dtrace, T, " [S]\n");
    #endif
    return;
#endif
  out_of_bound(addr);
}
