
#ifndef DEBUG_H
#define DEBUG_H

#include <assert.h>
#include <config.h>

#define LOG_PATH_T "logs/log-trace.txt"
#define T log_trace


#ifdef DEBUG

#define DEBUG_DECLARE() FILE *T;
#define DEBUG_INIT() do {\
    T = fopen(LOG_PATH_T, "w");\
    if (T) {\
        fprintf(T, "Log initialized.\n");\
    } else {\
        assert(0);\
    }\
} while (0)

#define DEBUG_END() do {\
    if (T) {\
        fprintf(T, "Log ended.\n");\
        fclose(T);\
    } else {\
        assert(0);\
    }\
} while (0)

// #define DEBUG_PRINT(name,target,fmt, ...) do{\
//     extern FILE *T;\
//     fprintf(target,"[%s:%d %s %llu]["#name"] ", __FILE__, __LINE__, __func__,npc_state.time);\
//     fprintf(target,fmt,## __VA_ARGS__);\
// }while(0)
#define DEBUG_PRINT(name,target,fmt, ...) do{\
    extern FILE *T;\
    fprintf(target,"[t:%llu]["#name"] ",npc_state.time);\
    fprintf(target,fmt,## __VA_ARGS__);\
}while(0)

#else
#define DEBUG_DECLARE()
#define DEBUG_INIT() do {} while (0)
#define DEBUG_END() do {} while (0)
#define DEBUG_PRINT(fmt, ...) do {} while (0)
#endif

#define DEBUG_ASSERT(cond,name,target,fmt, ...) do {\
    if (!(cond)) {\
        DEBUG_PRINT(name,target,fmt, ##__VA_ARGS__);\
    }\
} while (0)

#endif