ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }

MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
}

SECTIONS {
  . = ORIGIN(mrom);
  .text : {
    *(entry)
    *(.text*)
  } > mrom AT> mrom : text
  etext = .;
  _etext = .;
  .rodata : {
    *(.rodata*)
  } > mrom AT> mrom

  _data_ma_start = .;
  .data : {
    _data_sa_start = .;
    *(.data*)
    *(.sdata*)
    _data_sa_end = .;
  } > sram AT> mrom : data
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
