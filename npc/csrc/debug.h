
#ifndef DEBUG_H
#define DEBUG_H

#include <assert.h>

#define LOG_PATH "log.txt"
#define DEBUG 

#ifdef DEBUG
#define DEBUG_PRINT(name,fmt, ...) do{\
    FILE *log_file = fopen(LOG_PATH, "a");\
    if (log_file) {\
        fprintf(log_file,"[%s:%d %s ]["#name"] ", __FILE__, __LINE__, __func__);\
        fprintf(log_file,fmt,## __VA_ARGS__);\
        fclose(log_file);\
    }else{\
        assert(0);\
    }\
    }while(0)

#else
#define DEBUG_PRINT(fmt, ...) do {} while (0)
#endif

#define DEBUG_ASSERT(cond,name,fmt, ...) do {\
    if (!(cond)) {\
        DEBUG_PRINT(name,fmt, ##__VA_ARGS__);\
    }\
} while (0)

#endif