/**
 * dcrz encode (dcrzEncode.h)
 *
 */

#ifndef DCRZ_ENCODE_H
#define DCRZ_ENCODE_H

#ifdef _cplusplus
extern "C" {
#endif /* _cplusplus */

#include "dcrz.h"

// Function prototypes
extern uint8_t *bwt(uint8_t *, uint32_t);

extern uint8_t *mtfEncode(uint8_t *, uint32_t);

extern uint8_t *rleEncode(uint8_t *, uint32_t);

extern uint8_t huffEncode(uint8_t *, uint32_t);

#ifdef _cplusplus
}
#endif /* _cplusplus */

#endif /* DCRZ_ENCODE_H */
