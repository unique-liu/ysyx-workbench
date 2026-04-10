
#ifndef DEBUG_H
#define DEBUG_H

#include <assert.h>
#include <config.h>
#include <stdio.h>


#define LOG_PATH_T "logs/log-trace.txt"
#define T log_trace

#define FMT_WORD "0x%08" PRIx32

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

#define panic(...) do {\
    DEBUG_PRINT(error,T,__VA_ARGS__);\
    assert(0);\
    } while (0)
#define Assert(cond,...) do{\
    if (!(cond)) {\
        DEBUG_PRINT(assert,T,__VA_ARGS__);\
        assert(0);\
    }\
    }while(0)

#define Log(...) do {\
    DEBUG_PRINT(info,T,__VA_ARGS__);\
} while (0)


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