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

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

// this should be enough
static char buf[65536] = {};
static int buflen = 0;
static char code_buf[65536 + 128] = {}; // a little larger than `buf`
static char *code_format =
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";
int choose(int n) {
  return rand() % n;
}
void gen_num() {
  int num = rand() % 10000;
  char format[32]= {0};
  if (choose(2) == 0) {
    sprintf(format, "%d", num);
  } else {
    sprintf(format, "0x%x", num);
  }
  strcat(buf, format);
  buflen += strlen(format);
}
void gen(char c) {
  char format[2] = {c, '\0'};
  strcat(buf, format);
  buflen += strlen(format);
}
void gen_rand_op() {
  char ops[] = "+-*/";
  char op = ops[choose(4)];
  gen(op);
}
void gen_space() {
  int num = choose(5) + 1; // generate 1 to 5 spaces
  char format[6] = {0};
  memset(format, ' ', num);
  format[num] = '\0';
  strcat(buf, format);
  buflen += num;
}
void gen_rand_expr() {
  int choice = choose(3);
  if (buflen>30000) {
    choice = 3; // generate a number to avoid buffer overflow
  }
  switch (choice) {
    case 0: gen_space(); gen_num();gen_space(); break;
    case 1: gen('('); gen_rand_expr(); gen(')'); break;
    case 2: gen_rand_expr(); gen_rand_op(); gen_rand_expr(); break;
    case 3: gen_num(); break;
  }
}
int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;
  for (i = 0; i < loop; i ++) {
    buf[0] = '\0';
    buflen = 0;
    gen_rand_expr();

    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc /tmp/.code.c -o /tmp/.expr");
    if (ret != 0) continue;

    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;
    ret = fscanf(fp, "%d", &result);
    pclose(fp);

    printf("%u %s\n", result, buf);
  }
  return 0;
}
