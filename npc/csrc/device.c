#include <device.h>
#include <debug.h>
#include <exec.h>
#include <sys/time.h>
const mmio_map soc_device_map[SOC_DEVICE_NUM] = {
  {"clint", 0x02000000, 0xffff},//0x0200_0000~0x0200_ffff
  {"sram",0x0f000000,0x00001fff},//0x0f00_0000~0x0fff_ffff in fact but we only use 8KB for now:0x0f00_0000~0x0f00_1fff
  {"uart", 0x10000000, 0xfff},//0x1000_0000~0x1000_0fff
  {"spi", 0x10001000, 0xfff},//0x1000_1000~0x1000_1fff
  {"gpio",0x10002000,0xf},//0x1000_2000~0x1000_200f
  {"ps2",0x10011000,0x7},//0x1001_1000~0x1001_1007
  {"mrom", 0x20000000, 0xfff},//0x2000_0000~0x2000_0fff
  {"vga",0x21000000,0x1fffff},//0x2100_0000~0x211f_ffff
  {"flash", 0x30000000, 0x0fffffff},//0x3000_0000~0x3fff_ffff
  {"chiplinkMMIO",0x40000000,0x3fffffff},//0x4000_0000~0x7fff_ffff
  {"psRAM",0x80000000,0x1fffffff},//0x8000_0000~0x9fff_ffff
  {"sdram",0xa0000000,0x1fffffff},//0xa000_0000~0xbfff_ffff
  {"chiplinkMEM",0xc0000000,0x3fffffff}//0xc000_0000~0xffff_ffff
  
};

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
int in_soc_device(uint32_t addr){
  for (int i = 0; i < SOC_DEVICE_NUM; i++) {
    // TRACE(debug,"addr:0x%08x %s[0x%08x,0x%08x],addr>=low:%d,addr<high=%d",addr, soc_device_map[i].name, soc_device_map[i].addr, soc_device_map[i].addr + soc_device_map[i].len, addr >= soc_device_map[i].addr, addr < soc_device_map[i].addr + soc_device_map[i].len);
    if (addr >= soc_device_map[i].addr && addr < soc_device_map[i].addr + soc_device_map[i].len) {
      return i;
    }
  }
  return -1;
}

int in_device(uint32_t addr){
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