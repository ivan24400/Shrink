/**
 * Move To Front (mtf.h)
 *
 */

#ifndef MTF_H
#define MTF_H

#ifdef _cplusplus
extern "C" {
#endif /* _cplusplus */

#include "dcrz.h"

typedef struct LinkedList {
    uint8_t symbol;
    struct LinkedList *next, *prev;
} LinkedList;

LinkedList *head;

uint8_t *mbuffer;
uint32_t mbufferIndex;

// Devoid condition variable
bool isMTFused;

#ifdef _cplusplus
}
#endif /* _cplusplus */

#endif /* MTF_H */
