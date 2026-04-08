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
  int device_id = in_device(addr);
   
  if (device_id != -1) {
    int ret = device_read(device_id, addr, len);
    #ifdef CONFIG_DTRACE
    DEBUG_PRINT(dtrace, T, "read [%s@0x%08x] with length %d get 0x%08x\n", device_map[device_id].name, addr, len, ret);
    #endif
    return ret;
  }
    out_of_bound(addr);
    return 0;
}

int mmio_write(int addr,int len,int wdata,char wmask){
  int device_id = in_device(addr);
  if (device_id != -1) {
    #ifdef CONFIG_DTRACE
    DEBUG_PRINT(dtrace, T, "write [%s@0x%08x] with length %d save 0x%08x\n", device_map[device_id].name, addr, len, wdata);
    #endif
    return device_write(device_id, addr, len, wdata, wmask);
  }
    out_of_bound(addr);
    return 0;
}

int paddr_read(int addr, int len) {
  int ret = 0;
  if (in_pmem(addr)) {
    ret = pmem_read(addr, len);
    #ifdef CONFIG_MTRACE
    DEBUG_PRINT(mtrace, T, "read 0x%08x with length %d get 0x%08x\n", addr, len, ret);
    #endif
    return ret;
  }
#ifdef CONFIG_DEVICE
    ret = mmio_read(addr, len);
    return ret;
#endif
  out_of_bound(addr);
  return 0;
}

void paddr_write(int addr, int len, int wdata, char wmask) {

  if (in_pmem(addr)) { 
    pmem_write(addr, len, wdata, wmask);
    #ifdef CONFIG_MTRACE
    DEBUG_PRINT(mtrace, T, "write 0x%08x with length %d save 0x%08x\n", addr, len, wdata);
    #endif
    return; 
  }
 #ifdef CONFIG_DEVICE
    mmio_write(addr, len, wdata, wmask); 
    return;
#endif
  out_of_bound(addr);
}
