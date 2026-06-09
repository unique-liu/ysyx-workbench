#include "../riscv.h"
#include <am.h>
#define VGA_BASE     0x21000000
#define VGA_WIDTH    640
#define VGA_HEIGHT   480
#define VGA_SIZE     (VGA_WIDTH * VGA_HEIGHT * sizeof(uint32_t))
#define XY_TO_ADDR(x, y) (VGA_BASE + ((y) * VGA_WIDTH + (x)) * sizeof(uint32_t))
void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  cfg->present = true;
  cfg->width = VGA_WIDTH;
  cfg->height = VGA_HEIGHT;
}
void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *buf){
    if (buf->sync) {
        //nvboard vga need no sync, so just ignore it
        return;
    }else {
        for (int i = 0; i < buf->h; i++) {//row 
            for (int j = 0; j < buf->w; j++) {//column
                uint32_t pixel = ((uint32_t *)buf->pixels)[i * buf->w + j];
                outl(XY_TO_ADDR(buf->x + j, buf->y + i), pixel);
            }
        }
    }
}