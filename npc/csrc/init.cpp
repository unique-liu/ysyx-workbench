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

static char *log_file = NULL;
static char *diff_so_file = NULL;
static char *img_file = NULL;
static char *elf_file = NULL;
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
    {0          , 0                , NULL,  0 },
  };
  int o;
  while ( (o = getopt_long(argc, argv, "-bhl:d:p:e:t:", table, NULL)) != -1) {
    switch (o) {
      case 'b': sdb_set_batch_mode(); break;
      case 'p': sscanf(optarg, "%d", &difftest_port); break;
      case 'l': log_file = optarg; break;
      case 'd': diff_so_file = optarg; break;
      case 'e': elf_file = optarg; break;
      case 't': sdb_set_trace_mode(optarg); break;
      case 1: img_file = optarg; return 0;
      default:
        printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
        printf("\t-b,--batch              run with batch mode\n");
        printf("\t-l,--log=FILE           output log to FILE\n");
        printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
        printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
        printf("\t-e,--elf=ELF            load ELF file for debugging\n");
        printf("\t-t,--trace=MODE         set trace mode\n");
        exit(0);
    }
  }
  return 0;
}

static long load_img() {
  if (img_file == NULL) {
    panic("No image is given. Use the default build-in image.");
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

// bool load_program_elf(const std::string& filename) {
//     //loading function table for ftrace
//     if (filename.c_str() == NULL) {
//         return false;
//     }
//     FILE *fp = fopen(filename.c_str(), "rb");
//     if (!fp) {
//         return false;
//     }
//     int ret = init_function_table(fp);
//     assert(ret >= 0);
//     DEBUG_PRINT(init,T,"Loaded %d functions from ELF file.\n", ret);
//     fclose(fp);
//     //function table loading end

//     int fd = open(filename.c_str(), O_RDONLY);
//     if (fd < 0) {
//         fprintf(stderr, "Error: Cannot open ELF file: %s\n", filename.c_str());
//         return false;
//     }

//     struct stat st;
//     fstat(fd, &st);
//     //used for program loading
//     uint8_t* file_data = (uint8_t*)mmap(NULL, st.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
//     close(fd);
//     if (file_data == MAP_FAILED) {
//         fprintf(stderr, "Error: mmap failed for ELF file\n");
//         return false;
//     }

//     // 检查 ELF 头
//     Elf32_Ehdr* ehdr = (Elf32_Ehdr*)file_data;
//     if (ehdr->e_type != ET_EXEC) {
//         fprintf(stderr, "Error: File is not an executable ELF (type %d)\n", ehdr->e_type);
//         munmap(file_data, st.st_size);
//         return false;
//     }

//     // 遍历程序头表
//     Elf32_Phdr* phdr = (Elf32_Phdr*)(file_data + ehdr->e_phoff);
//     bool any_loaded = false;
//     for (int i = 0; i < ehdr->e_phnum; i++) {
//         if (phdr[i].p_type == PT_LOAD) {
//             uint32_t vaddr = phdr[i].p_vaddr;
//             uint32_t memsz = phdr[i].p_memsz;
//             uint32_t filesz = phdr[i].p_filesz;
//             uint32_t offset = phdr[i].p_offset;

//             // 检查段是否在模拟内存范围内
//             if (vaddr < PMEM_LEFT || (vaddr + memsz) > (PMEM_RIGHT)) {
//                 fprintf(stderr, "Warning: Segment at 0x%08x (size 0x%x) out of memory range, skipping\n",
//                         vaddr, memsz);
//                 continue;
//             }

//             // 计算在 mem 数组中的偏移（字节）
//             uint32_t array_offset = vaddr - PMEM_LEFT;
//             // 复制初始化数据
//             memcpy(mem + array_offset, file_data + offset, filesz);
//             // 如果 memsz > filesz，剩余部分清零（BSS）
//             if (memsz > filesz) {
//                 memset(mem + array_offset + filesz, 0, memsz - filesz);
//             }
//             printf("Loaded segment at 0x%08x (size %d bytes)\n", vaddr, memsz);
//             any_loaded = true;
//         }
//     }

//     munmap(file_data, st.st_size);
//     if (!any_loaded) {
//         fprintf(stderr, "Error: No loadable segments found in ELF\n");
//         return false;
//     }
//     return true;
// }

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

int init_all(int argc, char** argv) {
    printf("start to init npc...\n");
    npc_state.type = NPC_WAITING;
    npc_state.halt_pc = 0;
    npc_state.halt_ret = 0;
    npc_state.inst_count = 0;
    npc_state.time = 0;
    npc_state.inst_submit = 0;
    npc_state.trace_on = TRACE_OFF;

    DEBUG_INIT();
    Log("debug has been inited\n");

    // 解析命令行参数
    Log("start to parse arguments\n");
    parse_args(argc, argv);
    Log("parse arguments: log_file = %s, diff_so_file = %s, img_file = %s, elf_file = %s, difftest_port = %d\n",
        log_file ? log_file : "NULL", diff_so_file ? diff_so_file : "NULL", img_file ? img_file : "NULL", elf_file ? elf_file : "NULL", difftest_port);
    // if (argc < 2) {
    //     fprintf(stderr, "Usage: %s <program_file>\n", argv[0]);
    //     return -1;
    // }
    // std::string program_file = argv[1];  // 获取程序文件路径

    // 注册 SIGINT 信号处理函数
    signal(SIGINT, sigint_handler);

    // // 加载 ELF 文件到模拟内存，并初始化函数表
    // if (!load_program_elf(elf_file)) {
    //     return -1;
    // }
    /* Load the image to memory. This will overwrite the built-in image. */
    Log("start to load image\n");
    long img_size = load_img();
    Log("Loaded image file: %s, size: %ld\n", img_file ? img_file : "NULL", img_size);

    /* Load ELF file for debugging. */
    Log("start to load ELF file\n");
    load_elf();
    Log("Loaded ELF file: %s\n", elf_file ? elf_file : "NULL");

    Log("start to init difftest\n");
    init_difftest(diff_so_file, img_size, difftest_port);
    Log("Initialized difftest with reference: %s, image size: %ld, port: %d\n", diff_so_file ? diff_so_file : "NULL", img_size, difftest_port);

    // 初始化 sdb
    init_sdb();

    // 初始化反汇编
    init_disasm();

    init_verilator(argc, argv);

    // 初始化快照
    shoot_init();

    return 0;
}

int finish_all() {
    int weak_shoot = 0;
    int ret = 0;
    switch (npc_state.type) {
        case NPC_HALT:
            if (npc_state.halt_ret) {
              printf("\033[31m[FAILED] Hit Bad Trap\033[0m\n");
              ret = -1;
              weak_shoot = 1;
            }else {
              printf("\033[32m[SUCCEED] Hit Good Trap\033[0m\n");
            }
            
            break;
        case NPC_STOP:
            printf("[SUCCEED] Halt by interupt or command q\n");
            break;
        case NPC_TIMEOUT:
            printf("[FAILED] Halt by timeout\n");
            ret = -1;
            break;
        case NPC_ERROR:
            #ifdef CONFIG_ITRACE
            iringbuf_print();
            #endif
            printf("[FAILED] Halt with error at pc = " FMT_WORD "\n", npc_state.halt_pc);
            ret = -1;
            weak_shoot = 1;
            break;
        default:
            #ifdef CONFIG_ITRACE
            iringbuf_print();
            #endif
            printf("[UNKNOWN] Unknown halt reason\n");
            ret = -1;
    }

    if (weak_shoot) {//hit bad trap
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
      top->final();
      delete top;
      delete contextp;
    }
    DEBUG_END();

    return ret;
}