#ifndef __MYDEBUG_H__
#define __MYDEBUG_H__

#include <common.h>
#include <stdio.h>
#include <utils.h>

#define USE_FLOG
#ifdef USE_FLOG
#define Flog(name,format, ...)\
    log_write(ANSI_FMT("[%s:%d %s][%s] " format,ANSI_FG_BLUE) "\n",__FILE__, __LINE__, __func__, name, ## __VA_ARGS__)
#else
#define Flog(name,format, ...)
#endif

// #define USE_SDB_Flog
#ifdef USE_SDB_Flog
#define SDB_Flog Flog
#else
#define SDB_Flog(name,format, ...)
#endif



#endif
