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
#include <exec.h>
#include <mem.h>
#include <isa.h>
#include <readline/readline.h>
#include <readline/history.h>
#include <sdb.h>
#include <macro.h>
#include <stdio.h>
#include <stdlib.h>

static int is_batch_mode = false;
static int reseted = 0,quit = 0;

void init_regex();
void init_wp_pool();


/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(npc) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char *args) {
  if (npc_state.type != NPC_WAITING){
    printf("Error: Cannot step into instructions while the program is not in waiting state.\n");
    return 0;
  }
  if (reseted == 0) {
    reset(RESET_TIME);
    reseted = 1;
  }
  npc_state.type = NPC_RUNNING;
  execute(-1);
  return 0;
}


static int cmd_q(char *args) {
  if (npc_state.type == NPC_WAITING) {
    npc_state.type = NPC_STOP;
  }
  quit = 1;
  return 0;
}

static int cmd_help(char *args);
static int cmd_test(char *args);
static int cmd_si(char *args);
static int cmd_info(char *args);
static int cmd_x(char *args);
static int cmd_p(char *args);
static int cmd_w(char *args);
static int cmd_d(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  /* TODO: Add more commands */
  { "test", "Run the test cases for sdb", cmd_test },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  { "si", "Step into instruction(s)", cmd_si },
  { "info", "Display register or watchpoint information", cmd_info },
  { "x", "Examine memory: x N EXPR", cmd_x },
  { "p", "Evaluate expression EXPR and print the result", cmd_p },
  { "w", "Set a watchpoint for an expression", cmd_w },
  { "d", "Delete a watchpoint", cmd_d },

};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}
static int cmd_test(char *args) {
  extern void test_expr();
  test_expr();
  return 0;
}

static int cmd_si(char *args){
  if (npc_state.type != NPC_WAITING){
    printf("Error: Cannot step into instructions while the program is not in waiting state.\n");
    return 0;
  }
  if (reseted == 0) {
    reset(RESET_TIME);
    reseted = 1;
  }
  char *arg = strtok(NULL, " ");
  int steps = 1;
  if(arg != NULL){
    steps = atoi(arg);
  }
  npc_state.type = NPC_RUNNING;
  execute(steps);
  return 0;
}

static int cmd_info(char *args){
  char *arg = strtok(NULL, " ");
  if(arg == NULL){
    printf("Usage: info [r|w]\n");
    return 0;
  }
  if(strcmp(arg, "r") == 0){
    isa_reg_display(&cpu);
  }
  else if(strcmp(arg, "w") == 0){
    display_wp();
  }
  else{
    printf("Unknown argument '%s'\n", arg);
  }
  return 0;
}

static int cmd_x(char *args){
  char *arg = strtok(NULL, " ");
  if(arg == NULL){
    printf("Error: No number of units specified.\n");
    return 0;
  }
  //translate num of units to print
  int N = atoi(arg);
  arg = strtok(NULL, " ");
  if(arg == NULL){
    printf("Error: No address specified.\n");
    return 0;
  }

  //translate address to print and check if it is valid
  char *endptr = NULL;
  vaddr_t addr = strtoul(arg, &endptr, 16);
  if (*endptr != '\0') {
    printf("Invalid address: %s\n", arg);
    return 0;
  }
  vaddr_t end_addr = addr + N * sizeof(word_t);
  if (addr >= 0x87ffffff || addr < 0x80000000 || end_addr > 0x87ffffff || end_addr < 0x80000000) {
    printf("Invalid memory address: [0x%08x, 0x%08x]\n", (unsigned int)addr, (unsigned int)end_addr);
    printf("valid memory address: [0x80000000, 0x87ffffff]\n");
    return 0;
  }

  //print memory content
  printf("----- check mem: [0x%08x, 0x%08x] -----\n", (unsigned int)addr, (unsigned int)end_addr);
  for(int i = 0; i < N; i++){
    word_t data = paddr_read(addr + i * sizeof(word_t), sizeof(word_t));
    printf("0x%08x: 0x%08x\n", (unsigned int)(addr + i * sizeof(word_t)), data);
  }
  printf("----- check mem end -----\n");
  return 0;
}

static int cmd_p(char *args) {
  if (args == NULL) {
    printf("Usage: p EXPR\n");
    return 0;
  }
  bool success;
  word_t result = expr(args, &success);
  if (success) {
    printf("0x%08x\n", result);
  }else {
    printf("command p failed.\n");
  }
  return 0;
}

static int cmd_w(char *args) {
  if (args == NULL) {
    printf("Usage: w EXPR\n");
    return 0;
  }
  WP *wp = new_wp();
  strncpy(wp->expr, args, sizeof(wp->expr) - 1);
  wp->enabled = true;
  wp->last_value = expr(args, &(wp->enabled));
  wp->expr[sizeof(wp->expr) - 1] = '\0';
  
  if (!wp->enabled) {
    printf("failed to set watchpoint for expression %s.\n", args);
    free_wp(wp);
  } else {
    printf("watchpoint %d: %s\n", wp->NO, wp->expr);
  }
  return 0;
}

static int cmd_d(char *args) {
  if (args == NULL) {
    printf("Usage: d N\n");
    return 0;
  }
  int NO = atoi(args);
  WP *wp = find_wp(NO);
  if (wp == NULL) {
    printf("No watchpoint number %d.\n", NO);
    return 0;
  }
  free_wp(wp);
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_set_trace_mode(char *mode){
  if (strcmp(mode, "on") == 0) {
    npc_state.trace_on = TRACE_ON;
  } else if (strcmp(mode, "off") == 0) {
    npc_state.trace_on = TRACE_OFF;
  #ifdef CONFIG_AUTOTRACE
  } else if (strcmp(mode, "auto") == 0) {
    #ifndef CONFIG_BOARD
    npc_state.trace_on = is_batch_mode ? TRACE_AUTO : TRACE_OFF;
    #else
    printf("Warning: to avoid errors, board support can not work with multi thread\n");
    npc_state.trace_on = TRACE_OFF;
    #endif
  } else if(strcmp(mode, "clock") == 0) {
    npc_state.trace_on = TRACE_CLOCK;
  }
  #endif
  else {
    printf("Unknown trace mode '%s', use 'on' or 'off' maybe CONFIG_AUTOTRACE was not defined\n", mode);
  }
}

void sdb_set_difftest_mode(char *mode){
  if (strcmp(mode, "on") == 0) {
    npc_state.difftest_on = DIFF_ON;
    #ifndef CONFIG_DIFFTEST
    printf("Warning: DiffTest is not enabled in this build. Please enable CONFIG_DIFFTEST in config.h to use this feature.\n");
    #endif
  } else if (strcmp(mode, "off") == 0) {
    npc_state.difftest_on = DIFF_OFF;
  } else {
    printf("Unknown difftest mode '%s', use 'on' or 'off'\n", mode);
  }
} 

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    if (npc_state.type == NPC_WAITING) {
      npc_state.type = NPC_STOP;// interrupt by ctrl-c in batch mode
    }
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    // extern void sdl_clear_event_queue();
    // sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
    if (quit) {
      printf("Execution stopped.\n");
      return;
    }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
