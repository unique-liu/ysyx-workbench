#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  char buf[1024];
  va_list ap;
  va_start(ap, fmt);
  int len = vsprintf(buf, fmt, ap);
  va_end(ap);
  for (int i = 0; i < len; i++) {
    putch(buf[i]);
  }
  return len;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  int i = 0,j=0;
  int num;
  double f;
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

        case 'c':
          out[j++]=va_arg(ap,int);
          i++;
          break;

        case 's':
          str=va_arg(ap,char *);
          strcpy(out+j,str);
          j+=strlen(str);
          i++;
          break;
        
        case 'x':
          num=va_arg(ap,int);
          j+=strlen(numtohex(out+j,num));
          i++;
          break;

        case 'f':
          f=va_arg(ap,double);
          j+=strlen(doubletodecimal(out+j,f));
          i++;
          break;

        case '%':
          out[j++] = '%';
          i++;
          break;

        default:
          assert(0);
      }
    }else {
      out[j]=fmt[i];
      i++;
      j++;
    }
  }
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int len = vsprintf(out, fmt, ap);
  va_end(ap);
  return len;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  return vsnprintf(out, n, fmt, ap);
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
