/**
 * Decode a dcrz encoded stream of symbols
 */
#include "dcrzDecode.h"

void initdcrzDecode() {
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
void devoidDecode() {
    devoidRLE();
    devoidMTF();
    devoidIBWT();
    devoidHuffDecode();
}

/**
 * dcrz Decoding function 
 * @return error code
 */
int8_t dcrzDecode() {

    initdcrzDecode();

    do {
        if (!(tbuffer1 = huffDecode())) {
            devoidDecode();
            return 10;
        }
        if (!(tbuffer2 = rleDecode(tbuffer1, symbolsRead))) {
            devoidDecode();
            return 11;
        }
        if (utilizeRLE) {
            MY_FREE(tbuffer1);
        } else {
            devoidRLE();
        }

        if (!(tbuffer1 = mtfDecode(tbuffer2, rbufferIndex))) {
            devoidDecode();
            return 12;
        }
        devoidMTF();
        MY_FREE(tbuffer2);

        if (!(tbuffer2 = ibwt(tbuffer1, rbufferIndex))) {
            devoidDecode();
            return 13;
        }
        MY_FREE(tbuffer1);

        rbufferIndex = rbufferIndex - 3;

        if (!(tbuffer1 = rleDecode(tbuffer2, rbufferIndex))) {
            devoidDecode();
            return 14;
        }
        if (utilizeRLE) {
            MY_FREE(tbuffer2);
        }

        if (!(tbuffer2 = mtfDecode(tbuffer1, rbufferIndex))) {
            devoidDecode();
            return 15;
        }
        MY_FREE(tbuffer1);

        uint32_t k = 0;
        while (k < rbufferIndex) {
            fwrite(&tbuffer2[k], sizeof(uint8_t), 1, out);
            k++;
        }
        MY_FREE(tbuffer2);

        devoidDecode();

    } while (!isLastBlock);
    return 0;
}
