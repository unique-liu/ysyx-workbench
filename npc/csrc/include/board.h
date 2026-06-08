#ifndef BOARD_H
#define BOARD_H
#include <common.h>

#define I(key) {KEY_ ##key, AM_ ##key}

#define KEY_A	0x1C
#define KEY_B	0x32
#define KEY_C	0x21
#define KEY_D	0x23
#define KEY_E	0x24
#define KEY_F	0x2B
#define KEY_G	0x34
#define KEY_H	0x33
#define KEY_I	0x43
#define KEY_J	0x3B
#define KEY_K	0x42
#define KEY_L	0x4B
#define KEY_M	0x3A
#define KEY_N	0x31
#define KEY_O	0x44
#define KEY_P	0x4D
#define KEY_Q	0x15
#define KEY_R	0x2D
#define KEY_S	0x1B
#define KEY_T	0x2C
#define KEY_U	0x3C
#define KEY_V	0x2A
#define KEY_W	0x1D
#define KEY_X	0x22
#define KEY_Y	0x35
#define KEY_Z	0x1A
#define KEY_0	0x45
#define KEY_1	0x16
#define KEY_2	0x1E
#define KEY_3	0x26
#define KEY_4	0x25
#define KEY_5	0x2E
#define KEY_6	0x36
#define KEY_7	0x3D
#define KEY_8	0x3E
#define KEY_9	0x46
#define KEY_GRAVE	0x0E
#define KEY_MINUS 0x4E
#define KEY_EQUALS 0x55
#define KEY_BACKSLASH 0x5D
#define KEY_BACKSPACE 0x66
#define KEY_SPACE 0x29
#define KEY_TAB	0x0D
#define KEY_CAPSLOCK 0x58
#define KEY_LSHIFT	0x12
#define KEY_LCTRL	0x14
// #define KEY_LGUI	0xE0,0x1F
#define KEY_LALT	0x11
#define KEY_RSHIFT 0x59
// #define KEY_RCTRL 0xE0,0x14
// #define KEY_RGUI	0xE0,0x27
// #define KEY_RALT	0xE0,0x11
// #define KEY_APPLICATION 0xE0,0x2F
#define KEY_ENTER	0x5A
#define KEY_ESCAPE	0x76
#define KEY_F1	0x5
#define KEY_F2	0x6
#define KEY_F3	0x4
#define KEY_F4	0x0C
#define KEY_F5	0x3
#define KEY_F6	0x0B
#define KEY_F7	0x83
#define KEY_F8	0x0A
#define KEY_F9	0x1
#define KEY_F10 0x9
#define KEY_F11 0x78
#define KEY_F12 0x7
// #define KEY_PRINTSCREEN 0xE0,0x12
#define KEY_SCROLLLOCK	0x7E
#define KEY_LEFTBRACKET	0x54
// #define KEY_INSERT	0xE0,0x70
// #define KEY_HOME	0xE0,0x6C
// #define KEY_PAGEUP	0xE0,0x7D
// #define KEY_DELETE	0xE0,0x71
// #define KEY_END	0xE0,0x69
// #define KEY_PAGEDOWN	0xE0,0x7A
// #define KEY_UP	0xE0,0x75
// #define KEY_LEFT	0xE0,0x6B
// #define KEY_DOWN	0xE0,0x72
// #define KEY_RIGHT	0xE0,0x74
#define KEY_RIGHTBRACKET 0x5B
#define KEY_SEMICOLON	0x4C
#define KEY_APOSTROPHE	0x52
#define KEY_COMMA	0x41
#define KEY_PERIOD	0x49
#define KEY_SLASH	0x4A
//extend
#define KEY_LGUI	    0x1F
#define KEY_RCTRL       0x14
#define KEY_RGUI	    0x27
#define KEY_RALT	    0x11
#define KEY_APPLICATION 0x2F
#define KEY_PRINTSCREEN 0x12
#define KEY_INSERT	    0x70
#define KEY_HOME	    0x6C
#define KEY_PAGEUP	    0x7D
#define KEY_DELETE	    0x71
#define KEY_END	        0x69
#define KEY_PAGEDOWN	0x7A
#define KEY_UP	        0x75
#define KEY_LEFT	    0x6B
#define KEY_DOWN	    0x72
#define KEY_RIGHT	    0x74

// for am
#define AM_KEYS(_) \
  _(ESCAPE) _(F1) _(F2) _(F3) _(F4) _(F5) _(F6) _(F7) _(F8) _(F9) _(F10) _(F11) _(F12) \
  _(GRAVE) _(1) _(2) _(3) _(4) _(5) _(6) _(7) _(8) _(9) _(0) _(MINUS) _(EQUALS) _(BACKSPACE) \
  _(TAB) _(Q) _(W) _(E) _(R) _(T) _(Y) _(U) _(I) _(O) _(P) _(LEFTBRACKET) _(RIGHTBRACKET) _(BACKSLASH) \
  _(CAPSLOCK) _(A) _(S) _(D) _(F) _(G) _(H) _(J) _(K) _(L) _(SEMICOLON) _(APOSTROPHE) _(ENTER) \
  _(LSHIFT) _(Z) _(X) _(C) _(V) _(B) _(N) _(M) _(COMMA) _(PERIOD) _(SLASH) _(RSHIFT) \
  _(LCTRL) _(APPLICATION) _(LALT) _(SPACE) _(RALT) _(RCTRL) \
  _(UP) _(DOWN) _(LEFT) _(RIGHT) _(INSERT) _(DELETE) _(HOME) _(END) _(PAGEUP) _(PAGEDOWN)

#define AM_KEY_NAMES(key) AM_##key,
enum {
  AM_KEY_NONE = 0,
  AM_KEYS(AM_KEY_NAMES)
};



typedef struct {
  uint8_t keycode;
  uint8_t amcode;
} keyitem_t;

void init_board();
void update_board();
void finish_board();

#endif