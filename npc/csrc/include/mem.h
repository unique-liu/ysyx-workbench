#ifndef MEM_H
#define MEM_H

#include <common.h>


#define PMEM_LEFT  ((paddr_t)CONFIG_MBASE)
#define PMEM_RIGHT ((paddr_t)CONFIG_MBASE + CONFIG_MSIZE - 1)
#define RESET_VECTOR (PMEM_LEFT + CONFIG_PC_RESET_OFFSET)

typedef int vaddr_t ;
typedef int paddr_t ;

extern int mem_pc_now;
extern uint8_t mem[CONFIG_MSIZE];

/* convert the guest physical address in the guest program to host virtual address in NEMU */
uint8_t* guest_to_host(paddr_t paddr);
/* convert the host virtual address in NEMU to guest physical address in the guest program */
paddr_t host_to_guest(uint8_t *haddr);
int mmio_read(int addr,int len);
int mmio_write(int addr,int len,int wdata,char wmask);
int memory_read(int addr, int len);
void memory_write(int addr, int len, int wdata, char wmask);
int paddr_read(int addr, int len);
void paddr_write(int addr, int len, int wdata, char wmask);

int use_device_pc_checkout(int pc);

#endif