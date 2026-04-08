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
int fill_n(char *out,char* src, char c, int min_width) {
  int len = strlen(src);
  if (len >= min_width) {
    strcpy(out,src);
    return len;
  }else {
    while (len<min_width) {
      out[0]=c;
      out++;
      len++;
    }
    strcpy(out,src);
    return min_width;
  }
}

int deal_format(char type,char* out,char filler,int min_width, va_list ap) {
  int num;
  double f;
  char *str,*temp[30];
  switch (type) {
    case 'd':
      num=va_arg(ap,int);
      if (min_width) {
        return fill_n(out, numtodecimal(temp, num), filler, min_width);
      }else {
        return strlen(numtodecimal(out,num));
      }
      break;

    case 'c':
      out[0]=va_arg(ap,int);
      return 1;
      break;

    case 's':
      str=va_arg(ap,char *);
      if (min_width) {
        return fill_n(out, str, filler, min_width);
      }else {
        strcpy(out,str);
        return strlen(str);
      }
      break;
    
    case 'x':
      num=va_arg(ap,int);
      if (min_width) {
        return fill_n(out, numtohex(temp, num), filler, min_width);
      }else {
        return strlen(numtohex(out, num));
      }
      break;

    case 'f':
      f=va_arg(ap,double);
      if (min_width) {
        return fill_n(out, doubletodecimal(temp, f), filler, min_width);
      }else {
        return strlen(doubletodecimal(out,f));
      }
      break;

    case '%':
      out[0] = '%';
      return 1;
      break;

    default:
      putch(type);
      putstr(" is not supported\n");
      assert(0);
  }
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  int i = 0,j=0;
  int min_width=0;
  char filler=' ';
  while (1) {
    if (fmt[i] =='\0') {
      out[j] = '\0';
      return j;
    }else if (fmt[i] =='%') {
      i++;
      if (fmt[i]=='0') {
        filler='0';
        i++;
      }
      while (fmt[i]>='0' && fmt[i]<='9') {
        min_width=min_width*10+fmt[i]-'0';
        i++;
      }
      j+=deal_format(fmt[i],out+j,filler,min_width,ap);
      min_width=0;
      filler=' ';
      i++;
      // switch (fmt[i]) {
      //   // case 
      //   case 'd':
      //     num=va_arg(ap,int);
      //     j+=strlen(numtodecimal(out+j,num));
      //     i++;
      //     break;

      //   case 'c':
      //     out[j++]=va_arg(ap,int);
      //     i++;
      //     break;

      //   case 's':
      //     str=va_arg(ap,char *);
      //     strcpy(out+j,str);
      //     j+=strlen(str);
      //     i++;
      //     break;
        
      //   case 'x':
      //     num=va_arg(ap,int);
      //     j+=strlen(numtohex(out+j,num));
      //     i++;
      //     break;

      //   case 'f':
      //     f=va_arg(ap,double);
      //     j+=strlen(doubletodecimal(out+j,f));
      //     i++;
      //     break;

      //   case '%':
      //     out[j++] = '%';
      //     i++;
      //     break;

      //   default:
      //     while (fmt[i]>='0' && fmt[i]<='9') {
      //       min_width=min_width*10+fmt[i]-'0';
      //       i++;
      //     }
      //     assert(0);
      // }
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
