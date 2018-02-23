/**
 * Program to compute run length for a given buffer of symbols
 *
 */

/**
 * Run length encoder and decoder
 */

#include "rle.h"

/**
 * Release all resources
 */
void devoidRLE() {
    if (isRLEused) {
        MY_FREE(rbuffer)
        isRLEused = false;
    }
}

/**
 * Initialize required variables.
 * @param bufferSize Size of runlength buffer
 * @return error
 */
uint8_t initRLE(uint32_t bufferSize) {
    MY_MALLOC(rbuffer, uint8_t*, bufferSize, sizeof(uint8_t))
    isRLEused = true;
    utilizeRLE = true;
    rbufferIndex = 0;
    return SUCCESS;
}

/**
 * Function to compute run length of given buffer
 * @param buffer Input symbols
 * @param symbolCount total number of input symbols
 * @return run length encoded buffer
 */
uint8_t *rleEncode(uint8_t *buffer, uint32_t symbolCount) {
    if (!buffer || !symbolCount) {
        fprintf(stderr, "RL_E: Invalid arguments\n");
        return NULL;
    }

    if (!initRLE(2 * symbolCount)) { return NULL; }

    for (register uint32_t i = 0; i < symbolCount;) {

        uint16_t count = 1;

        if (rbufferIndex > symbolCount) { // IF RLE expands
            rbufferIndex = symbolsRead;
            utilizeRLE = false;
            return buffer;
        }
        while (buffer[i] == buffer[i + count] && count < MAX_SYMBOLS) {
            count++;
        }
        if (count >= 5) {
            rbuffer[rbufferIndex++] = count - 1;
            rbuffer[rbufferIndex++] = buffer[i];
            i = i + count;
        } else {
            if ((i + 8) < symbolCount &&
                buffer[i + 4] == buffer[i + 5] &&
                buffer[i + 4] == buffer[i + 6] &&
                buffer[i + 4] == buffer[i + 7] &&
                buffer[i + 4] == buffer[i + 8]
                    ) {
                // literal of size four
                rbuffer[rbufferIndex++] = FOUR_LITERAL;
                rbuffer[rbufferIndex++] = buffer[i];
                rbuffer[rbufferIndex++] = buffer[i + 1];
                rbuffer[rbufferIndex++] = buffer[i + 2];
                rbuffer[rbufferIndex++] = buffer[i + 3];
                i = i + 4;

            } else if ((i + 7) < symbolCount &&
                       buffer[i + 3] == buffer[i + 4] &&
                       buffer[i + 3] == buffer[i + 5] &&
                       buffer[i + 3] == buffer[i + 6] &&
                       buffer[i + 3] == buffer[i + 7]
                    ) {
                // literal of size three
                rbuffer[rbufferIndex++] = THREE_LITERAL;
                rbuffer[rbufferIndex++] = buffer[i];
                rbuffer[rbufferIndex++] = buffer[i + 1];
                rbuffer[rbufferIndex++] = buffer[i + 2];
                i = i + 3;

            } else if ((i + 6) < symbolCount &&
                       buffer[i + 2] == buffer[i + 3] &&
                       buffer[i + 2] == buffer[i + 4] &&
                       buffer[i + 2] == buffer[i + 5] &&
                       buffer[i + 2] == buffer[i + 6]
                    ) {
                // literal of size two
                rbuffer[rbufferIndex++] = TWO_LITERAL;
                rbuffer[rbufferIndex++] = buffer[i];
                rbuffer[rbufferIndex++] = buffer[i + 1];
                i = i + 2;

            } else if ((i + 5) < symbolCount &&
                       buffer[i + 1] == buffer[i + 2] &&
                       buffer[i + 1] == buffer[i + 3] &&
                       buffer[i + 1] == buffer[i + 4] &&
                       buffer[i + 1] == buffer[i + 5]
                    ) {
                // literal of size one
                rbuffer[rbufferIndex++] = ONE_LITERAL;
                rbuffer[rbufferIndex++] = buffer[i];
                i = i + 1;
            } else {    // default case
                if ((i + 3) < symbolCount) {
                    // literal of size four
                    rbuffer[rbufferIndex++] = FOUR_LITERAL;
                    rbuffer[rbufferIndex++] = buffer[i];
                    rbuffer[rbufferIndex++] = buffer[i + 1];
                    rbuffer[rbufferIndex++] = buffer[i + 2];
                    rbuffer[rbufferIndex++] = buffer[i + 3];
                    i = i + 4;
                } else if ((i + 2) < symbolCount) {
                    rbuffer[rbufferIndex++] = THREE_LITERAL;
                    rbuffer[rbufferIndex++] = buffer[i];
                    rbuffer[rbufferIndex++] = buffer[i + 1];
                    rbuffer[rbufferIndex++] = buffer[i + 2];
                    i = i + 3;
                } else if ((i + 1) < symbolCount) {
                    rbuffer[rbufferIndex++] = TWO_LITERAL;
                    rbuffer[rbufferIndex++] = buffer[i];
                    rbuffer[rbufferIndex++] = buffer[i + 1];
                    i = i + 2;
                } else if ((i) < symbolCount) {
                    rbuffer[rbufferIndex++] = ONE_LITERAL;
                    rbuffer[rbufferIndex++] = buffer[i];
                    i = i + 1;
                }
            }
        }
        count = 1;
    }
    if (rbufferIndex >= symbolCount) { // IF RLE expands
        rbufferIndex = symbolsRead;
        utilizeRLE = false;
        return buffer;
    }

    return rbuffer;
}

/**
 * Function to decode run-length encoded symbols
 * @param buffer run length encoded buffer
 * @param symbolCount total symbols present in buffer
 * @return run-length decoded form of buffer
 */
uint8_t *rleDecode(uint8_t *buffer, uint32_t symbolCount) {
    if (!buffer || !symbolCount) {
        fprintf(stderr, "RL_D: Invalid arguments\n");
        return NULL;
    }
    if (!utilizeRLE) {
        isRLEused = false;
        rbufferIndex = symbolCount;
        return buffer;
    }
    if (!initRLE(2 * symbolCount)) { return NULL; }

    for (register uint32_t i = 0; i < symbolCount;) {
        uint16_t count = buffer[i];
        if (count == ONE_LITERAL) {
            rbuffer[rbufferIndex++] = buffer[i + 1];
            i = i + 2;
        } else if (count == TWO_LITERAL) {
            rbuffer[rbufferIndex++] = buffer[i + 1];
            rbuffer[rbufferIndex++] = buffer[i + 2];
            i = i + 3;
        } else if (count == THREE_LITERAL) {
            rbuffer[rbufferIndex++] = buffer[i + 1];
            rbuffer[rbufferIndex++] = buffer[i + 2];
            rbuffer[rbufferIndex++] = buffer[i + 3];
            i = i + 4;
        } else if (count == FOUR_LITERAL) {
            rbuffer[rbufferIndex++] = buffer[i + 1];
            rbuffer[rbufferIndex++] = buffer[i + 2];
            rbuffer[rbufferIndex++] = buffer[i + 3];
            rbuffer[rbufferIndex++] = buffer[i + 4];
            i = i + 5;
        } else {
            count++;
            while (count) {
                rbuffer[rbufferIndex++] = buffer[i + 1];
                count--;
            }
            i = i + 2;
        }
    }
    return rbuffer;
}

