#include <init.h>


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
    {0          , 0                , NULL,  0 },
  };
  int o;
  while ( (o = getopt_long(argc, argv, "-bhl:d:p:e:", table, NULL)) != -1) {
    switch (o) {
      case 'b': sdb_set_batch_mode(); break;
      case 'p': sscanf(optarg, "%d", &difftest_port); break;
      case 'l': log_file = optarg; break;
      case 'd': diff_so_file = optarg; break;
      case 'e': elf_file = optarg; break;
      case 1: img_file = optarg; return 0;
      default:
        printf("Usage: %s [OPTION...] IMAGE [args]\n\n", argv[0]);
        printf("\t-b,--batch              run with batch mode\n");
        printf("\t-l,--log=FILE           output log to FILE\n");
        printf("\t-d,--diff=REF_SO        run DiffTest with reference REF_SO\n");
        printf("\t-p,--port=PORT          run DiffTest with port PORT\n");
        printf("\t-e,--elf=ELF            load ELF file for debugging\n");
        printf("\n");
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
  Assert(fp, "Can not open '%s'", img_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The image is %s, size = %ld", img_file, size);

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
  Assert(fp, "Can not open '%s'", elf_file);

  fseek(fp, 0, SEEK_END);
  long size = ftell(fp);

  Log("The ELF file is %s, size = %ld", elf_file, size);

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

void init_verilator(int argc, char** argv) {
    contextp = new VerilatedContext;
    tfp = new VerilatedFstC;
    contextp->commandArgs(argc, argv);
    top = new VCPUtop{contextp};
    Verilated::traceEverOn(true);
    top->trace(tfp, 99);
    tfp->open("logs/dump.fst");
}

int init_all(int argc, char** argv) {
    DEBUG_INIT();

    // 解析命令行参数
    parse_args(argc, argv);
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
    long img_size = load_img();

    /* Load ELF file for debugging. */
    load_elf();

    init_difftest(diff_so_file, img_size, difftest_port);

    // 初始化 sdb
    init_sdb();

    // 初始化反汇编
    init_disasm();

    init_verilator(argc, argv);

    npc_state.type = NPC_WAITING;
    npc_state.halt_pc = 0;
    npc_state.inst_count = 0;
    npc_state.time = 0;
    return 0;
}

int finish_all() {
    tfp->close();
    top->final();
    delete top;
    delete tfp;
    delete contextp;

    int ret = 0;
    switch (npc_state.type) {
        case NPC_HALT:
            printf("[SUCCEED] Halt by instruction\n");
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
            break;
        default:
            #ifdef CONFIG_ITRACE
            iringbuf_print();
            #endif
            printf("[UNKNOWN] Unknown halt reason\n");
            ret = -1;
    }

    
    DEBUG_END();

    return ret;
}