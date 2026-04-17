#include <device.h>
#include <debug.h>
#include <exec.h>
#include <sys/time.h>


const mmio_map device_map[DEVICE_NUM] = {
  {"serial", 0x10000000, 0x4},
  {"timer", 0xa0000048, 0x8}
};
static uint64_t time_start = 0;//us

static uint64_t get_time_internal() {
  struct timeval now;
  gettimeofday(&now, NULL);
  uint64_t us = now.tv_sec * 1000000 + now.tv_usec;
  return us;
}
uint64_t get_time() {
  if (time_start == 0) time_start = get_time_internal();
  uint64_t now = get_time_internal();
  return now - time_start;
}
int init_device() {
  // Initialize devices if needed
  // print device information
  for (int i = 0; i < DEVICE_NUM; i++) {
    TRACE(dtrace,"Device %d: %s at [0x%08x ~ 0x%08x)\n", i, device_map[i].name, device_map[i].addr, device_map[i].len+device_map[i].addr);
  } 
  return 0;
}
int in_device(int addr){
  for (int i = 0; i < DEVICE_NUM; i++) {
    if (addr >= device_map[i].addr && addr < device_map[i].addr + device_map[i].len) {
      return i;
    }
  }
  return -1;
}

int device_read(int device_id, int addr, int len) {

  switch (device_id) {
  case 0:
    // Handle serial device read
    TRACE(dtrace,"read %s is not implemented\n", device_map[device_id].name);
    npc_state.type = NPC_ERROR;
    break;
  case 1:
    // Handle timer device read
    if (addr == device_map[device_id].addr) {
      return get_time() & 0xFFFFFFFF;
    } else {
      return (get_time() >> 32) & 0xFFFFFFFF;
    } 
    break;
  default:
    // Handle error
    TRACE(dtrace,"read unkown device at 0x%08x\n", addr);
    npc_state.type = NPC_ERROR;
    break;
  }
  return 0;
}

int device_write(int device_id, int addr, int len, int wdata, char wmask) {
  switch (device_id) {
  case 0:
    // Handle serial device write
    putchar((char)wdata);
    break;
  case 1:
    // Handle timer device write
    TRACE(dtrace,"write %s is not implemented\n", device_map[device_id].name);
    npc_state.type = NPC_ERROR;
    break;
  default:
    // Handle error
    TRACE(dtrace,"write unkown device at 0x%08x\n", addr);
    npc_state.type = NPC_ERROR;
    break;
  }
  return 0;

}