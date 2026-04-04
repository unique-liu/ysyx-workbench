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

#include <cpu/cpu.h>
#include <cpu/decode.h>
#include <cpu/difftest.h>
#include <locale.h>
#include "../monitor/sdb/sdb.h"
#include <elf.h>
/* The assembly code of instructions executed is only output to the screen
 * when the number of instructions executed is less than this value.
 * This is useful when you use the `si' command.
 * You can modify this value as you want.
 */
#define MAX_INST_TO_PRINT 10
#define IRINGBUF_SIZE 16

CPU_state cpu = {};
uint64_t g_nr_guest_inst = 0;
static uint64_t g_timer = 0; // unit: us
static bool g_print_step = false;

//iringbuf
char iringbuf[IRINGBUF_SIZE][128];
int iringbuf_idx = 0;
void iringbuf_record(const char *s) {
  strncpy(iringbuf[iringbuf_idx], s, 128);
  iringbuf_idx = (iringbuf_idx + 1) % IRINGBUF_SIZE;
}
void iringbuf_print() {
  int idx = iringbuf_idx;
  int error_idx = (idx - 1 + IRINGBUF_SIZE) % IRINGBUF_SIZE;
  printf("Instruction Ring Buffer (most recent at the top):\n");
  while (idx != error_idx) {
    idx = (idx + 1) % IRINGBUF_SIZE;
    printf("   %s\n", iringbuf[idx]);
  }
  printf("=> %s\n", iringbuf[error_idx]);
}
//iringbuf end

//ftrace
#define FTRACE_MAX_NAME_LEN 32
#define FTRACE_MAX_FUNC_NUM 100
#define FTRACE_MAX_CALL_DEPTH 100
struct function_table{
  char name[FTRACE_MAX_NAME_LEN];
  word_t addr;
  long size;
};

struct function_table func_table[FTRACE_MAX_FUNC_NUM];
uint64_t ftrace_call_depth = 0; 
word_t ftrace_call_stack[FTRACE_MAX_CALL_DEPTH];

int init_function_table(FILE * fp) {
  int fread_ret = 0;
  // 读取ELF文件头
  Elf32_Ehdr elf_header;
  fread_ret = fread(&elf_header, sizeof(Elf32_Ehdr), 1, fp);
  assert(fread_ret == 1);
  // 定位到节区头表
  fseek(fp, elf_header.e_shoff, SEEK_SET);

  // 读取节区头表
  Elf32_Shdr section_headers[elf_header.e_shnum];
  fread_ret = 0;
  fread_ret = fread(section_headers, sizeof(Elf32_Shdr), elf_header.e_shnum, fp);
  assert(fread_ret == elf_header.e_shnum);

  // 查找符号表和字符串表
  Elf32_Shdr *symtab_section = NULL;
  Elf32_Shdr *strtab_section = NULL;
  for (int i = 0; i < elf_header.e_shnum; i++) {
    if (section_headers[i].sh_type == SHT_SYMTAB) {
      symtab_section = &section_headers[i];
    } else if (section_headers[i].sh_type == SHT_STRTAB && i != elf_header.e_shstrndx) {
      strtab_section = &section_headers[i];
    }
  }

  if (symtab_section == NULL || strtab_section == NULL) {
    fprintf(stderr, "Failed to find symbol table or string table in ELF file.\n");
    return -1;
  }

  // 读取符号表和字符串表
  Elf32_Sym symtab[symtab_section->sh_size / sizeof(Elf32_Sym)];
  char strtab[strtab_section->sh_size];

  fseek(fp, symtab_section->sh_offset, SEEK_SET);
  fread_ret = 0;
  fread_ret = fread(symtab, sizeof(Elf32_Sym), symtab_section->sh_size / sizeof(Elf32_Sym), fp);
  assert(fread_ret == symtab_section->sh_size / sizeof(Elf32_Sym));

  fseek(fp, strtab_section->sh_offset, SEEK_SET);
  fread_ret = 0;
  fread_ret = fread(strtab, sizeof(char), strtab_section->sh_size, fp);
  assert(fread_ret == strtab_section->sh_size);

  // 提取函数信息
  int func_count = 0;
  for (int i = 0; i < symtab_section->sh_size / sizeof(Elf32_Sym); i++) {
    if (ELF32_ST_TYPE(symtab[i].st_info) == STT_FUNC && symtab[i].st_size > 0) {
      strncpy(func_table[func_count].name, &strtab[symtab[i].st_name], sizeof(func_table[func_count].name) - 1);
      func_table[func_count].addr = symtab[i].st_value;
      func_table[func_count].size = symtab[i].st_size;
      func_count++;
      if (func_count >= FTRACE_MAX_FUNC_NUM) {
        fprintf(stderr, "Function table is full, some functions may not be recorded.\n");
        break;
      }
    }
  }
  return func_count;
}

static int find_function_by_addr(word_t addr) {
  for (int i = 0; i < FTRACE_MAX_FUNC_NUM; i++) {
    if (addr >= func_table[i].addr && addr < func_table[i].addr + func_table[i].size) {
      return i;
    }
  }
  return -1;
}

static void ftrace_call(word_t pc, word_t target,int rd) {
  
  int current_idx = find_function_by_addr(pc);
  int target_idx = find_function_by_addr(target);
  log_write("[ftrace]:deep%2d [%10s@"FMT_PADDR"]call deep%2d[%10s@"FMT_PADDR"]\n", (int)ftrace_call_depth, ((current_idx != -1) ? func_table[current_idx].name : "???"), pc, (int)ftrace_call_depth+1, ((target_idx != -1) ? func_table[target_idx].name : "???"), target);
  if (ftrace_call_depth < FTRACE_MAX_CALL_DEPTH) {
    ftrace_call_stack[ftrace_call_depth] = (rd != 0)? pc+4 : 0;
  }
  ftrace_call_depth++;
}

