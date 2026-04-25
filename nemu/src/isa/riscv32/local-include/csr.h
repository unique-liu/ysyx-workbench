#ifndef __CSR_H__
#define __CSR_H__
#include <common.h>
#define CSR_PRIV_M 3
#define CSR_PRIV_S 1
#define CSR_PRIV_U 0

typedef struct{
    union {
        struct {
        uint32_t WPRI0 : 1;//0
        uint32_t SIE : 1;//1
        uint32_t WPRI1 : 1;//2
        uint32_t MIE : 1;//3
        uint32_t WPRI2 : 1;//4
        uint32_t SPIE : 1;//5
        uint32_t UBE : 1;//6
        uint32_t MPIE : 1;//7
        uint32_t SPP : 1;//8
        uint32_t VS : 2;//10:9
        uint32_t MPP : 2;//12:11
        uint32_t FS : 2;//14:13
        uint32_t XS : 2;//16:15
        uint32_t MPRV : 1;//17
        uint32_t SUM : 1;//18
        uint32_t MXR : 1;//19
        uint32_t TVM : 1;//20
        uint32_t TW : 1;//21
        uint32_t TSR : 1;//22
        uint32_t WPRI3 : 7;//30:23
        uint32_t SD : 1;//31
        };
        word_t val;
    };
} mstatus_t;

#endif