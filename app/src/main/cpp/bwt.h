/**
 * Burrows Wheeler transform (bwt.h)
 *
 */

#ifndef BWT_H
#define BWT_H

#ifdef _cplusplus
extern "C" {
#endif /* _cplusplus */

#include "dcrz.h"

#define SET_NXT_INDEX(i)    if( i < bSymbolCount-1 ){    \
                    i = i + 1;        \
                } else {            \
                    i = 0;            \
                }

uint32_t compareLength;
uint32_t bSymbolCount;

uint32_t *indexBuffer;
uint8_t *bInBuffer;
uint8_t *bOutBuffer;

// Devoid condition variable
bool isBWTused;

#ifdef _cplusplus
}
#endif /* _cplusplus */

#endif  /* BWT_H */
