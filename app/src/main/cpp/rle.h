/**
 * Run Length (rle.h)
 *
 */

#ifndef RLE_H
#define RLE_H

#ifdef _cplusplus
extern "C" {
#endif /* _cplusplus */

#include "dcrz.h"

#define ONE_LITERAL 0
#define TWO_LITERAL 1
#define THREE_LITERAL 2
#define FOUR_LITERAL 3

uint8_t *rbuffer;
uint32_t rbufferIndex;

// Was rle algorithm executed ?
bool utilizeRLE;

// Devoid condition variable
bool isRLEused;

#ifdef _cplusplus
}
#endif /* _cplusplus */

#endif /* RLE_H */
