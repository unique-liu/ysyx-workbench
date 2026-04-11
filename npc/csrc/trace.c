#include <trace.h>
#include <exec.h>

struct function_table func_table[FTRACE_MAX_FUNC_NUM];
uint64_t ftrace_call_depth = 0; 
word_t ftrace_call_stack[FTRACE_MAX_CALL_DEPTH];
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
    printf("   %s\n", iringbuf[idx]);
    idx = (idx + 1) % IRINGBUF_SIZE;
  }
  printf("=> %s\n", iringbuf[error_idx]);
}

void itrace_record(const char *s) {
  DEBUG_PRINT(itrace,T,"%s\n", s);
  iringbuf_record(s);
}
//iringbuf end

//ftrace
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
#ifdef CONFIG_FTRACE
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
  if (current_idx == target_idx && rd == 0) {
    // should be "j" instruction, not a call, ignore it
    return;
  }
  DEBUG_PRINT(ftrace,T,"deep%2d [%10s@0x%08x]%s call deep%2d[%10s@0x%08x]\n", (int)ftrace_call_depth, ((current_idx != -1) ? func_table[current_idx].name : "???"), pc,((rd != 0) ? "" : "tail"), ((rd!=0)?(int)ftrace_call_depth+1:(int)ftrace_call_depth), ((target_idx != -1) ? func_table[target_idx].name : "???"), target);
  if (ftrace_call_depth < FTRACE_MAX_CALL_DEPTH) {
    if (rd != 0) {
      ftrace_call_stack[ftrace_call_depth] = pc+4;
      ftrace_call_depth++;
    }
  }else {
    printf("ftrace call stack overflow at pc: 0x%08x\n", pc);
  }
}

static void ftrace_ret(word_t pc, word_t target) {
  int target_deep  = 0;
  for (int i = ftrace_call_depth-1; i>=0; i--) {
    if (ftrace_call_stack[i] == target) {
      target_deep = i;
      break;
    }
  }
  int current_idx = find_function_by_addr(pc);
  int target_idx = find_function_by_addr(target);
  DEBUG_PRINT(ftrace,T,"deep%2d [%10s@0x%08x]  ret deep%2d[%10s@0x%08x]\n", (int)ftrace_call_depth, ((current_idx != -1) ? func_table[current_idx].name : "???"), pc, target_deep, ((target_idx != -1) ? func_table[target_idx].name : "???"), target);
  ftrace_call_depth = target_deep;
}
#endif

void ftrace_enter(word_t pc, word_t target,int rd,int rs1){
  #ifdef CONFIG_FTRACE
  if (rs1 != 1 || rd != 0) {
    ftrace_call(pc, target,rd);
  }else {
    ftrace_ret(pc, target);
  }
  #endif
}
//ftrace end