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
#include "../local-include/reg.h"
#include <isa.h>
const trap_code_t trap_code[] = {
  {0, "Instruction address misaligned"},
  {1, "Instruction access fault"},
  {2, "Illegal instruction"},
  {3, "Breakpoint"},
  {4, "Load address misaligned"},
  {5, "Load access fault"},
  {6, "Store/AMO address misaligned"},
  {7, "Store/AMO access fault"},
  {8, "Environment call from U-mode"},
  {9, "Environment call from S-mode"},
  {11, "Environment call from M-mode"}
};

word_t isa_raise_intr(word_t NO, vaddr_t epc) {
  /* TODO: Trigger an interrupt/exception with ``NO''.
   * Then return the address of the interrupt/exception vector.
   */
  #ifdef CONFIG_ETRACE
  log_write("[etrace]: intr NO = %d, epc = " FMT_WORD "mtvec = " FMT_WORD "\n", NO, epc, csr_n("mtvec"));
  #endif
  csr_n("mepc") = epc;
  csr_n("mcause") = NO;
  return csr_n("mtvec");
}

word_t isa_return_intr(){
  #ifdef CONFIG_ETRACE
  log_write("[etrace]: mret, mepc = " FMT_WORD "\n", csr_n("mepc"));
  #endif
  return csr_n("mepc");
}

word_t isa_query_intr() {
  return INTR_EMPTY;
}