static void ftrace_ret(word_t pc, word_t target) {
  int current_deep = ftrace_call_depth - 1;
  int target_deep  = 0;
  for (int i = current_deep-1; i>=0; i--) {
    if (ftrace_call_stack[i] == target) {
      target_deep = i;
      break;
    }
  }
  int current_idx = find_function_by_addr(pc);
  int target_idx = find_function_by_addr(target);
  log_write("[ftrace]:deep%2d [%10s@"FMT_PADDR"] ret deep%2d[%s@"FMT_PADDR"]\n", (int)ftrace_call_depth, ((current_idx != -1) ? func_table[current_idx].name : "???"), pc, target_deep, ((target_idx != -1) ? func_table[target_idx].name : "???"), target);
  ftrace_call_depth = target_deep+1;
}


void ftrace_enter(word_t pc, word_t target,int rd,int rs1){
  #ifdef CONFIG_FTRACE
  if (rs1 != 1) {
    if (rd != 0) {
      ftrace_call(pc, target,rd);
    }
    
  }else {
    ftrace_ret(pc, target);
  }
  #endif
}
//ftrace end

void device_update();

static void trace_and_difftest(Decode *_this, vaddr_t dnpc) {
#ifdef CONFIG_ITRACE_COND
  if (ITRACE_COND) { log_write("[itrace]%s\n", _this->logbuf); }
#endif

  iringbuf_record(_this->logbuf);

  if (g_print_step) { IFDEF(CONFIG_ITRACE, puts(_this->logbuf)); }
  IFDEF(CONFIG_DIFFTEST, difftest_step(_this->pc, dnpc));

  #ifdef CONFIG_WATCHPOINT
  int trigger_wp = check_wp();
  if (trigger_wp!=0) {
    nemu_state.state = NEMU_STOP;
    printf("Hit %d watchpoints.\n", trigger_wp);
  }
  #endif
}

static void exec_once(Decode *s, vaddr_t pc) {
  s->pc = pc;
  s->snpc = pc;
  isa_exec_once(s);
  cpu.pc = s->dnpc;
#ifdef CONFIG_ITRACE
  char *p = s->logbuf;
  p += snprintf(p, sizeof(s->logbuf), FMT_WORD ":", s->pc);
  int ilen = s->snpc - s->pc;
  int i;
  uint8_t *inst = (uint8_t *)&s->isa.inst;
#ifdef CONFIG_ISA_x86
  for (i = 0; i < ilen; i ++) {
#else
  for (i = ilen - 1; i >= 0; i --) {
#endif
    p += snprintf(p, 4, " %02x", inst[i]);
  }
  int ilen_max = MUXDEF(CONFIG_ISA_x86, 8, 4);
  int space_len = ilen_max - ilen;
  if (space_len < 0) space_len = 0;
  space_len = space_len * 3 + 1;
  memset(p, ' ', space_len);
  p += space_len;

  void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
  disassemble(p, s->logbuf + sizeof(s->logbuf) - p,
      MUXDEF(CONFIG_ISA_x86, s->snpc, s->pc), (uint8_t *)&s->isa.inst, ilen);
#endif
}

static void execute(uint64_t n) {
  Decode s;
  for (;n > 0; n --) {
    exec_once(&s, cpu.pc);
    g_nr_guest_inst ++;
    trace_and_difftest(&s, cpu.pc);
    if (nemu_state.state != NEMU_RUNNING) break;
    IFDEF(CONFIG_DEVICE, device_update());
  }
}

static void statistic() {
  IFNDEF(CONFIG_TARGET_AM, setlocale(LC_NUMERIC, ""));
#define NUMBERIC_FMT MUXDEF(CONFIG_TARGET_AM, "%", "%'") PRIu64
  Log("host time spent = " NUMBERIC_FMT " us", g_timer);
  Log("total guest instructions = " NUMBERIC_FMT, g_nr_guest_inst);
  if (g_timer > 0) Log("simulation frequency = " NUMBERIC_FMT " inst/s", g_nr_guest_inst * 1000000 / g_timer);
  else Log("Finish running in less than 1 us and can not calculate the simulation frequency");
}

void assert_fail_msg() {
  isa_reg_display();
  statistic();
}

/* Simulate how the CPU works. */
void cpu_exec(uint64_t n) {
  g_print_step = (n < MAX_INST_TO_PRINT);
  switch (nemu_state.state) {
    case NEMU_END: case NEMU_ABORT: case NEMU_QUIT:
      printf("Program execution has ended. To restart the program, exit NEMU and run again.\n");
      return;
    default: nemu_state.state = NEMU_RUNNING;
  }

  uint64_t timer_start = get_time();

  execute(n);

  uint64_t timer_end = get_time();
  g_timer += timer_end - timer_start;

  switch (nemu_state.state) {
    case NEMU_RUNNING: nemu_state.state = NEMU_STOP; break;

    case NEMU_END: case NEMU_ABORT:
      Log("nemu: %s at pc = " FMT_WORD,
          (nemu_state.state == NEMU_ABORT ? ANSI_FMT("ABORT", ANSI_FG_RED) :
           (nemu_state.halt_ret == 0 ? ANSI_FMT("HIT GOOD TRAP", ANSI_FG_GREEN) :
            ANSI_FMT("HIT BAD TRAP", ANSI_FG_RED))),
          nemu_state.halt_pc);
      // fall through
    case NEMU_QUIT: statistic();
  }
}
