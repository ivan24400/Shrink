/**
 * Inverse Burrows wheeler transform
 */

#include "ibwt.h"

/**
 * Release all resources
 */
void devoidIBWT() {
    if (isIBWTused) {
        MY_FREE(ocrTable);
        MY_FREE(sp);
        isIBWTused = false;
    }
}

/**
 * Get symbol by key of same type.
 * @param key Key to use for value
 * @param prevValue it stores value for previous key
 * @return symbol format of value for given key
 */
uint8_t getValueByKey(uint32_t *key, uint32_t symbolCount) {

    for (register uint32_t i = 0; i < symbolCount; i++) {
        if (sp[i].key == *key) {
            *key = sp[i].value;
            return ((sp[i].value >> 24) & 0xff);
        }
    }
    return 0;
}

/**
 * Swap two unsigned 32 bit numbers
 * @param m first number
 * @param n second number
 */
void swap(uint32_t *m, uint32_t *n) {
    uint32_t t = *m;
    *m = *n;
    *n = t;
}

/**
 * Sorts keys in symbol pair table lexicographically
 * @param low first index
 * @param high last index
 */
void sortSymbolPairs(int32_t low, int32_t high) {
    uint32_t pivot = sp[low].key;
    int32_t i = low;
    int32_t j = high;

    while (i <= j) {
        while (sp[i].key < pivot)
            i++;

        while (sp[j].key > pivot)
            j--;

        if (i <= j) {
            swap(&sp[i].key, &sp[j].key);
            i++;
            j--;
        }
    }
    if (low < j)
        sortSymbolPairs(low, j);
    if (i < high)
        sortSymbolPairs(i, high);
}

uint8_t initSymbolPairArray(uint8_t *buffer, uint32_t symbolCount) {
    MY_MALLOC(sp, SymbolPair*, symbolCount, sizeof(SymbolPair))
    MY_CALLOC(ocrTable, uint32_t*, MAX_SYMBOLS, sizeof(uint32_t))
    MY_MALLOC(ibBuffer, uint8_t*, symbolCount, sizeof(uint8_t))


    // 1 based occurences of symbols
    for (register uint32_t i = 3, j = 0; i < (symbolCount + 3); i++, j++) {
        ocrTable[buffer[i]]++;
        sp[j].value = ((buffer[i] << 24) | ocrTable[buffer[i]]);
        sp[j].key = sp[j].value;
    }

    // Sort keys
    sortSymbolPairs(0, symbolCount - 1);
    return SUCCESS;
}

/**
 * Compute inverse burrows wheeler transform for given input buffer.
 * @param buffer input buffer
 * @param key key to original string in bwt 
 * @param symbolCount total symbols in buffer
 * @return inverse burrows wheeler transform of buffer
 */
uint8_t *ibwt(uint8_t *buffer, uint32_t symbolCount) {
    if (!buffer || !symbolCount) {
        fprintf(stderr, "Invalid arguments buffer\n");
        return NULL;
    }
    isIBWTused = true;
    symbolCount = symbolCount - 3;
    if (!initSymbolPairArray(buffer, symbolCount)) { return NULL; }

    uint32_t key = buffer[0]; // header
    key = key | (buffer[1] << 8);
    key = key | (buffer[2] << 16);

    if (key & 0x00800000) { // RLE should be used next
        utilizeRLE = true;
    } else {
        utilizeRLE = false;
    }

    key = key & 0x007fffff;

    uint32_t prevValue = sp[key].value;
    ibBuffer[symbolCount - 1] = ((prevValue >> 24) & 0xff);

    for (register int32_t i = symbolCount - 2; i >= 0; i--) {
        ibBuffer[i] = getValueByKey(&prevValue, symbolCount);
    }

    return ibBuffer;
}
