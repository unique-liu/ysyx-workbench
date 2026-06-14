#include <am.h>
#include "../riscv.h"
#define RTC_ADDR 0x0200bff8
#define CYCLE_PER_US 198 * 1000 // test in use, not fixed in fact
void __am_timer_init() {
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uptime->us = (inl(RTC_ADDR) | ((uint64_t)inl(RTC_ADDR + 4) << 32)) / CYCLE_PER_US;
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {
  rtc->second = 0;
  rtc->minute = 0;
  rtc->hour   = 0;
  rtc->day    = 0;
  rtc->month  = 0;
  rtc->year   = 1900;
}
