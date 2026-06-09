#ifndef PERFORMANCE_H
#define PERFORMANCE_H
#include <common.h>

#define PERF(name,call) {0,1,#name,call}
typedef void (*perf_callback_t)(uint8_t code);
typedef struct{
    uint64_t counter;
    char valid;// 是否有效
    const char *name;
    perf_callback_t callback;// 性能事件发生时的回调函数, 可以根据code区分不同的事件
} perf_item_t;

typedef struct{
    uint64_t inst_count;// 这种指令的总执行次数
    uint64_t inst_total_cycle; //这种指令的总周期数
} inst_type_t;

void record(uint8_t type,uint8_t code);
void report_performance();
#endif // PERFORMANCE_H