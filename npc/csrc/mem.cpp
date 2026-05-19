#include <mem.h>
#include <exec.h>
#include <device.h>

int mem_pc_now = 0;
uint8_t mem[CONFIG_MSIZE];
#define QUEUE_SIZE 10
int use_device_pc[QUEUE_SIZE];
int use_device_pc_head = 0;
int use_device_pc_tail = 0;

// for device access checkout in difftest, record the pc of instructions that access device, and skip checkout for these instructions
void use_device_pc_in(int pc) {
    if ((use_device_pc_head + 1) % QUEUE_SIZE == use_device_pc_tail) {
        printf("use_device_pc queue is full!\n");
        npc_state.type = NPC_ERROR;
        npc_state.halt_pc = pc;
    }else {
        use_device_pc[use_device_pc_head] = pc;
        use_device_pc_head = (use_device_pc_head + 1) % QUEUE_SIZE;
    }
}
int use_device_pc_checkout(int pc) {
     if (use_device_pc_head == use_device_pc_tail) {
        return 0; // queue is empty
    }else if(pc!=0 && pc == use_device_pc[use_device_pc_tail]) {
        use_device_pc_tail = (use_device_pc_tail + 1) % QUEUE_SIZE;
        return 1; // checkout success
    }else {
        return 0; // checkout fail
    }
}

uint8_t* guest_to_host(paddr_t paddr) { 
  if (paddr >= PMEM_LEFT && paddr < PMEM_RIGHT) {
    return mem + (paddr - CONFIG_MBASE); 
  }
  DEBUG_PRINT(error, T, "g2h address 0x%08x is out of bound of pmem [0x%08x, 0x%08x]\n", paddr, PMEM_LEFT, PMEM_RIGHT);
  panic("address 0x%08x is out of bound of pmem [0x%08x, 0x%08x]", paddr, PMEM_LEFT, PMEM_RIGHT);
}
paddr_t host_to_guest(uint8_t *haddr) { 
  paddr_t addr = haddr - mem + CONFIG_MBASE;
  if (addr >= PMEM_LEFT && addr < PMEM_RIGHT) {
    return addr;
  }
  DEBUG_PRINT(error, T, "h2g address 0x%08x is out of bound of pmem [0x%08x, 0x%08x]\n", addr, PMEM_LEFT, PMEM_RIGHT);
  panic("address 0x%08x is out of bound of pmem [0x%08x, 0x%08x]", addr, PMEM_LEFT, PMEM_RIGHT);
}

static int in_pmem(int addr) {
    if (addr >= PMEM_LEFT && addr + 4 <= PMEM_RIGHT) {
      return 1;
    }
    return 0;
}

static void out_of_bound(int addr) {
    printf("address 0x%08x is out of bound of pmem [0x%08x, 0x%08x]\n", addr, PMEM_LEFT, PMEM_RIGHT);
    DEBUG_PRINT(error, T, "mem address 0x%08x is out of bound of pmem [0x%08x, 0x%08x]\n", addr, PMEM_LEFT, PMEM_RIGHT);
    npc_state.type = NPC_ERROR;
}

int pmem_read(int addr, int len) {
  int real_addr = addr - PMEM_LEFT;
  int ret = mem[real_addr] | (mem[real_addr + 1] << 8) | (mem[real_addr + 2] << 16) | (mem[real_addr + 3] << 24);

  return ret;
}

void pmem_write(int addr, int len, int wdata,char wmask) {
  int real_addr = addr - PMEM_LEFT;
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

//read and write with device
int mmio_read(int addr,int len){
  #ifndef CONFIG_DEVICE
  TRACE(dtrace,"read device is not supported\n");
  out_of_bound(addr);
  return 0;
  #endif

  int device_id = in_device(addr);
   
  if (device_id != -1) {
    int ret = device_read(device_id, addr, len);
    #ifdef CONFIG_DTRACE
    TRACE(dtrace,"pc 0x%08x read [%s@0x%08x] with length %d get 0x%08x\n", mem_pc_now, device_map[device_id].name, addr, len, ret);
    #endif
    #ifdef CONFIG_DIFFTEST
    if(npc_state.difftest_on == DIFF_ON) {
      use_device_pc_in(mem_pc_now);
    }
    #endif
    return ret;
  }
    out_of_bound(addr);
    return 0;
}

int mmio_write(int addr,int len,int wdata,char wmask){
  #ifndef CONFIG_DEVICE
  TRACE(dtrace,"write device is not supported\n");
  out_of_bound(addr);
  return 0;
  #endif

  int device_id = in_device(addr);
  if (device_id != -1) {
    #ifdef CONFIG_DTRACE
    TRACE(dtrace,"pc 0x%08x write [%s@0x%08x] with length %d mask %d save 0x%08x\n", mem_pc_now, device_map[device_id].name, addr, len, wmask, wdata);
    #endif
    #ifdef CONFIG_DIFFTEST
    if(npc_state.difftest_on == DIFF_ON) {
      use_device_pc_in(mem_pc_now);
    }
    #endif
    return device_write(device_id, addr, len, wdata, wmask);
  }
  out_of_bound(addr);
  return 0;
}
//read and write with memory
int memory_read(int addr, int len){
  int ret = 0;
  if (in_pmem(addr)) {
    ret = pmem_read(addr, len);
    #ifdef CONFIG_MTRACE
    TRACE(mtrace,"pc 0x%08x read 0x%08x with length %d get 0x%08x\n", mem_pc_now, addr, len, ret);
    #endif
    return ret;
  }
  out_of_bound(addr);
  return 0;
}

void memory_write(int addr, int len, int wdata, char wmask){
  if (in_pmem(addr)) { 
    pmem_write(addr, len, wdata, wmask);
    #ifdef CONFIG_MTRACE
    TRACE(mtrace,"pc 0x%08x write 0x%08x with length %d mask %d save 0x%08x\n", mem_pc_now, addr, len, wmask, wdata);
    #endif
    return; 
  }
  out_of_bound(addr);
}


//read and write with memory or device
int paddr_read(int addr, int len) {
  int ret = 0;
  if (in_pmem(addr)) {
    ret = pmem_read(addr, len);
    #ifdef CONFIG_MTRACE
    TRACE(mtrace,"pc 0x%08x read 0x%08x with length %d get 0x%08x\n", mem_pc_now, addr, len, ret);
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
    TRACE(mtrace,"pc 0x%08x write 0x%08x with length %d mask %d save 0x%08x\n", mem_pc_now, addr, len, wmask, wdata);
    #endif
    return; 
  }
 #ifdef CONFIG_DEVICE
    mmio_write(addr, len, wdata, wmask); 
    return;
#endif
  out_of_bound(addr);
}

// extern "C" void flash_read(int32_t addr, int32_t *data) { assert(0); }
// extern "C" void mrom_read(int32_t addr, int32_t *data) { assert(0); }