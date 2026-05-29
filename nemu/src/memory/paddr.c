/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <memory/host.h>
#include <memory/paddr.h>
#include <device/mmio.h>
#include <isa.h>


#ifdef USE_SOC
static uint8_t mrom[MROM_SIZE] PG_ALIGN = {};
static uint8_t sram[SRAM_SIZE] PG_ALIGN = {};
static uint8_t *pmem = NULL;
#else

#if   defined(CONFIG_PMEM_MALLOC)
static uint8_t *pmem = NULL;
#else // CONFIG_PMEM_GARRAY
static uint8_t pmem[CONFIG_MSIZE] PG_ALIGN = {};
#endif

#endif


uint8_t* guest_to_host(paddr_t paddr) { 
  #ifdef USE_SOC
  if (in_mrom(paddr)) {
    return mrom + paddr - MROM_BASE;
  }else if (in_sram(paddr)) {
    return sram + paddr - SRAM_BASE;
  }else {
    panic("addr="FMT_PADDR" is out of bound of mrom and sram at PC="FMT_WORD, paddr, cpu.pc);
  }
  #else
  return pmem + paddr - CONFIG_MBASE; 
  #endif
}
paddr_t host_to_guest(uint8_t *haddr) { return haddr - pmem + CONFIG_MBASE; }

#ifndef USE_SOC
static word_t pmem_read(paddr_t addr, int len) {
  word_t ret = host_read(guest_to_host(addr), len);
  return ret;
}

static void pmem_write(paddr_t addr, int len, word_t data) {
  host_write(guest_to_host(addr), len, data);
}

static void out_of_bound(paddr_t addr) {
  panic("address = " FMT_PADDR " is out of bound of pmem [" FMT_PADDR ", " FMT_PADDR "] at pc = " FMT_WORD,
      addr, PMEM_LEFT, PMEM_RIGHT, cpu.pc);
}
#endif

void init_mem() {
#ifndef USE_SOC
#if   defined(CONFIG_PMEM_MALLOC)
  pmem = malloc(CONFIG_MSIZE);
  assert(pmem);
#endif
  IFDEF(CONFIG_MEM_RANDOM, memset(pmem, rand(), CONFIG_MSIZE));
  Log("physical memory area [" FMT_PADDR ", " FMT_PADDR "]", PMEM_LEFT, PMEM_RIGHT);
#else
  Log("mrom area [" FMT_PADDR ", " FMT_PADDR "]", MROM_BASE, MROM_BASE + MROM_SIZE - 1);
  Log("sram area [" FMT_PADDR ", " FMT_PADDR "]", SRAM_BASE, SRAM_BASE + SRAM_SIZE - 1);

#endif
}

word_t paddr_read(paddr_t addr, int len) {
  #ifdef USE_SOC
  if (in_mrom(addr)) {
    return host_read(mrom + addr - MROM_BASE, len);
  }else if (in_sram(addr)) {
    return host_read(sram + addr - SRAM_BASE, len);
  }else {
    panic("addr="FMT_PADDR" is out of bound of mrom and sram at PC="FMT_WORD, addr, cpu.pc);
  }
  #else
  word_t ret = 0;
  if (likely(in_pmem(addr))) {
    #ifdef CONFIG_MTRACE
    log_write("[mtrace]:  read " FMT_PADDR ", len = %d, data = " , addr, len);
    #endif
    ret = pmem_read(addr, len);
    #ifdef CONFIG_MTRACE
    log_write(FMT_WORD "[S]\n", ret);
    #endif
    return ret;
  }
  IFDEF(CONFIG_DEVICE, 
    // #ifdef CONFIG_MTRACE
    // log_write("[mtrace]: io   read " FMT_PADDR ", len = %d, data = " , addr, len);
    // #endif
    ret = mmio_read(addr, len);
    // #ifdef CONFIG_MTRACE
    // log_write(FMT_WORD "[S]\n", ret);
    // #endif
    return ret);
  out_of_bound(addr);
  return 0;
  #endif
}

void paddr_write(paddr_t addr, int len, word_t data) {
  #ifdef USE_SOC
  if (in_mrom(addr)) {
    panic("should not write to mrom at PC="FMT_WORD, cpu.pc);
  }else if (in_sram(addr)) {
    host_write(sram + addr - SRAM_BASE, len, data);
  }else {
    panic("addr="FMT_PADDR" is out of bound of mrom and sram at PC="FMT_WORD, addr, cpu.pc);
  }
  #else

  if (likely(in_pmem(addr))) { 
    #ifdef CONFIG_MTRACE
    log_write("[mtrace]: write " FMT_PADDR ", len = %d, data = " FMT_WORD , addr, len, data);
    #endif
    pmem_write(addr, len, data);
    #ifdef CONFIG_MTRACE
    log_write("[S]\n");
    #endif
    return; 
  }
  IFDEF(CONFIG_DEVICE, 
    // #ifdef CONFIG_MTRACE
    // log_write("[mtrace]: io  write " FMT_PADDR ", len = %d, data = " FMT_WORD , addr, len, data);
    // #endif
    mmio_write(addr, len, data); 
    // #ifdef CONFIG_MTRACE
    // log_write("[S]\n");
    // #endif
    return);
  out_of_bound(addr);
  #endif
}

#ifdef USE_SOC
void write_mrom(paddr_t addr, int len, word_t data) {//used for loading program into mrom
  if (in_mrom(addr)) {
    host_write(mrom + addr - MROM_BASE, len, data);
  }else {
    panic("addr="FMT_PADDR" is out of bound of mrom", addr);
  }
}
#endif