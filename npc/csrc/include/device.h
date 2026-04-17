#ifndef DEVICE_H
#define DEVICE_H
#include <stdint.h>

#define DEVICE_NUM 2

typedef struct {
  const char *name;
  uint32_t addr;
  uint32_t len;
} mmio_map;
extern const mmio_map device_map[DEVICE_NUM];
int in_device(int addr);
int device_read(int device_id, int addr, int len);
int device_write(int device_id, int addr, int len, int wdata, char wmask);

#endif