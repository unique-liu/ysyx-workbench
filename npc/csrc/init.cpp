#include <init.h>
#include <unistd.h>
#include <mem.h>
#include <signal.h>
#include <exec.h>
#include <trace.h>
#include <sdb.h>
#include <difftest.h>
#include <getopt.h>
#include <elf.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <cstring>
#include <shoot.h>
#include <isa.h>
#include <board.h>
#include <performance.h>

static char *log_file = NULL;
static char *diff_so_file = NULL;
static char *img_file = NULL;
static char *elf_file = NULL;
static char *mrom_file = NULL;
static char *flash_file = NULL;
static int difftest_port = 1234;

DEBUG_DECLARE();
void init_disasm();

static int parse_args(int argc, char *argv[]) {
  const struct option table[] = {
    {"batch"    , no_argument      , NULL, 'b'},
    {"log"      , required_argument, NULL, 'l'},
    {"diff"     , required_argument, NULL, 'd'},
    {"port"     , required_argument, NULL, 'p'},
    {"help"     , no_argument      , NULL, 'h'},
    {"elf"      , required_argument, NULL,  'e' },
    {"trace"    , required_argument, NULL,  't' },
    {"trace_clock", required_argument, NULL, 'c'},
    {"diff_on"  , required_argument, NULL,  'D' },
    {"time"     , required_argument, NULL,  'T' },
    {"mrom",      required_argument, NULL,  'm' },
    {"flash",      required_argument, NULL,  'f' },
    {"report",      required_argument, NULL,  'r' },
    {0          , 0                , NULL,  0 },
  };
  int o;
  while ( (o = getopt_long(argc, argv, "-bhl:d:p:e:t:c:D:T:m:f:r:", table, NULL)) != -1) {
    switch (o) {
      case 'b': sdb_set_batch_mode(); break;
      case 'p': sscanf(optarg, "%d", &difftest_port); break;
      case 'l': log_file = optarg; break;
      case 'd': diff_so_file = optarg; break;
      case 'e': elf_file = optarg; break;
      case 't': sdb_set_trace_mode(optarg); break;
      case 'c': sscanf(optarg, "%lld", &npc_state.trace_clock); break;
      case 'D': sdb_set_difftest_mode(optarg); break;
      case 'T': sscanf(optarg, "%lld", &npc_state.time_limit); break;
      case 'm': mrom_file = optarg; break;
      case 'f': flash_file = optarg; break;
      case 'r': sdb_set_report_mode(optarg); break;
      case 1: img_file = optarg; return 0;
      default:
        printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
        printf("\t-b,--batch              run with batch mode\n");
        printf("\t-l,--log=FILE           output log to FILE\n");
        printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
        printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
        printf("\t-e,--elf=ELF            load ELF file for debugging\n");
        printf("\t-t,--trace=MODE         set trace mode\n");
        printf("\t-c,--trace_clock=CLOCK  set trace clock\n");
        printf("\t-D,--diff_on=MODE       set difftest mode\n");
        printf("\t-T,--time=TIME          set time limit\n");
        printf("\t-m,--mrom=FILE          load mrom file\n");
        printf("\t-f,--flash=FILE         load flash file\n");
        printf("\t-r,--report=MODE        set report mode\n");
        exit(0);
    }
  }
  return 0;
}

