#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  int len = 0;
  while (s[len] != '\0') {
    len++;
  }
  return len;
}

char *strcpy(char *dst, const char *src) {
  size_t i;
  for (i = 0;src[i] != '\0'; i++)
      dst[i] = src[i];
  dst[i] = '\0';
  return dst;
}

char *strncpy(char *dst, const char *src, size_t n) {
  size_t i;
  for (i = 0; i < n && src[i] != '\0'; i++)
      dst[i] = src[i];
  for ( ; i < n; i++)
      dst[i] = '\0';
  return dst;
}

char *strcat(char *dst, const char *src) {
  size_t dest_len = strlen(dst);
  int i = 0;
  for (i = 0 ;src[i] != '\0' ; i++)
    dst[dest_len + i] = src[i];
  dst[dest_len + i] = '\0';
  return dst;
}

int strcmp(const char *s1, const char *s2) {
  size_t i = 0;
  for (i = 0; ; i++) {
    if (s1[i] != s2[i]) {
      return s1[i] - s2[i];
    }
    if (s1[i] == '\0') {
      return 0;
    }
  }
}

int strncmp(const char *s1, const char *s2, size_t n) {
  size_t i;
  for (i = 0; i < n; i++) {
    if (s1[i] != s2[i]) {
      return s1[i] - s2[i];
    }
    if (s1[i] == '\0') {
      return 0;
    }
  }
  return 0;
   
}

void *memset(void *s, int c, size_t n) {
  size_t i;
  for (i = 0; i < n; i++) {
    ((char *)s)[i] = (char)c;
  }
  return s;
}

void *memmove(void *dst, const void *src, size_t n) {
  char temp[n];
  size_t i;
  for (i = 0; i < n; i++) {
    temp[i] = ((char *)src)[i];
  }
  for (i = 0; i < n; i++) {
    ((char *)dst)[i] = temp[i];
  }
  return dst;
}

void *memcpy(void *out, const void *in, size_t n) {
  for (size_t i =0; i < n; i++) {
    ((char *)out)[i] = ((char *)in)[i];
  }
  return out;
}

int memcmp(const void *s1, const void *s2, size_t n) {
  for (size_t i = 0; i < n; i++) {
    if (((char *)s1)[i] != ((char *)s2)[i]) {
      return ((char *)s1)[i] - ((char *)s2)[i];
    }
  }
  return 0;
}

void *numtodecimal(void *dst, int num) {
  char temp[20];
  int i = 0;
  if (num == 0) {
    temp[i++] = '0';
  } else {
    if (num < 0) {
      temp[i++] = '-';
      num = -num;
    }
    while (num > 0) {
      temp[i++] = (num % 10) + '0';
      num /= 10;
    }
  }
  for (int j = 0; j < i; j++) {
    ((char *)dst)[j] = temp[i - j - 1];
  }
  ((char *)dst)[i] = '\0';
  return dst;
}

void *numtohex(void *dst, int num) {
  char temp[20];
  int i = 0;
  if (num == 0) {
    temp[i++] = '0';
  } else {
    while (num > 0) {
      int remainder = num % 16;
      if (remainder < 10) {
        temp[i++] = remainder + '0';
      } else {
        temp[i++] = remainder - 10 + 'a';
      }
      num /= 16;
    }
  }
  for (int j = 0; j < i; j++) {
    ((char *)dst)[j] = temp[i - j - 1];
  }
  ((char *)dst)[i] = '\0';
  return dst;
}

void *doubletodecimal(void *dst, double num) {
  // // 处理整数部分
  // int int_part = (int)num;
  // char int_str[20];
  // numtodecimal(int_str, int_part);

  // // 处理小数部分，保留两位小数
  // double frac_part = num - int_part;
  // char frac_str[20];
  // numtodecimal(frac_str, (int)(frac_part * 100));

  // // 拼接整数部分和小数部分
  // char *p = (char *)dst;
  // strcpy(p, int_str);
  // p += strlen(int_str);
  // *p++ = '.';
  // strcpy(p, frac_str);

  strcpy(dst,"?.??");
  
  return dst;
}

#endif
