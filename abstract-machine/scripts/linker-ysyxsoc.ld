ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }

MEMORY {
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  flash : ORIGIN = 0x30000000, LENGTH = 0x10000000
}

SECTIONS {
  . = ORIGIN(flash);
  .text : {
    *(entry)
    *(.text*)
  } > flash AT> flash : text
  etext = .;
  _etext = .;
  .rodata : {
    *(.rodata*)
  } > flash AT> flash

  _data_ma_start = .;
  .data : {
    _data_sa_start = .;
    *(.data*)
    *(.sdata*)
    _data_sa_end = .;
  } > sram AT> flash : data
  _data_ma_end = _data_ma_start + SIZEOF(.data);
  edata = .;
  _data = .;
  
  .bss (NOLOAD) : {
	_bss_start = .;
    _bss_sa_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    _bss_sa_end = .;
  } > sram
  

}
