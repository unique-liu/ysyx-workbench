# 关键路径
PWD = $(shell pwd)
# 这个是SOC的路径
SOC_HOME ?= ../ysyxSoC
# 这个是nvboard的路径
NVBOARD_HOME ?= ../nvboard   
# 这个是nemu的路径
NEMU_HOME ?= ../nemu

# build 目录
BUILD_DIR = $(PWD)/build
BUILD_CHISEL_DIR = $(BUILD_DIR)/chisel
BUILD_VERILATOR_DIR = $(BUILD_DIR)/verilator

CPP_DIRS = $(PWD)/csrc
CHISEL_DIRS = $(PWD)/src
VERILOG_NO_DPI_DIRS = $(BUILD_CHISEL_DIR)/soc
VERILOG_NPC_DIRS = $(BUILD_CHISEL_DIR)/npc vsrc
VERILOG_SOC_DIRS = $(BUILD_CHISEL_DIR)/soc vsrc
VERILOG_SOC_EXTRA_DIRS =  $(SOC_HOME)/perip
CONSTR_DIRS = $(PWD)/constr
# 顶层模块名
TOP_MODULE_SOC = ysyxSoCFull #SOC顶层模块名
TOP_MODULE_NPC = ysyx_26050152 #NPC顶层模块名
SOC_TARGET = $(BUILD_VERILATOR_DIR)/soc/Vtop
NPC_TARGET = $(BUILD_VERILATOR_DIR)/npc/Vtop

# Verilator 可执行文件
VERILATOR = verilator

# 收集所有.v .sv .cpp 文件
VERILOG_SRC_NO_DPI = $(shell find $(VERILOG_NO_DPI_DIRS) -type f \( -name "*.v" -o -name "*.sv" \) 2>/dev/null)
VERILOG_NPC_SRC = $(shell find $(VERILOG_NPC_DIRS) -type f \( -name "*.v" -o -name "*.sv" \) 2>/dev/null)
VERILOG_SOC_SRC = $(shell find $(VERILOG_SOC_DIRS) -type f \( -name "*.v" -o -name "*.sv" \) 2>/dev/null)
VERILOG_SOC_EXTRA_SRC = $(shell find $(VERILOG_SOC_EXTRA_DIRS) -type f \( -name "*.v" -o -name "*.sv" \) 2>/dev/null)
VERILOG_SOC_EXTRA_SRC += $(SOC_HOME)/build/ysyxSoCFull.v

CPP_SRC_COMMON = $(shell find $(CPP_DIRS) -type f \( -name "*.c" -o -name "*.cpp" \) ! -name "auto_bind.cpp" 2>/dev/null)
CPP_SOC_SRC = $(CPP_SRC_COMMON) $(AUTO_BIND)
CPP_NPC_SRC = $(CPP_SRC_COMMON)

ifeq ($(strip $(VERILOG_NPC_SRC)),)
$(error No Verilog/SystemVerilog files found in $(VERILOG_NPC_DIRS))
endif

ifeq ($(strip $(CPP_SRC_COMMON)),)
$(warning No C++ source files found, assuming pure Verilog simulation)
endif


# ----- verilog -> bin
# 编译选项
VCC_FLAGZ = -I$(CURDIR)/csrc/include -I$(NEMU_HOME)/tools/capstone/repo/include   -Os -march=native -mtune=native
# nvboard要求
VCC_FLAGZ += -I$(NVBOARD_HOME)/usr/include 

# 链接选项
LIBS = $(CURDIR)/csrc/lib/libcapstone.so.5 -lreadline -ldl -pie
# nvboard要求
LIBS_SOC = $(LIBS) $(NVBOARD_HOME)/build/nvboard.a $(shell sdl2-config --libs) -lSDL2_image -lSDL2_ttf

VERILATOR_FLAGS = --autoflush  --build --exe -j --timescale "1ns/1ns" --no-timing -O3 --x-assign fast --x-initial fast
# 如需波形跟踪，添加：
VERILATOR_FLAGS += --trace --trace-fst

VERILATOR_SOC_FLAGS = $(VERILATOR_FLAGS) --top-module $(TOP_MODULE_SOC) --cc -CFLAGS "$(VCC_FLAGZ) -DCONFIG_USE_SOC" -LDFLAGS "$(LIBS_SOC)"\
			 -I$(SOC_HOME)/perip/uart16550/rtl -I$(SOC_HOME)/perip/spi/rtl   --Mdir $(BUILD_VERILATOR_DIR)/soc
VERILATOR_NPC_FLAGS = $(VERILATOR_FLAGS) --top-module $(TOP_MODULE_NPC) --cc -CFLAGS "$(VCC_FLAGZ)" -LDFLAGS "$(LIBS)"\
			  --Mdir $(BUILD_VERILATOR_DIR)/npc

# 目标：构建可执行文件
verilator_npc: $(NPC_TARGET)
verilator_soc: $(SOC_TARGET)

