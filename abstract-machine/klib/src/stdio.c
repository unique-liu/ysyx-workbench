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

// the old version of vsprintf is troubled bu va_list type incompatibility
// int deal_format(char type,char* out,char filler,int min_width, va_list* ap) {
//   int num;
//   double f;
//   char *str,*temp[30];
//   switch (type) {
//     case 'd':
//       num=va_arg(*ap,int);
//       if (min_width) {
//         return fill_n(out, numtodecimal(temp, num), filler, min_width);
//       }else {
//         return strlen(numtodecimal(out,num));
//       }
//       break;

//     case 'c':
//       out[0]=va_arg(*ap,int);
//       return 1;
//       break;

//     case 's':
//       str=va_arg(*ap,char *);
//       if (min_width) {
//         return fill_n(out, str, filler, min_width);
//       }else {
//         strcpy(out,str);
//         return strlen(str);
//       }
//       break;
    
//     case 'x':
//       num=va_arg(*ap,int);
//       if (min_width) {
//         return fill_n(out, numtohex(temp, num), filler, min_width);
//       }else {
//         return strlen(numtohex(out, num));
//       }
//       break;

//     case 'f':
//       f=va_arg(*ap,double);
//       if (min_width) {
//         return fill_n(out, doubletodecimal(temp, f), filler, min_width);
//       }else {
//         return strlen(doubletodecimal(out,f));
//       }
//       break;

//     case '%':
//       out[0] = '%';
//       return 1;
//       break;

//     default:
//       putch(type);
//       putstr(" is not supported\n");
//       assert(0);
//   }
// }

// int vsprintf(char *out, const char *fmt, va_list ap) {
//   int i = 0,j=0;
//   int min_width=0;
//   char filler=' ';
//   while (1) {
//     if (fmt[i] =='\0') {
//       out[j] = '\0';
//       return j;
//     }else if (fmt[i] =='%') {
//       i++;
//       if (fmt[i]=='0') {
//         filler='0';
//         i++;
//       }
//       while (fmt[i]>='0' && fmt[i]<='9') {
//         min_width=min_width*10+fmt[i]-'0';
//         i++;
//       }
//       j+=deal_format(fmt[i],out+j,filler,min_width,&ap);
//       min_width=0;
//       filler=' ';
//       i++;
//       // switch (fmt[i]) {
//       //   // case 
//       //   case 'd':
//       //     num=va_arg(ap,int);
//       //     j+=strlen(numtodecimal(out+j,num));
//       //     i++;
//       //     break;

//       //   case 'c':
//       //     out[j++]=va_arg(ap,int);
//       //     i++;
//       //     break;

//       //   case 's':
//       //     str=va_arg(ap,char *);
//       //     strcpy(out+j,str);
//       //     j+=strlen(str);
//       //     i++;
//       //     break;
        
//       //   case 'x':
//       //     num=va_arg(ap,int);
//       //     j+=strlen(numtohex(out+j,num));
//       //     i++;
//       //     break;

//       //   case 'f':
//       //     f=va_arg(ap,double);
//       //     j+=strlen(doubletodecimal(out+j,f));
//       //     i++;
//       //     break;

//       //   case '%':
//       //     out[j++] = '%';
//       //     i++;
//       //     break;

//       //   default:
//       //     while (fmt[i]>='0' && fmt[i]<='9') {
//       //       min_width=min_width*10+fmt[i]-'0';
//       //       i++;
//       //     }
//       //     assert(0);
//       // }
//     }else {
//       out[j]=fmt[i];
//       i++;
//       j++;
//     }
//   }
// }

int vsprintf(char *out, const char *fmt, va_list ap) {
    int i = 0, j = 0;
    int min_width = 0;
    char filler = ' ';

    // 临时变量，用于各种格式转换
    int num;
    double f;
    char *str;
    char temp[30];   // 用于存储转换后的数字字符串（原代码中 char *temp[30] 是错误的，修正为字符数组）

    while (1) {
        if (fmt[i] == '\0') {
            out[j] = '\0';
            return j;
        }
        else if (fmt[i] == '%') {
            i++;                     // 跳过 '%'
            if (fmt[i] == '0') {     // 处理填充字符 '0'
                filler = '0';
                i++;
            }
            min_width = 0;
            while (fmt[i] >= '0' && fmt[i] <= '9') {   // 解析最小宽度
                min_width = min_width * 10 + (fmt[i] - '0');
                i++;
            }
            // 此时 fmt[i] 是格式字符（如 d,c,s,x,f,%）
            char type = fmt[i];
            int len = 0;   // 本次格式转换输出的字符数

            // long = int 所以 %ld 和 %d 没有区别
            if (type == 'l') {
                i++;
                type = fmt[i];
            }

            switch (type) {
                case 'd':
                    num = va_arg(ap, int);
                    if (min_width) {
                        len = fill_n(out + j, numtodecimal(temp, num), filler, min_width);
                    } else {
                        len = strlen(numtodecimal(out + j, num));
                    }
                    break;

                case 'c':
                    out[j] = va_arg(ap, int);
                    len = 1;
                    break;

                case 's':
                    str = va_arg(ap, char *);
                    if (min_width) {
                        len = fill_n(out + j, str, filler, min_width);
                    } else {
                        strcpy(out + j, str);
                        len = strlen(str);
                    }
                    break;

                case 'x':
                    num = va_arg(ap, int);
                    if (min_width) {
                        len = fill_n(out + j, numtohex(temp, num), filler, min_width);
                    } else {
                        len = strlen(numtohex(out + j, num));
                    }
                    break;

                case 'f':
                    f = va_arg(ap, double);
                    if (min_width) {
                        len = fill_n(out + j, doubletodecimal(temp, f), filler, min_width);
                    } else {
                        len = strlen(doubletodecimal(out + j, f));
                    }
                    break;

                case '%':
                    out[j] = '%';
                    len = 1;
                    break;

                default:
                    putch(type);
                    putstr(" is not supported\n");
                    assert(0);
            }

            j += len;
            i++;                     // 跳过已经处理过的格式字符
            min_width = 0;           // 重置宽度限定符
            filler = ' ';            // 重置填充字符
        }
        else {
            // 普通字符直接复制
            out[j] = fmt[i];
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
