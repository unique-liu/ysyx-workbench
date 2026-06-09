#include <performance.h>
#include <exec.h>

static void call_inst_type(uint8_t code);
static void call_ifu_no_inst(uint8_t code);
static void call_lsu_mem_delay(uint8_t code);
static perf_item_t perf_items[] = {
    PERF("invalid", NULL),
    PERF("inst_type",call_inst_type),
    PERF("ifu_no_inst", call_ifu_no_inst),
    PERF("lsu_mem_delay", call_lsu_mem_delay),
};// 最多支持256种性能事件

void record(uint8_t type, uint8_t code) {
    // 记录性能事件, 根据type记录不同的性能事件
    if (perf_items[type].valid == 0) {
        printf("invalid performance event type: %d\n", type);
        npc_state.type = NPC_ERROR;
        return;
    }
    perf_items[type].counter++;
    if (perf_items[type].callback) {
        perf_items[type].callback(code);
    }
}

static inst_type_t inst_types[] = {
    {0,0},// 其他
    {0,0},// 计算类
    {0,0},// 分支类
    {0,0},// 存储类
    {0,0},// 加载类
    {0,0},// CSR类
    {0,0},// 控制类
};
static void call_inst_type(uint8_t code) {
    // 根据code记录指令类型事件, 如R/I/S/B/U/J等
    if (code & 0x80) {//最高位表示是新的指令
        code = code & 0x7F;
        inst_types[code].inst_count++;
    }
    inst_types[code].inst_total_cycle++;
}
static void report_inst_type() {
    const char* type_names[] = {"other", "alu", "branch", "store", "load", "csr", "control"};
    printf("----- Instruction type performance:\n");
    for (int i = 0; i < sizeof(inst_types)/sizeof(inst_types[0]); i++) {
        printf("%s: count = %lu, total_cycle = %lu, avg_cycle = %lu\n", type_names[i], inst_types[i].inst_count, inst_types[i].inst_total_cycle, inst_types[i].inst_total_cycle / (inst_types[i].inst_count ? inst_types[i].inst_count : 1));
    }
}
static inst_type_t ifu_no_inst[] = {
    {0,0},// other
    {0,0},// wait lsu mem access
    {0,0},// wait error mem access 
    {0,0},// wait memory access
};
static void call_ifu_no_inst(uint8_t code) {
    // 根据code记录指令类型事件, 如R/I/S/B/U/J等
    if (code & 0x80) {//最高位表示是新的指令
        code = code & 0x7F;
        ifu_no_inst[code].inst_count++;
    }
    ifu_no_inst[code].inst_total_cycle++;
}
static void report_ifu_no_inst() {
    uint64_t total_waste_cycle = 0;
    for (int i = 0; i < sizeof(ifu_no_inst)/sizeof(ifu_no_inst[0]); i++) {
        total_waste_cycle += ifu_no_inst[i].inst_count;
    }
    printf("----- IFU no inst performance:\n");
    printf("total %ld cycles delay for ifu no inst\n",total_waste_cycle);
    printf("using %ld cycles wait error mem access and %ld cycles wait memory access\n", ifu_no_inst[2].inst_total_cycle, ifu_no_inst[3].inst_total_cycle);
    printf("ifu axi request wait lsu %ld cycles", ifu_no_inst[1].inst_total_cycle);
}
static inst_type_t lsu_mem_delay[] = {
    {0,0},// other
    {0,0},// wait ifu mem access
    {0,0},// wait error mem access 
    {0,0},// wait memory access
};
static void call_lsu_mem_delay(uint8_t code) {
    // 根据code记录指令类型事件, 如R/I/S/B/U/J等
    if (code & 0x80) {//最高位表示是新的指令
        code = code & 0x7F;
        lsu_mem_delay[code].inst_count++;
    }
    lsu_mem_delay[code].inst_total_cycle++;
}

static void report_lsu_mem_delay() {
    uint64_t total_waste_cycle = 0;
    for (int i = 0; i < sizeof(lsu_mem_delay)/sizeof(lsu_mem_delay[0]); i++) {
        total_waste_cycle += lsu_mem_delay[i].inst_count;
    }
    printf("----- LSU mem delay performance:\n");
    printf("total %ld cycles delay for lsu mem access\n",total_waste_cycle);
    printf("using %ld cycles wait ifu mem access and %ld cycles wait memory access\n", lsu_mem_delay[1].inst_total_cycle, lsu_mem_delay[3].inst_total_cycle);
    printf("lsu axi request wait ifu %ld cycles", lsu_mem_delay[2].inst_total_cycle);
}

void report_performance() {
    report_inst_type();
    report_ifu_no_inst();
    report_lsu_mem_delay();
}