$(SOC_TARGET): $(SOC_CPU_TOP) $(VERILOG_SOC_EXTRA_SRC) $(CPP_SOC_SRC)
	mkdir -p $(BUILD_VERILATOR_DIR)/soc
	$(VERILATOR) $(VERILATOR_SOC_FLAGS) $(SOC_CPU_TOP) $(VERILOG_SOC_EXTRA_SRC) $(CPP_SOC_SRC) -o $@

$(NPC_TARGET): $(NPC_CPU_TOP) $(CPP_NPC_SRC)
	mkdir -p $(BUILD_VERILATOR_DIR)/npc
	$(VERILATOR) $(VERILATOR_NPC_FLAGS) $(NPC_CPU_TOP) $(CPP_NPC_SRC) -o $@

# ------ Chisel -> verilog
NPC_CPU_TOP = $(BUILD_DIR)/ysyx_26050152-npc.sv
SOC_CPU_TOP = $(BUILD_DIR)/ysyx_26050152.sv
CHISEL_FILES = $(shell find $(CHISEL_DIRS) -type f \( -name "*.scala" \) 2>/dev/null)

verilog_npc: $(NPC_CPU_TOP)
verilog_soc: $(SOC_CPU_TOP)

$(NPC_CPU_TOP): $(CHISEL_FILES)
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_CHISEL_DIR)/npc
	JAVA_OPTS="-Duse_soc=false"  mill -j6 -i runMain Elaborate --target-dir $(BUILD_CHISEL_DIR)/npc
	cat $(VERILOG_NPC_SRC) > $(NPC_CPU_TOP)

$(SOC_CPU_TOP): $(CHISEL_FILES)
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_CHISEL_DIR)/soc
	JAVA_OPTS="-Duse_soc=true"  mill -j6 -i runMain Elaborate --target-dir $(BUILD_CHISEL_DIR)/soc
	cat $(VERILOG_SOC_SRC) > $(SOC_CPU_TOP)
# ------ verilog -> report

YOSYS_STA_DIR = /home/liu/yosys-sta
MY_YOSYS_DIR = $(PWD)/yosys
YOSYS_SDC_FILE = $(MY_YOSYS_DIR)/default.sdc
YOSYS_RTL_FILES = $(strip $(foreach f,$(VERILOG_SRC_NO_DPI),$(realpath $(f)))) $(MY_YOSYS_DIR)/Void.v
YOSYS_CLK_FREQ_MHZ = 10000
YOSYS_CLK_PORT_NAME = clock
O = $(MY_YOSYS_DIR)/report
yosys:
	rm -rf yosys/report/
	$(MAKE) -C $(YOSYS_STA_DIR) sta \
		DESIGN=$(TOP_MODULE_NPC) \
		SDC_FILE=$(YOSYS_SDC_FILE) \
		CLK_FREQ_MHZ=$(YOSYS_CLK_FREQ_MHZ) \
		CLK_PORT_NAME=$(YOSYS_CLK_PORT_NAME) \
		O=$(O) \
		RTL_FILES="$(YOSYS_RTL_FILES)"

# ------ nxdc -> C
AUTO_BIND = $(CPP_DIRS)/auto_bind.cpp
nvboard: $(AUTO_BIND)
$(AUTO_BIND): $(CONSTR_DIRS)/top.nxdc
	@python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $(CONSTR_DIRS)/top.nxdc $(AUTO_BIND)

# ------ 其他
check:
	@echo "SOC_HOME: $(SOC_HOME)"
	@echo "NVBOARD_HOME: $(NVBOARD_HOME)"
	@echo "NEMU_HOME: $(NEMU_HOME)"

test:
	mill -i test

help:
	mill -i runMain Elaborate --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

bsp:
	mill -i mill.bsp.BSP/install

idea:
	mill -i mill.idea.GenIdea/idea

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: test verilog verilator help reformat checkformat clean yosys



# Some convenient rules
IMG ?=
BOOT ?=
DIFF = $(NEMU_HOME)/build/riscv32-nemu-interpreter-so
ELF ?= 
ARGS ?=
SOC_EXEC := $(SOC_TARGET) $(ARGS) --elf=$(ELF) --diff=$(DIFF) --flash=$(BOOT) $(IMG)
NPC_EXEC := $(NPC_TARGET) $(ARGS) --elf=$(ELF) --diff=$(DIFF) $(IMG)

sim-env: $(NPC_TARGET)

sim_soc: $(SOC_TARGET)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(SOC_EXEC)

sim_npc: $(NPC_TARGET)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(NPC_EXEC)

sim-test: $(NPC_TARGET)
	$(NPC_TARGET) -b --trace=on --diff_on=off --mrom=/home/liu/ysyx-workbench/npc/test/ctest/char-test.bin

# ------ command for course
verilog:$(SOC_CPU_TOP)

sim-iverilog: 
	@echo not supported

sim-iverilog-netlist: 
	@echo not supported

-include ../Makefile
