#ifndef __COMMON_H__
#define __COMMON_H__

#include <stdint.h>
#include <config.h>
#include <debug.h>

typedef uint32_t word_t;

#ifdef CONFIG_USE_SOC
#define VCPUtop VysyxSoCFull
#define INCLUDE_TOP "VysyxSoCFull.h"
#else
#define VCPUtop VCPUtop
#define INCLUDE_TOP "VCPUtop.h"
#endif

#endif