#include "../riscv.h"
#include <am.h>
#define PS2_BASE     0x10011000
void __am_input_keybrd(AM_INPUT_KEYBRD_T *kbd) {
  uint8_t data = inb(PS2_BASE);
  kbd->keydown = data & 0x80 ? false : true; // use the highest bit to indicate whether the key is released
  kbd->keycode = data & 0x7F; // the rest 7 bits are the keycode
}
