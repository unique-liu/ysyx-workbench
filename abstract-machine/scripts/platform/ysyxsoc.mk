AM_SRCS := riscv/ysyxsoc/start.S \
           riscv/ysyxsoc/trm.c \
           riscv/ysyxsoc/ioe.c \
           riscv/ysyxsoc/timer.c \
           riscv/ysyxsoc/input.c \
           riscv/ysyxsoc/cte.c \
           riscv/ysyxsoc/trap.S \
           riscv/ysyxsoc/uart.c \
           riscv/ysyxsoc/gpu.c \
           platform/dummy/vme.c \
           platform/dummy/mpe.c \

CFLAGS    += -fdata-sections -ffunction-sections
LDSCRIPTS += $(AM_HOME)/scripts/linker-ysyxsoc.ld
LDFLAGS   += --defsym=_pmem_start=0x20000000 --defsym=_entry_offset=0x0 --defsym=_heap_start=0xa0000000 --defsym=_stack_pointer=0x0f002000 
LDFLAGS   += --gc-sections -e _start
NPCFLAGS  += -b --trace=off --trace_clock=55000000 --diff_on=off --time=100000000000

MAINARGS_MAX_LEN = 64
MAINARGS_PLACEHOLDER = the_insert-arg_rule_in_Makefile_will_insert_mainargs_here
CFLAGS += -DMAINARGS_MAX_LEN=$(MAINARGS_MAX_LEN) -DMAINARGS_PLACEHOLDER=$(MAINARGS_PLACEHOLDER)

insert-arg: image
	@python3 $(AM_HOME)/tools/insert-arg.py $(IMAGE).bin $(MAINARGS_MAX_LEN) $(MAINARGS_PLACEHOLDER) "$(mainargs)"

image: image-dep
	@$(OBJDUMP) -d $(IMAGE).elf > $(IMAGE).txt
	@echo + OBJCOPY "->" $(IMAGE_REL).bin
	@$(OBJCOPY) -S --set-section-flags .bss=alloc -O binary $(IMAGE).elf $(IMAGE).bin 
# .bss 不会再有“contents” 了，因为现在不会直接运行bin，trm会分配bss的空间并且在运行时初始化它，所以这里直接把.bss的内容去掉了


run: insert-arg
	$(MAKE) -C $(NPC_HOME) sim ARGS="$(NPCFLAGS)" BOOT=$(IMAGE).bin ELF=$(IMAGE).elf

.PHONY: insert-arg
