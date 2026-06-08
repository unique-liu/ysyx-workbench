#include <board.h>
#include <nvboard.h>
#include "VysyxSoCFull.h"
#include <exec.h>

void nvboard_bind_all_pins(VysyxSoCFull* top);

void init_board() {
    #ifdef CONFIG_BOARD
    nvboard_bind_all_pins(top);
    nvboard_init();
    #else
    printf("Board support is disabled. Please enable CONFIG_BOARD to use it.\n"); 
    #endif
}

void update_board() {
    #ifdef CONFIG_BOARD
    nvboard_update();
    #endif

}
void finish_board() {
    #ifdef CONFIG_BOARD
    nvboard_quit();
    #endif
}

static const keyitem_t transtable[] = {
    I(A), I(B), I(C), I(D), I(E), I(F), I(G), I(H), I(I), I(J), I(K), I(L), I(M), I(N), I(O), I(P), I(Q), I(R), I(S), I(T), I(U), I(V), I(W), I(X), I(Y), I(Z),
    I(0), I(1), I(2), I(3), I(4), I(5), I(6), I(7), I(8), I(9),
    I(GRAVE), I(MINUS), I(EQUALS), I(BACKSLASH), I(BACKSPACE), I(SPACE), I(TAB), I(CAPSLOCK), I(LSHIFT), I(LCTRL), I(LALT), I(RSHIFT),
    I(F1), I(F2), I(F3), I(F4), I(F5), I(F6), I(F7), I(F8), I(F9), I(F10), I(F11), I(F12),
    I(ESCAPE),I(ENTER),I(LEFTBRACKET),I(RIGHTBRACKET),I(SEMICOLON),I(APOSTROPHE),I(COMMA),I(PERIOD),I(SLASH)
};
static const keyitem_t transtable_extend[] = {
    I(HOME), I(UP), I(PAGEUP), I(LEFT), I(RIGHT), I(END), I(DOWN), I(PAGEDOWN), I(INSERT), I(DELETE), I(RCTRL), I(RALT),
};

extern "C" void key_handler(uint8_t keycode, uint8_t *data,char is_ext,char is_release) { 
  uint8_t return_data = 0;// 0 means no key is pressed
  if (is_ext) {
    for (int i = 0; i < sizeof(transtable_extend) / sizeof(keyitem_t); i++) {
      if (transtable_extend[i].keycode == keycode) {
        return_data = transtable_extend[i].amcode;
        break;
      }
    }
  } else {
    for (int i = 0; i < sizeof(transtable) / sizeof(keyitem_t); i++) {
      if (transtable[i].keycode == keycode) {
        return_data = transtable[i].amcode;
        break;
      }
    }
  }
  *data = (is_release)? return_data | 0x80 : return_data;//use the highest bit to indicate whether the key is released
  TRACE(dtrace,"keycode:0x%02x is_ext:%d is_release:%d return:%02x\n", keycode, is_ext, is_release, return_data);
}