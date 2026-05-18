PWD = $(shell pwd)
BUILD_DIR = $(PWD)/build
CPP_DIRS = csrc
VERILOG_DIRS = $(BUILD_DIR) vsrc
VERILATOR_DIR = obj_dir
# 顶层模块名
TOP_MODULE = CPUtop
TARGET = Vtop

# Verilator 可执行文件
VERILATOR = verilator

# 收集所有.v .sv .cpp 文件
VERILOG_SRC = $(shell find $(VERILOG_DIRS) -type f \( -name "*.v" -o -name "*.sv" \) 2>/dev/null)
VERILOG_SRC_NO_DPI = $(shell find $(BUILD_DIR) -type f \( -name "*.v" -o -name "*.sv" \) 2>/dev/null)
CPP_SRC = $(shell find $(CPP_DIRS) -type f \( -name "*.c" -o -name "*.cpp" \) 2>/dev/null)

ifeq ($(strip $(VERILOG_SRC)),)
$(error No Verilog/SystemVerilog files found in $(VERILOG_DIRS))
endif

ifeq ($(strip $(CPP_SRC)),)
$(warning No C++ source files found, assuming pure Verilog simulation)
endif

# 编译选项
VCC_FLAGZ = -I$(CURDIR)/csrc/include -I$(NEMU_HOME)/tools/capstone/repo/include  
LIBS = $(CURDIR)/csrc/lib/libcapstone.so.5 -lreadline -ldl -pie
VERILATOR_FLAGS = --cc -CFLAGS "$(VCC_FLAGZ)" -LDFLAGS "$(LIBS)" --build --exe -j --top-module $(TOP_MODULE)
# 如需波形跟踪，添加：
VERILATOR_FLAGS += --trace --trace-fst
# 目标：构建可执行文件
verilator: $(TARGET)

$(TARGET): $(VERILOG_SRC) $(CPP_SRC)
	$(VERILATOR) $(VERILATOR_FLAGS) $(VERILOG_SRC) $(CPP_SRC) -o $@


verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -j6 -i runMain Elaborate --target-dir $(BUILD_DIR)


YOSYS_STA_DIR = /home/liu/yosys-sta
MY_YOSYS_DIR = $(PWD)/yosys
SDC_FILE = $(MY_YOSYS_DIR)/default.sdc
RTL_FILES = $(strip $(foreach f,$(VERILOG_SRC_NO_DPI),$(realpath $(f)))) $(MY_YOSYS_DIR)/Void.v
CLK_FREQ_MHZ = 100
CLK_PORT_NAME = clock
O = $(BUILD_DIR)/sta
yosys:
	$(MAKE) -C $(YOSYS_STA_DIR) sta \
		DESIGN=$(TOP_MODULE) \
		SDC_FILE=$(SDC_FILE) \
		CLK_FREQ_MHZ=$(CLK_FREQ_MHZ) \
		CLK_PORT_NAME=$(CLK_PORT_NAME) \
		O=$(O) \
		RTL_FILES="$(RTL_FILES)"

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

.PHONY: test verilog help reformat checkformat clean yosys

# Some convenient rules
BINARY = $(VERILATOR_DIR)/Vtop
IMG ?=
DIFF = $(NEMU_HOME)/build/riscv32-nemu-interpreter-so
ELF = $(IMG:.bin=.elf)
ARGS ?=
NPC_EXEC := $(BINARY) $(ARGS) --elf=$(ELF) --diff=$(DIFF) $(IMG)
sim-env: $(BINARY)

sim: sim-env
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	$(NPC_EXEC)

-include ../Makefile
