/**
 * dcrz encode stream of symbols
 */

#include "dcrzEncode.h"

/**
 * Initialize dcrz encode
 */
void initdcrzEncode() {
    isRLEused = false;
    utilizeRLE = false;
    isMTFused = false;
    isBWTused = false;
    isHUFFused = false;
    isLastBlock = false;
}

/**
 * Release all the resources.
 */
void devoidEncode() {
    devoidRLE();
    devoidMTF();
    devoidBWT();
    devoidHuffEncode();
}

/**
 * dcrz compression function
 * @return error code
 */
int8_t dcrzEncode() {

    initdcrzEncode();

    while (true) {
        symbolsRead = fread(ibuffer, sizeof(uint8_t), BUFFER_SIZE, in);

        if (!symbolsRead) { break; }
        if (symbolsRead < BUFFER_SIZE) { isLastBlock = true; }

        if (!(tbuffer1 = mtfEncode(ibuffer, symbolsRead))) {
            devoidEncode();
            return ERR_MTF_1;
        }
        devoidMTF();

        if (!(tbuffer2 = rleEncode(tbuffer1, symbolsRead))) {
            devoidEncode();
            return ERR_RLE_1;
        }

        if (utilizeRLE) {
            MY_FREE(tbuffer1);
        } else {
            devoidRLE();
        }

        if (!(tbuffer1 = bwt(tbuffer2, rbufferIndex))) {
            devoidEncode();
            return ERR_BWT;
        }
        MY_FREE(tbuffer2);

        if (!(tbuffer2 = mtfEncode(tbuffer1, rbufferIndex + 3))) {
            devoidEncode();
            return ERR_MTF_2;
        }
        MY_FREE(tbuffer1);


        if (!(tbuffer1 = rleEncode(tbuffer2, rbufferIndex + 3))) {
            devoidEncode();
            return ERR_RLE_2;
        }

        if (!utilizeRLE) {
            if (!huffEncode(tbuffer2, rbufferIndex + 3)) {
                devoidEncode();
                return ERR_HUFF;
            }
        } else {
            MY_FREE(tbuffer2);
            if (!huffEncode(tbuffer1, rbufferIndex)) {
                devoidEncode();
                return ERR_HUFF;
            }
        }

        devoidEncode();

        if (isLastBlock) { break; }
    }

    return 0;
}
