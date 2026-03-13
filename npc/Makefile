
# ----- prepare variables -----
TOPNAME = top
INC_PATH ?=

#  verilator-related
ifeq ($(VERILATOR_ROOT),)
  VERILATOR = verilator
else
  export VERILATOR_ROOT
  VERILATOR = $(VERILATOR_ROOT)/bin/verilator
endif
VERILATOR_CFLAGS += -MMD --build -cc  \
				-O3 --x-assign fast --x-initial fast --noassert

INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""

#  build directory
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

# nvboard-related
NXDC_FILES = constr/top.nxdc
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)

# project source
VSRCS = $(shell find $(abspath ./vsrc) -name "*.v")
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")
CSRCS += $(SRC_AUTO_BIND)


# ----- necessary preparation -----
# detect spaces in path
ifneq ($(words $(CURDIR)),1)
  $(error Unsupported: GNU Make cannot build in directories containing spaces, build elsewhere: '$(CURDIR)')
endif
# prepare build directory
$(shell mkdir -p $(BUILD_DIR))


include $(NVBOARD_HOME)/scripts/nvboard.mk

# ----- rules -----
# commands
default:
	@echo "-- NOT SUPPORTED --------"

all:$(BIN)
	@echo "-- READY FOR SIMULATION --------"


sim: $(BIN)
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@$^

clean:
	rm -rf $(BUILD_DIR)

.PHONY: default all clean run
# file generation
$(SRC_AUTO_BIND): $(NXDC_FILES)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

$(BIN): $(VSRCS) $(CSRCS) $(NVBOARD_ARCHIVE)
	@rm -rf $(OBJ_DIR)
	$(VERILATOR) $(VERILATOR_CFLAGS) \
		--top-module $(TOPNAME) $^ \
		$(addprefix -CFLAGS , $(CXXFLAGS)) $(addprefix -LDFLAGS , $(LDFLAGS)) \
		--Mdir $(OBJ_DIR) --exe -o $(abspath $(BIN))


include ../Makefile
