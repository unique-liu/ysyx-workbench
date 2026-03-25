#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  panic("Not implemented");
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  panic("Not implemented");
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int i = 0,j=0;

  int num;
  char *str;
  while (1) {
    if (fmt[i] =='\0') {
      out[j] = '\0';
      return j;
    }else if (fmt[i] =='%') {
      i++;
      switch (fmt[i]) {
        case 'd':
          num=va_arg(ap,int);
          j+=strlen(numtodecimal(out+j,num));
          i++;
          break;

        case 's':
          str=va_arg(ap,char *);
          strcpy(out+j,str);
          j+=strlen(str);
          i++;
          break;

        default:
          return -1;
      }
    }else {
      out[j]=fmt[i];
      i++;
      j++;
    }
  }
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
