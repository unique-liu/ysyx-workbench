ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; entry PT_LOAD;bss PT_NULL;}

MEMORY {
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  flash : ORIGIN = 0x30000000, LENGTH = 0x10000000
  psram : ORIGIN = 0x80000000, LENGTH = 0x400000
}

SECTIONS {
  . = ORIGIN(flash);
  .entry : {
    *(.entry)
  } > flash AT> flash : entry

  
  .bootloader : {
    _bootloader_sa_start = .;
    *(.bootloader)
    _bootloader_sa_end = .;
  } > psram AT> flash : text
  _bootloader_ma_start = LOADADDR(.bootloader);
  _bootloader_ma_end = _bootloader_ma_start + SIZEOF(.bootloader);

 
  .text : {
    _text_sa_start = .;
    *(.text*)
    _text_sa_end = .;
  } > psram AT> flash : text
   _text_ma_start = LOADADDR(.text);
  _text_ma_end = _text_ma_start + SIZEOF(.text);

  etext = .;
  _etext = .;
 
  .data : {
    _data_sa_start = .;
    *(.rodata*)
    *(.data*)
    *(.sdata*)
    _data_sa_end = .;
  } > psram AT> flash : data
  _data_ma_start = LOADADDR(.data);
  _data_ma_end = _data_ma_start + SIZEOF(.data);
  edata = .;
  _data = .;
  
  .bss (NOLOAD): {
	_bss_start = .;
    _bss_sa_start = .;
    *(.bss.*)
    *(.sbss*)
    *(.scommon)
    _bss_sa_end = .;
  } > psram : bss

}
