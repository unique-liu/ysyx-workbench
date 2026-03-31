#include <verilated.h>
#include "verilated_fst_c.h"
#include <stdio.h>
#include <stdlib.h>
#include <assert.h>
// Include model header, generated from Verilating "top.v"
#include "VCPUtop.h"
#include <fstream>
#include <string>
#include <cstring>
#include <signal.h>
#include <elf.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/stat.h>

#define MEM_SIZE_BYTES (64 * 1024 * 1024)
#define MEM_BASE 0x80000000
#define MAX_TIME 100000
uint8_t mem[MEM_SIZE_BYTES];

int halt = 0;
int error = 0;

void sigint_handler(int signum) {
    printf("halt with interupt\n");
    halt = 1;  // 设置标志，不直接关闭波形
    error = 1; // 设置错误标志，表示是被中断信号终止的
}

bool load_program_elf(const std::string& filename) {
    int fd = open(filename.c_str(), O_RDONLY);
    if (fd < 0) {
        fprintf(stderr, "Error: Cannot open ELF file: %s\n", filename.c_str());
        return false;
    }

    struct stat st;
    fstat(fd, &st);
    uint8_t* file_data = (uint8_t*)mmap(NULL, st.st_size, PROT_READ, MAP_PRIVATE, fd, 0);
    close(fd);
    if (file_data == MAP_FAILED) {
        fprintf(stderr, "Error: mmap failed for ELF file\n");
        return false;
    }

    // 检查 ELF 头
    Elf32_Ehdr* ehdr = (Elf32_Ehdr*)file_data;
    if (ehdr->e_type != ET_EXEC) {
        fprintf(stderr, "Error: File is not an executable ELF (type %d)\n", ehdr->e_type);
        munmap(file_data, st.st_size);
        return false;
    }

    // 遍历程序头表
    Elf32_Phdr* phdr = (Elf32_Phdr*)(file_data + ehdr->e_phoff);
    bool any_loaded = false;
    for (int i = 0; i < ehdr->e_phnum; i++) {
        if (phdr[i].p_type == PT_LOAD) {
            uint32_t vaddr = phdr[i].p_vaddr;
            uint32_t memsz = phdr[i].p_memsz;
            uint32_t filesz = phdr[i].p_filesz;
            uint32_t offset = phdr[i].p_offset;

            // 检查段是否在模拟内存范围内
            if (vaddr < MEM_BASE || (vaddr + memsz) > (MEM_BASE + MEM_SIZE_BYTES)) {
                fprintf(stderr, "Warning: Segment at 0x%08x (size 0x%x) out of memory range, skipping\n",
                        vaddr, memsz);
                continue;
            }

            // 计算在 mem 数组中的偏移（字节）
            uint32_t array_offset = vaddr - MEM_BASE;
            // 复制初始化数据
            memcpy(mem + array_offset, file_data + offset, filesz);
            // 如果 memsz > filesz，剩余部分清零（BSS）
            if (memsz > filesz) {
                memset(mem + array_offset + filesz, 0, memsz - filesz);
            }
            printf("Loaded segment at 0x%08x (size %d bytes)\n", vaddr, memsz);
            any_loaded = true;
        }
    }

    munmap(file_data, st.st_size);
    if (!any_loaded) {
        fprintf(stderr, "Error: No loadable segments found in ELF\n");
        return false;
    }
    return true;
}
int main(int argc, char** argv) {
    signal(SIGINT, sigint_handler);
    // 解析命令行参数：假设最后一个参数是程序文件路径
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <program_file>\n", argv[0]);
        return 1;
    }
    std::string program_file = argv[1];  // 获取程序文件路径

    // Construct a VerilatedContext to hold simulation time, etc.
    VerilatedContext* const contextp = new VerilatedContext;
    VerilatedFstC* tfp = new VerilatedFstC;
    // Pass arguments so Verilated code can see them, e.g. $value$plusargs
    // This needs to be called before you create any model
    contextp->commandArgs(argc, argv);
    
    
    // Construct the Verilated model, from Vtop.h generated from Verilating "top.v"
    VCPUtop* const top = new VCPUtop{contextp};
    int time = 0;


    Verilated::traceEverOn(true);
    top->trace(tfp, 99);
    tfp->open("dump.fst");

    if (!load_program_elf(program_file)) {
        return 1;
    }
    for (int i = 0; i < 10; i++) {
        
        top->reset = 1; top->clock = 0; top->eval();tfp->dump(time);time++;
        top->clock = 1; top->eval();tfp->dump(time);time++;
    }
    top->reset = 0;
    while (!halt && (time < MAX_TIME)) {
        top->clock = 0; top->eval();tfp->dump(time);time++;
        top->clock = 1; top->eval();tfp->dump(time);time++;
        
    }

    // Final model cleanup
    tfp->close();  
    top->final();

    // Destroy model
    delete top;

    // Return good completion status
    if (error) {
        fprintf(stderr, "Simulation finished with errors.\n");
        return 1;
    }else if(time >= MAX_TIME){
        fprintf(stderr, "Simulation finished with timeout.\n");
        return 1;
    }else{
        fprintf(stdout, "Simulation finished successfully.\n");
        return 0;
    }
}
