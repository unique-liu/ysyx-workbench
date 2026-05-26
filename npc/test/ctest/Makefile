
all:
	riscv64-linux-gnu-gcc -c -march=rv32i -mabi=ilp32 -ffreestanding -nostdlib -fno-builtin -O2 -o char-test.o char-test.c
	riscv64-linux-gnu-ld -melf32lriscv -Ttext=0x0 -N -o char-test.elf char-test.o
	riscv64-linux-gnu-objcopy -O binary -j .text char-test.elf char-test.bin