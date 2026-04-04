#ifndef INIT_H
#define INIT_H
#include <stdio.h>
#include <elf.h>
#include <stdlib.h>
#include <fstream>
#include <string>
#include <cstring>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <debug.h>
#include <unistd.h>
#include <mem.h>
#include <signal.h>
#include <exec.h>
#include <debug.h>

bool load_program_elf(const std::string& filename);
int init_all(int argc, char** argv);
int finish_all();
#endif