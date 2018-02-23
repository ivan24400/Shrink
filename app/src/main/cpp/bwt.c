/**
 * Burrows wheeler transform
 */
#include "bwt.h"

/**
 * Release all Resources
 */
void devoidBWT() {
    if (isBWTused) {
        MY_FREE(indexBuffer);
        isBWTused = false;
    }
}

/**
 * Initialize variables
 * @param b input buffer
 * @param n number of symbols in buffer
 */
uint8_t initBWT(uint8_t *b, uint32_t n) {
    compareLength = 0;
    bSymbolCount = n;
    bInBuffer = b;
    isBWTused = true;

    MY_MALLOC(indexBuffer, uint32_t*, bSymbolCount, sizeof(uint32_t))
    MY_MALLOC(bOutBuffer, uint8_t*, (bSymbolCount + 3), sizeof(uint8_t));

    for (register uint32_t i = 0; i < bSymbolCount; i++) {
        indexBuffer[i] = i;
    }
    return SUCCESS;
}

/**
 * String comparision function
 * @param a index in buffer
 * @param b index in buffer
 * @return integer representing a > b or a < b
 */
int compareString(const void *a, const void *b) {

    if (compareLength < bSymbolCount) {
        uint32_t m = *(uint32_t *) a;
        uint32_t n = *(uint32_t *) b;

        if (bInBuffer[m] > bInBuffer[n]) {
            compareLength = 0;
            return 1;
        } else if (bInBuffer[m] < bInBuffer[n]) {
            compareLength = 0;
            return -1;
        } else {
            compareLength++;
            SET_NXT_INDEX(m)
            SET_NXT_INDEX(n)
            return compareString(&m, &n);
        }
    } else {
        return 0;
    }

}

/**
 * Compute burrows wheeler transform
 * @param buffer input buffer
 * @param symbolCount number of symbols in buffer
 * @return burrows wheeler transformed version of input
 */
uint8_t *bwt(uint8_t *buffer, uint32_t symbolCount) {
    if (!buffer || !symbolCount) {
        fprintf(stderr, "BWT: Invalid arguments buffer %p sc %d\n", buffer, symbolCount);
        return NULL;
    }
    if (!initBWT(buffer, symbolCount)) { return NULL; }

    qsort(indexBuffer, bSymbolCount, sizeof(uint32_t), compareString);

    uint32_t key = 0x00800000; // RLE used
    int32_t index = 0;
    for (register uint32_t i = 0, j = 3; i < bSymbolCount; j++, i++) {

        index = indexBuffer[i] - 1;
        if (index < 0) {
            key = i;
            index = index + bSymbolCount;
        }
        bOutBuffer[j] = bInBuffer[index];
    }
    if (key == 0x00800000) {
        fprintf(stderr, "BWT: Invalid key\n");
        return NULL;
    }

    if (!utilizeRLE) {
        key = key & 0x007fffff; // RLE not used
    }
    bOutBuffer[0] = key & 0xff;  // Little Endian
    bOutBuffer[1] = (key >> 8) & 0xff;
    bOutBuffer[2] = (key >> 16) & 0xff;

    return bOutBuffer;
}
