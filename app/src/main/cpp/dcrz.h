/**
 * dcrz compression (dcrz.h)
 *
 */

#ifndef DCRZ_H
#define DCRZ_H

#ifdef _cplusplus
extern "C" {
#endif /* _cplusplus */

#include<stdio.h>
#include<stdlib.h>
#include<stdbool.h>
#include<string.h>
#include<stdint.h>

#define MAX_SYMBOLS 256    // max 256
#define BUFFER_SIZE 100000 // max 2_000_000
#define ERROR 99
#define SUCCESS 24

#define MY_MALLOC(ptr, type, num, size) ptr = (type)malloc(num*size);                                \
                if(!ptr) {                                    \
                    fprintf(stderr,"Memory allocation (malloc) for %s failed\n",#ptr);    \
                    return 0;                                \
                }
#define MY_CALLOC(ptr, type, num, size) ptr = (type)calloc(num,size);                                \
                if(!ptr) {                                    \
                    fprintf(stderr,"Memory allocation (calloc) for %s failed\n",#ptr);    \
                    return 0;                                \
                }

#define MY_FREE(ptr) free(ptr);

FILE *in, *out;
bool isLastBlock;
bool isLastChunk;

// Input and Temp buffer
uint8_t *ibuffer;
uint8_t *tbuffer1;
uint8_t *tbuffer2;
uint32_t symbolsRead;

// Function Prototypes
extern int8_t dcrzEncode();

extern int8_t dcrzDecode();

extern bool isRLEused;
extern bool utilizeRLE;
extern uint32_t rbufferIndex;

extern bool isMTFused;
extern bool isBWTused;
extern bool isIBWTused;
extern bool isHUFFused;

extern void devoidRLE();

extern void devoidMTF();

extern void devoidBWT();

extern void devoidIBWT();

extern void devoidHuffEncode();

extern void devoidHuffDecode();

#ifdef _cplusplus
}
#endif /* _cplusplus */

#endif /* DCRZ_H */
