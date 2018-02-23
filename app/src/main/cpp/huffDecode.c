/**
 * Huffman decoder
 */

#include "huffman.h"

/**
 * Release all resources
 */
void devoidHuffDecode() {
    if (isHUFFused) {
        for (register uint32_t i = 0; i < leafCount; i++) {
            MY_FREE(ct[i].code);
        }
        MY_FREE(ct);
        MY_FREE(code);
        freeNode(htree);
        isHUFFused = false;
    }
}

/**
 * Read bits from input stream
 * @param isFlush to flush input stream or not
 * @return raw bit
 */
uint8_t readBit(bool isFlush) {

    if (isFlush) {
        rCursor = 7;
        bitBuffer = 0;
        return 2;
    }

    if (rCursor < 0 || rCursor == 7) {
        rCursor = 7;
        fread(&bitBuffer, sizeof(uint8_t), 1, in);
    }

    return ((bitBuffer & (1 << rCursor--)) ? 1 : 0);
}

/**
 * Read byte from current bit position
 * @return raw byte
 */
uint8_t readByte() {
    uint8_t t = 0;
    int8_t ptr = 7;
    while (ptr >= 0) {
        t = t | (readBit(false) << ptr--);
    }
    return t;
}

/**
 * Add binary codeword for given symbol
 * @param data symbol
 * @param n codeword length of data
 */
void addDecodeWord(uint8_t data, uint8_t n) {
    ct[ctIndex].symbol = data;
    ct[ctIndex].codeLength = n;

    for (register uint8_t i = 0; i < n; ++i) {
        ct[ctIndex].code[i] = code[i];
    }
    ctIndex++;

}

/**
 * Create a new empty node. Used for perfect binary tree.
 * @return pointer to empty node
 */
Node *newEmptyNode() {

    Node *temp;
    MY_MALLOC(temp, Node*, 1, sizeof(Node))
    temp->left = NULL;
    temp->right = NULL;
    temp->data = '+'; // dummy data
    temp->freq = 0;

    return temp;
}

/**
 * Creates codeword table for given tree. It creates binary
 * codeword for each symbol in buffer
 * @param root node whose codeword is to be calculated
 * @param index current index in codeword array
 */
void initDecodeTable(Node *root, uint8_t index) {

    if (root->left) {
        code[index] = 0;
        initDecodeTable(root->left, index + 1);
    }

    if (root->right) {
        code[index] = 1;
        initDecodeTable(root->right, index + 1);
    }

    if (!root->left && !root->right) {
        addDecodeWord(root->data, index);
    }
}

/**
 * Read binary tree in preorder format recursively
 * @param node the node whose value is read
 */
void readTree(Node **node) {
    uint8_t bit = readBit(false);
    *node = newEmptyNode();

    if (bit) {
        (*node)->data = readByte();
        leafCount++;
        return;
    }
    readTree(&(*node)->left);
    readTree(&(*node)->right);
}

/**
 * Decode huffman encoded blocks
 * @return huffman decoded buffer
 */
uint8_t *huffDecode() {
    uint8_t byte = 0;
    uint16_t i, j;
    uint32_t b1 = 0;
    uint32_t bcount = 0;

    hBufferIndex = 0;
    symbolsRead = 0; // declared in dcrz.h
    isHUFFused = true;
    leafCount = 0;
    rCursor = 7;

    fread(&byte, sizeof(uint8_t), 1, in);
    b1 = byte;
    fread(&byte, sizeof(uint8_t), 1, in);
    b1 = b1 | (byte << 8);
    fread(&byte, sizeof(uint8_t), 1, in);
    b1 = b1 | (byte << 16);

    isLastBlock = b1 & 0x00800000;

    if (b1 & 0x00400000) {
        utilizeRLE = true; // RLE used
    } else {
        utilizeRLE = false;
    }

    symbolsRead = b1 & 0x003fffff;
    bcount = symbolsRead;

    MY_CALLOC(hBuffer, uint8_t*, bcount, sizeof(uint8_t))

    readBit(true);
    readTree(&htree);
    readBit(true);

    height = heightOf(htree);

    MY_MALLOC(ct, CodeTable*, leafCount, sizeof(CodeTable))
    MY_CALLOC(code, uint8_t*, height, sizeof(uint8_t))
    ctIndex = 0;

    for (i = 0; i < leafCount; i++) {
        ct[i].symbol = 0;
        MY_CALLOC(ct[i].code, uint8_t*, height, sizeof(uint8_t));
    }

    initDecodeTable(htree, 0);

    while (bcount) {
        i = 0;
        j = 0;
        code[i] = readBit(false);

        while (j < ctIndex) {
            if (code[i] == ct[j].code[i]) {
                if (ct[j].codeLength - 1 == i) {
                    hBuffer[hBufferIndex++] = ct[j].symbol;
                    break;
                } else {
                    i++;
                    code[i] = readBit(false);
                }
            } else {
                j++;
            }
        }
        if (j == ctIndex) {
            fprintf(stderr, "HUFFMAN: Invalid code !\n");
            return NULL;
        }
        bcount--;
    }
    readBit(true);

    return hBuffer;
}
