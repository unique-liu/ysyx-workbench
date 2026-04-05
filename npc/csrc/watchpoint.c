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

#include <sdb.h>
// #include "include/debug.h"
#define NR_WP 32

// typedef struct watchpoint {
//   int NO;
//   struct watchpoint *next;

//   /* TODO: Add more members if necessary */
//   bool enabled;
//   char expr[256];
// } WP;

static WP wp_pool[NR_WP] = {};
static WP *head = NULL, *free_ = NULL;

void init_wp_pool() {
  int i;
  for (i = 0; i < NR_WP; i ++) {
    wp_pool[i].NO = i;
    wp_pool[i].next = (i == NR_WP - 1 ? NULL : &wp_pool[i + 1]);

    wp_pool[i].enabled = false;
    wp_pool[i].last_value = 0;
    wp_pool[i].expr[0] = '\0';
  }

  head = NULL;
  free_ = wp_pool;
}

/* TODO: Implement the functionality of watchpoint */

WP* new_wp(){
  if (free_==NULL) {
    printf( "No available watchpoint.\n");
    exit(1);
  }
  WP *wp = free_;
  free_ = free_->next;
  wp->next = head;
  head = wp;
  return wp;
}
void free_wp(WP *wp){
  WP *prev = NULL, *cur = head;
  while (cur!=NULL) {
    if (cur == wp) {
      if (prev == NULL) {
        head = cur->next;
      } else {
        prev->next = cur->next;
      }
      cur->next = free_;
      free_ = cur;
      return;
    }
    prev = cur;
    cur = cur->next;
  }
  printf("The watchpoint to be freed is not found.\n");
}
WP* find_wp(int NO) {
  if (NO>=0 && NO<NR_WP) {
    return &wp_pool[NO];
  }
  return NULL;
}
void display_wp() {
  WP *cur = head;
  if (cur == NULL) {
    printf("No watchpoint.\n");
    return;
  }
  printf("----- watchpoint list -----\n");
  printf("NO\tENABLED\tLAST\t\tEXPR\n");
  
  while (cur!=NULL) {
    printf("%d\t%s\t0x%08x\t\t\t%s\n", cur->NO, cur->enabled?"true":"false", cur->last_value, cur->expr);
    cur = cur->next;
  }
  printf("----- watchpoint list end -----\n");
}
int check_wp() {
  WP *cur = head;
  int triggered = 0;
  while (cur!=NULL) {
    if (cur->enabled) {
      bool success;
      word_t val = expr(cur->expr, &success);
      if (!success) {
        printf("failed to evaluate watchpoint %d expression %s.\n", cur->NO, cur->expr);
      } else if (val != cur->last_value) {
        printf("Hit watchpoint %d: (%s) = 0x%08x (last: 0x%08x)\n", cur->NO, cur->expr, val, cur->last_value);
        cur->last_value = val;
        triggered++;
      }
    }
    cur = cur->next;
  }
  return triggered;
}