static long load_img() {
  if (img_file == NULL) {
    Log("No image is given. Use the default build-in image.");
    return 0;
  }

  FILE *fp = fopen(img_file, "rb");
  Assert(fp, "Can not open '%s'\n", img_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The image is %s, size = %ld\n", img_file, size);

  fseek(fp, 0, SEEK_SET);
  int ret = fread(guest_to_host(RESET_VECTOR), size, 1, fp);
  assert(ret == 1);

  fclose(fp);
  return size;
}

static long load_mrom() {
  if (mrom_file == NULL) {
    Log("no mrom file given, skip");
    return 0;
  }

  FILE *fp = fopen(mrom_file, "rb");
  Assert(fp, "Can not open '%s'\n", mrom_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The mrom is %s, size = %ld\n", mrom_file, size);
  if (size >= CONFIG_MROM_SIZE) {
    panic("mrom file size %ld exceeds mrom size %d", size, CONFIG_MROM_SIZE);
  }

  fseek(fp, 0, SEEK_SET);
  int ret = fread(mrom, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
  return size;
}

static long load_flash() {
  if (flash_file == NULL) {
    Log("no flash file given, skip");
    return 0;
  }

  FILE *fp = fopen(flash_file, "rb");
  Assert(fp, "Can not open '%s'\n", flash_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The flash is %s, size = %ld\n", flash_file, size);
  if (size >= CONFIG_FLASH_SIZE) {
    panic("flash file size %ld exceeds flash size %d", size, CONFIG_FLASH_SIZE);
  }

  fseek(fp, 0, SEEK_SET);
  int ret = fread(flash, size, 1, fp);
  assert(ret == 1);

  fclose(fp);
  return size;
}

static long load_elf() {
  if (elf_file == NULL) {
    Log("No ELF file is given. Skip loading ELF.");
    return 0;
  }

  FILE *fp = fopen(elf_file, "rb");
  Assert(fp, "Can not open '%s'\n", elf_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The ELF file is %s, size = %ld\n", elf_file, size);

  fseek(fp, 0, SEEK_SET);
  int ret = init_function_table(fp);
  assert(ret >= 0);
  Log("Loaded %d functions from ELF file.", ret);

  fclose(fp);
  return size;
}


void sigint_handler(int signum) {
    printf("halt with interupt\n");
    npc_state.type = NPC_WAITING;
}

void init_fst(){
    #ifdef CONFIG_FST
    tfp = new VerilatedFstC;
    top->trace(tfp, 99);
    tfp->open("logs/dump.fst");
    #endif
}

void init_verilator(int argc, char** argv) {
    contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    top = new VCPUtop{contextp};
    Verilated::traceEverOn(true);
    
    if (npc_state.trace_on == TRACE_ON) {
      init_fst();
    }
    
}

void init_npc_state(){
    cpu.pc = INIT_PC;

    npc_state.type = NPC_WAITING;
    npc_state.halt_pc = INIT_PC;
    npc_state.halt_ret = 0;
    npc_state.real_time = 0;
    npc_state.inst_count = 0;
    npc_state.time = 0;
    npc_state.inst_submit = 0;
    npc_state.trace_on = TRACE_OFF;
    npc_state.difftest_on = DIFF_OFF;
    npc_state.time_limit = MAX_TIME;
}

int init_all(int argc, char** argv) {
    printf("start to init npc...\n");
    init_npc_state();

    DEBUG_INIT();
    Log("debug has been inited\n");

    // 解析命令行参数
    Log("start to parse arguments\n");
    parse_args(argc, argv);
    Log("parse arguments: log_file = %s, diff_so_file = %s, img_file = %s, elf_file = %s, difftest_port = %d, mrom_file = %s, flash_file = %s\n",
        log_file ? log_file : "NULL", diff_so_file ? diff_so_file : "NULL", img_file ? img_file : "NULL", elf_file ? elf_file : "NULL", difftest_port, mrom_file ? mrom_file : "NULL", flash_file ? flash_file : "NULL");

    // 注册 SIGINT 信号处理函数
    signal(SIGINT, sigint_handler);


    // load mrom file if needed
    Log("start to load mrom\n");
    long mrom_size = load_mrom();
    Log("Loaded mrom file: %s, size: %ld\n", mrom_file ? mrom_file : "NULL", mrom_size);

    // load flash file if needed
    Log("start to load flash\n");
    long flash_size = load_flash();
    Log("Loaded flash file: %s, size: %ld\n", flash_file ? flash_file : "NULL", flash_size);

    /* Load the image to memory. This will overwrite the built-in image. */
    Log("start to load image\n");
    long img_size = load_img();
    Log("Loaded image file: %s, size: %ld\n", img_file ? img_file : "NULL", img_size);

    /* Load ELF file for debugging. */
    Log("start to load ELF file\n");
    load_elf();
    Log("Loaded ELF file: %s\n", elf_file ? elf_file : "NULL");

    #ifdef CONFIG_DIFFTEST
    if (npc_state.difftest_on == DIFF_ON) {
      Log("start to init difftest\n");
      init_difftest(diff_so_file, img_size, difftest_port);
      Log("Initialized difftest with reference: %s, image size: %ld, port: %d\n", diff_so_file ? diff_so_file : "NULL", img_size, difftest_port);
    }else {
      Log("DiffTest is not enabled by arguments, skip initializing difftest.\n");
    }
    #else
    Log("DiffTest is not enabled by CONFIG_DIFFTEST.\n");
    #endif

    // 初始化 sdb
    init_sdb();

    // 初始化反汇编
    init_disasm();

    // 初始化 Verilator
    init_verilator(argc, argv);

    // 初始化板级支持
    init_board();

    // 初始化快照
    shoot_init();

    return 0;
}

int finish_all() {
    int wake_shoot = 0;
    int ret = 0;
    switch (npc_state.type) {
      case NPC_HALT:
          if (npc_state.halt_ret) {
            printf("\033[31m[FAILED] Hit Bad Trap\033[0m\n");
            ret = -1;
            wake_shoot = 1;
          }else {
            printf("\033[32m[SUCCEED] Hit Good Trap\033[0m\n");
          }
          break;
      case NPC_STOP:
          printf("\033[32m[SUCCEED] Halt by interupt or command q\033[0m\n");
          wake_shoot = 1;
          break;
      case NPC_TIMEOUT:
          printf("\033[31m[FAILED] Halt by timeout\033[0m\n");
          ret = -1;
          wake_shoot = 1;
          break;
      case NPC_ERROR:
          #ifdef CONFIG_ITRACE
          iringbuf_print();
          #endif
          printf("\033[31m[FAILED] Halt with error at pc = " FMT_WORD "\033[0m\n", npc_state.halt_pc);
          ret = -1;
          wake_shoot = 1;
          break;
      default:
          #ifdef CONFIG_ITRACE
          iringbuf_print();
          #endif
          printf("\033[31m[UNKNOWN] Unknown halt reason\033[0m\n");
          ret = -1;
    }
    long long cycle = npc_state.time / 2;
    printf("\033[34mSimulation Info: time: %lld us, cycle: %lld, inst: %lld, CPms: %lld, CPI: %.2f\033[0m\n",
      npc_state.real_time, cycle, npc_state.inst_count,
      npc_state.real_time / 1000 == 0 ? 0 : cycle / (npc_state.real_time / 1000),
      (float)npc_state.inst_count == 0 ? 0 : cycle / (float)npc_state.inst_count);

    if (wake_shoot) {//hit bad trap
      shoot_weakup();
    }else {
      shoot_clear();
    }

    #ifdef CONFIG_FST
    if (npc_state.trace_on==TRACE_ON) {
      tfp->close();
      delete tfp;
    }
    #endif
    if (i_am_child == 0) {
      DEBUG_PRINT(finish, T, "main(pid:%d) finish all with ret %d\n", getpid(), ret);
      printf("main(pid:%d) finish all with ret %d\n", getpid(), ret);
      finish_board();
      top->final();
      delete top;
      delete contextp;
      if (npc_state.report_on == REPORT_ON) {
        report_performance();
      }
    }else {
      DEBUG_PRINT(finish, T, "child process(pid:%d) finish all with ret %d\n", getpid(), ret);
      printf("child process(pid:%d) finish all with ret %d\n", getpid(), ret);
    }
    
    DEBUG_END();

    return ret;
}