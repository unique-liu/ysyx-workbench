#ifndef INIT_H
#define INIT_H
#include <common.h>
// bool load_program_elf(const std::string& filename);
void init_fst();
int init_all(int argc, char** argv);
int finish_all();
#endif