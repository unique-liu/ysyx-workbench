#ifndef DEVICE_H
#define DEVICE_H
#include <stdint.h>
#define SOC_DEVICE_NUM 13
#define DEVICE_NUM 2

typedef struct {
  const char *name;
  uint32_t addr;
  uint32_t len;
} mmio_map;
extern const mmio_map soc_device_map[SOC_DEVICE_NUM];
extern const mmio_map device_map[DEVICE_NUM];
int in_soc_device(uint32_t addr);
int in_device(uint32_t addr);
int device_read(int device_id, int addr, int len);
int device_write(int device_id, int addr, int len, int wdata, char wmask);

#endif