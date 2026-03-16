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

#include <isa.h>

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>
#include <debug.h>
enum {
  TK_NOTYPE = 256, TK_EQ,TK_DECIMAL,TK_HEX

  /* TODO: Add more token types */

};

static struct rule {
  const char *regex;
  int token_type;
} rules[] = {

  /* TODO: Add more rules.
   * Pay attention to the precedence level of different rules.
   */

  {" +", TK_NOTYPE},    // spaces
  {"\\+", '+'},         // plus
  {"-", '-'},           // minus
  {"\\*", '*'},         // multiply
  {"/", '/'},           // divide
  {"\\(", '('},         // left parenthesis
  {"\\)", ')'},         // right parenthesis
  {"0x[0-9a-fA-F]+", TK_HEX},      // hexadecimal number
  {"[0-9]+", TK_DECIMAL},      // decimal number
  // {"[a-zA-Z_][a-zA-Z0-9_]*", '1'}, // identifier (variable name)
  // {"!=", TK_EQ + 1},    // not equal
  // {">=", TK_EQ + 2},    // greater than or equal to
  // {"<=", TK_EQ + 3},    // less than or equal to
  // {">", '>'},           // greater than
  // {"<", '<'},           // less than
  {"==", TK_EQ},        // equal
};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex() {
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i ++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static Token tokens[10000] __attribute__((used)) = {};
static int nr_token __attribute__((used))  = 0;

static bool make_token(char *e) {
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0') {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i ++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        // Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
        //     i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */

        switch (rules[i].token_type) {
          case TK_NOTYPE: break; // do nothing for notype
          case '+': case '-': case '*': case '/': case '(': case ')':
            tokens[nr_token].type = rules[i].token_type;
            nr_token++;
            break;
          case TK_DECIMAL: case TK_HEX:
            if(substr_len >= sizeof(tokens[nr_token].str)) {
              printf("number too long at position %d\n%s\n%*.s^\n", position, e, position, "");
              return false;
            }
            tokens[nr_token].type = rules[i].token_type;
            strncpy(tokens[nr_token].str, substr_start, substr_len);
            tokens[nr_token].str[substr_len] = '\0'; // null-terminate
            nr_token++;
            break;
          default: 
            printf("unkown token type at position %d\n%s\n%*.s^\n", position, e, position, "");
            return false;
            break;
        }

        break;
      }
    }

    if (i == NR_REGEX) {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

bool check_parentheses(int p, int q, bool *error) {
  int count = 0;
  int surrounded_parentheses = 1;//judge whether the parentheses at position p and q are a pair of parentheses that can surround the whole expression
  //scan all the parentheses in the expression
  for (int i = p; i <= q; i++) {
    if (tokens[i].type == '(') {
      count++;
    } else if (tokens[i].type == ')') {
      count--;
    }
    if (count == 0 && i < q) {
      surrounded_parentheses = 0;
    }
  }
  if (count == 0) {
    *error = false;
    if (surrounded_parentheses == 1) {
      return true;
    }else {
      return false;
    }
  }else {
    printf("unmatched parentheses.\n");
    *error = true;
    return false;
  }
}

int find_main_operator(int p, int q) {
  int main_op = -1;
  int min_precedence = 100; // a large number
  int parentheses_count = 0;

  for (int i = p; i <= q; i++) {
    if (tokens[i].type == '(') {
      parentheses_count++;
    } else if (tokens[i].type == ')') {
      parentheses_count--;
    } 
    if (parentheses_count == 0) { // only consider operators outside parentheses
      int precedence;
      switch (tokens[i].type) {
        case '+': case '-': precedence = 1; break;
        case '*': case '/': precedence = 2; break;
        default: continue; // skip non-operator tokens
      }
      if (precedence <= min_precedence) { // right associative
        min_precedence = precedence;
        main_op = i;
      }
    }
  }

  return main_op;
}

void tokens_to_string(int p, int q, char *buf, int buf_size) {
  int pos = 0;
  for (int i = p; i <= q; i++) {
    // pos += snprintf(buf + pos, buf_size - pos, "[%d:'%s'] ", tokens[i].type, tokens[i].str);
    switch (tokens[i].type) {
      case TK_DECIMAL: case TK_HEX:
        pos += snprintf(buf + pos, buf_size - pos, "[%s] ", tokens[i].str);
        break;
      case '+': case '-': case '*': case '/': case '(': case ')':
        pos += snprintf(buf + pos, buf_size - pos, "[%c] ", tokens[i].type);
        break;
      default:
        pos += snprintf(buf + pos, buf_size - pos, "unkown_token_type_%d ", tokens[i].type);
        break;
    }
  }
  buf[pos] = '\0';
}
char debug_buf[65536];
word_t eval(int p, int q, bool *error) {

  //----- for debug
  if (p <= q) {
    tokens_to_string(p, q, debug_buf, sizeof(debug_buf));
    SDB_Flog("eval-info", "token[%d,%d] evaluating expression: %s", p, q, debug_buf);
  }
  //----- end debug

  bool suberror;
  if (p > q) {
    /* Bad expression */
    printf("unkown error cases p > q.\n");
    SDB_Flog("eval-info", "token[%d,%d] eval error: p>q", p, q);
    *error = true;
    return 0;
  }
  else if (p == q) {
    /* Single token.
     * For now this token should be a number.
     * Return the value of the number.
     */
    if (tokens[p].type == TK_DECIMAL) {
      return atoi(tokens[p].str);
    } else if (tokens[p].type == TK_HEX) {
      return strtol(tokens[p].str, NULL, 16);
    } else {
      *error = true;
      return 0;
    }
  }
  else if (check_parentheses(p, q, &suberror) == true) {
    /* The expression is surrounded by a matched pair of parentheses.
     * If that is the case, just throw away the parentheses.
     */
    return eval(p + 1, q - 1, error);
  }
  else {
    if (suberror == true) {
      *error = true;
      return 0;
    }
    int op = find_main_operator(p, q); //the position of the main operator 
    if (op == -1) {
      printf("no operator found in expression.\n");
      *error = true;
      return 0;
    }
    int val1 = eval(p, op - 1,&suberror);
    if (suberror == true) {
      *error = true;
      return 0;
    }
    int val2 = eval(op + 1, q,&suberror);
    if (suberror == true) {
      *error = true;
      return 0;
    }

    *error = false;
    switch (tokens[op].type) {
      case '+': return val1 + val2;
      case '-': return val1 - val2;
      case '*': return val1 * val2;
      case '/': 
        if (val2 == 0) {
          printf("division by zero.\n");
          *error = true;
          return 0; 
        }
        return val1 / val2;
      default: 
        printf("unkown operator at position %d\n", op);
        *error = true;
        return 0;
    }
  }
}

word_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }

  /* TODO: Insert codes to evaluate the expression. */
  bool error;
  word_t result = eval(0, nr_token - 1, &error);
  if (error) {
    *success = false;
    printf("failed to evaluate expression.\n");
  } else {
    *success = true;
  }
  return result;
}

void test_expr() {
  int runs =0, passed = 0, failed = 0;
  word_t result;
  bool success;
  char e[65536];
  char c;
  FILE *fd = fopen("/home/liu/ysyx-workbench/nemu/tools/gen-expr/input", "r");
  assert(fd != NULL);
  while ((c=getc(fd)) != EOF) {
    if (c == '&') {
      assert(fscanf(fd, "%u|%[^\n]\n", &result, e)==2);
      success = false;
      word_t eval_result = expr(e, &success);
      runs++;
      if (success) {
        if (eval_result != result) {
          printf("\trun %d failed\n", runs);
          failed++;
        } else {
          printf("\trun %d passed\n", runs);
          passed++;
        }
      } else {
        printf("\trun %d error\n", runs);
        failed++;
      }
    }
  
  }
  printf("test end, run %d, pass: %d, failed: %d\n",runs,passed,failed);
}