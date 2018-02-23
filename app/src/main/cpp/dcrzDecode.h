/**
 * dcrz decode (dcrzDecode.h)
 *
 */

#ifndef DCRZ_DECODE_H
#define DCRZ_DECODE_H

#ifdef _cplusplus
extern "C" {
#endif /* _cplusplus */

#include "dcrz.h"

// Function prototypes
extern uint8_t *mtfDecode(uint8_t *, uint32_t);

extern uint8_t *rleDecode(uint8_t *, uint32_t);

extern uint8_t *ibwt(uint8_t *, uint32_t);

extern uint8_t *huffDecode();

#ifdef _cplusplus
}
#endif /* _cplusplus */

#endif /* DCRZ_DECODE_H */
