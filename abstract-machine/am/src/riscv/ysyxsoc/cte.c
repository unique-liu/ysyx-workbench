#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  // print_context(c);
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case 8: case 9: case 11: ev.event = EVENT_YIELD; c->mepc += 4;break;
      default: ev.event = EVENT_ERROR; break;
    }
    // print_context(c);
    c = user_handler(ev, c);
    assert(c != NULL);
  }
  // printf("ready to return to ctx:\n");
  // print_context(c);
  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));

  // register event handler
  user_handler = handler;

  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context *ctx = (Context *)(kstack.end - sizeof(Context));
  ctx->mepc = (uintptr_t)entry;
  ctx->mstatus = 0x1800; // MPP = 11 (M-mode)
  ctx->gpr[2] = (uintptr_t)ctx; // sp
  ctx->gpr[10] = (uintptr_t)arg; // a0
  return ctx;
}

void yield() {
#ifdef __riscv_e
  asm volatile("li a5, -1; ecall");
#else
  asm volatile("li a7, -1; ecall");
#endif
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}
