/**
 * Inverse Burrows Wheeler transform (ibwt.h)
 *
 */

#ifndef IBWT_H
#define IBWT_H

#ifdef _cplusplus
extern "C" {
#endif /* _cplusplus */

#include "dcrz.h"

typedef struct SymbolPair {
    uint32_t key;
    uint32_t value;
} SymbolPair;

SymbolPair *sp;
uint32_t *ocrTable; // Occurence table

uint8_t *ibBuffer; // output buffer

// Devoid condition variable
bool isIBWTused;

#ifdef _cplusplus
}
#endif /* _cplusplus */

#endif /* IBWT_H */
