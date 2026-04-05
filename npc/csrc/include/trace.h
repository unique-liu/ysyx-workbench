#ifndef TRACE_H
#define TRACE_H

#include <stdio.h>
#include <string.h>
#include <stdint.h>
#include <elf.h>
#include <assert.h>
#include <config.h>
#include "debug.h"

typedef uint32_t word_t ;

#define IRINGBUF_SIZE 16

//ftrace
#define FTRACE_MAX_NAME_LEN 32
#define FTRACE_MAX_FUNC_NUM 100
#define FTRACE_MAX_CALL_DEPTH 100
struct function_table{
  char name[FTRACE_MAX_NAME_LEN];
  word_t addr;
  long size;
};

int init_function_table(FILE * fp);
void itrace_record(const char *s);
void iringbuf_print();
void ftrace_enter(word_t pc, word_t target,int rd,int rs1);

#endif // TRACE_H