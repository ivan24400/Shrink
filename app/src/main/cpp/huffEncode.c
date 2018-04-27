/**
 * Huffman encoder
 */

#include "huffman.h"
long byteCount=0;
/**
 * Deallocate binary tree nodes
 */
void freeNode(Node *node) {
    if (node) {
        freeNode(node->left);
        freeNode(node->right);
        MY_FREE(node);
    }
}

/**
 * Release all resources
 */
void devoidHuffEncode() {
    if (isHUFFused) {
        MY_FREE(heap->array);
        MY_FREE(heap);
        MY_FREE(st);
        MY_FREE(code);
        for (register uint16_t i = 0; i < MAX_SYMBOLS; i++) {
            free(ct[i].code);
        }
        MY_FREE(ct);
        freeNode(htree);
        isHUFFused = false;
    }
}


/**
 * Calculate height of given node
 * @param node height of which is to be calculated
 * @return height of node
 */
uint16_t heightOf(Node *node) {
    if (!node) {
        return 0;
    }
    uint16_t left = heightOf(node->left);
    uint16_t right = heightOf(node->right);
    if (left > right)
        return left + 1;
    else
        return right + 1;
}

/**
 * Create a new node with given symbol and its frequency
 * @param data symbol
 * @param freq frequency of data in given buffer
 * @return pointer to newly created node.
 */
Node *newNode(uint8_t data, uint32_t freq) {
    Node *temp;
    MY_MALLOC(temp, Node*, 1, sizeof(Node))
    temp->left = NULL;
    temp->right = NULL;
    temp->data = data;
    temp->freq = freq;

    return temp;
}

/**
 * Swap two nodes
 * @param a double pointer to one node
 * @param b double pointer to second node
 */
void swapNodes(Node **a, Node **b) {
    Node *t = *a;
    *a = *b;
    *b = t;
}

/**
 * Insert given node into heap
 * @param node Node to be inserted
 */
void insertNode(Node *node) {

    int32_t i = heap->size;
    ++heap->size;

    while (i && node->freq < heap->array[(i - 1) / 2]->freq) {
        heap->array[i] = heap->array[(i - 1) / 2];
        i = (i - 1) / 2;
    }
    heap->array[i] = node;
}

/**
 * Balance heap tree
 * @param index Index of a node
 */
void heapify(uint32_t index) {

    uint32_t smallest = index;
    uint32_t left = 2 * index + 1;
    uint32_t right = 2 * index + 2;

    if (left < heap->size &&
        heap->array[left]->freq < heap->array[smallest]->freq)
        smallest = left;

    if (right < heap->size &&
        heap->array[right]->freq < heap->array[smallest]->freq)
        smallest = right;

    if (smallest != index) {
        swapNodes(&heap->array[smallest], &heap->array[index]);
        heapify(smallest);
    }
}

/**
 * Returns root node of heap
 * @return root node of heap
 */
Node *getNode() {

    Node *temp = heap->array[0];
    heap->array[0] = heap->array[heap->size - 1];

    --heap->size;
    heapify(0);

    return temp;
}

/**
 * Create a heap tree for given symbols and their frequencies
 * @return error
 */
uint8_t buildHeap() {

    heap = (Heap *) malloc(sizeof(Heap));
    heap->size = 0;
    MY_MALLOC(heap->array, Node**, totalUniqueSymbolCount, sizeof(Node *))

    for (register uint32_t i = 0; i < totalUniqueSymbolCount; ++i)
        heap->array[i] = newNode(st[i].data, st[i].freq);

    heap->size = totalUniqueSymbolCount;

    int32_t n = heap->size - 1;

    for (register int32_t i = (n - 1) / 2; i >= 0; i--)
        heapify(i);

    return SUCCESS;
}

/**
 * Create huffman tree for given symbols
 * @return pointer to root node of heap
 */
Node *buildHuffmanTree() {
    Node *left, *right, *sum;
    if (!buildHeap()) {
        return NULL;
    }

    while (heap->size != 1) {

        left = getNode();
        right = getNode();

        sum = newNode('+', left->freq + right->freq);
        if (!sum) {
            return NULL;
        }
        sum->left = left;
        sum->right = right;

        insertNode(sum);
    }

    return getNode();
}


/**
 * Add code length for given symbol
 * @param data symbol
 * @param n code length of data
 */
void addCode(uint8_t data, uint8_t n) {
    ct[data].codeLength = n;
    for (register uint8_t i = 0; i < n; i++) {
        ct[data].code[i] = code[i];
    }
}

/**
 * Creates code table for given tree. It creates binary
 * code for each symbol in buffer
 * @param root node whose code is to be calculated
 * @param index current index in code array
 */
void initCodeTable(Node *root, uint8_t index) {
    if (root->left) {
        code[index] = 0;
        initCodeTable(root->left, index + 1);
    }

    if (root->right) {
        code[index] = 1;
        initCodeTable(root->right, index + 1);
    }

    if (!root->left && !root->right) {
        addCode(root->data, index);
    }
}

/**
 * Function to write bit level data
 * @param bit Bit to be written
 * @param isFlush flush bit stream
 */
void writeBit(uint8_t bit, bool isFlush) {

    if (isFlush && ((wCursor < 7) && (wCursor >= -1))) {
        fwrite(&bitBuffer, sizeof(uint8_t), 1, out);
        byteCount++;
        bitBuffer = 0;
        wCursor = 7;
        return;

    }
    if (isFlush && (wCursor == 7)) { return; }

    if (wCursor < 0) {
        fwrite(&bitBuffer, sizeof(uint8_t), 1, out);
        byteCount++;
        bitBuffer = 0;
        wCursor = 7;
    }

    if (bit) {
        bitBuffer = bitBuffer | (uint8_t) (1 << wCursor);
    }
    wCursor--;
}

/**
 * Write Byte from current bit-level position
 * in output stream.
 * @param data data to be written
 */
void writeByte(uint8_t data) {
    int8_t ptr = 7;
    while (ptr >= 0) {
        writeBit((uint8_t) (((data >> ptr--) & 0x01)), false);
    }
}


/**
 * Write symbols used in preorder format
 * @param node Current node
 */
void writeTree(Node *node) {
    if (node) {
        if (!node->left && !node->right) {
            writeBit(1, false);
            writeByte(node->data);
            return;
        }
        writeBit(0, false);
        writeTree(node->left);
        writeTree(node->right);
    }
}

/**
 * Write header information (variable length)
 * @param root Root node of tree
 */
void writeHeader(Node *root, uint32_t totalSymbols) {
    uint8_t byte = 0;
    uint32_t b1 = 0;
    __android_log_print(ANDROID_LOG_DEBUG,"JNI_HUFFMAN","huffEncode: isLastBlock %d isLastChunk %d\n");
    fflush(stdout);
    if (isLastBlock && isLastChunk) { //  declared in dcrz.h
        b1 = 0x00800000;
    }

    if (utilizeRLE) { // Is rle used in previous phase
        b1 = b1 | 0x00400000;
    }

    b1 = b1 | (totalSymbols & 0x003fffff);

    fprintf(logFile,"Writing header %x @ %lu");

    // Little Endian
    byte = (uint8_t) (b1 & 0xff);
    fwrite(&byte, sizeof(uint8_t), 1, out); byteCount++;

    byte = (uint8_t) ((b1 >> 8) & 0xff);
    fwrite(&byte, sizeof(uint8_t), 1, out); byteCount++;

    byte = (uint8_t) ((b1 >> 16) & 0xff);
    fwrite(&byte, sizeof(uint8_t), 1, out); byteCount++;

    wCursor = 7;
    writeTree(root);
    writeBit(0, true);

}

/**
 * Write code for each symbol in buffer to output
 * @param buffer input buffer
 * @param bufferLength size of buffer
 */
void writeCodes(uint8_t *buffer, uint32_t bufferLength) {
    wCursor = 7;
    for (register uint32_t i = 0; i < bufferLength; i++) {
        for (register uint8_t j = 0; j < ct[buffer[i]].codeLength; j++) {
            writeBit(ct[buffer[i]].code[j], false);
        }
    }
    writeBit(0, true);
}

/**
 * Calculate frequencies of each symbol
 * @param ibuffer input buffer
 * @param symbolCount total symbols in buffer
 * @return error
 */
uint8_t computeFrequencies(uint8_t *ibuffer, uint32_t symbolCount) {

    totalUniqueSymbolCount = 0;
    height = 0;
    isHUFFused = true;
    wCursor = 7;

    MY_CALLOC(freq, uint32_t*, MAX_SYMBOLS, sizeof(uint32_t))

    for (register uint32_t i = 0; i < symbolCount; i++) {
        if (!freq[ibuffer[i]]) {
            totalUniqueSymbolCount++;
        }
        freq[ibuffer[i]]++;
    }

    // Compute symbol table (symbol, frequencies)
    MY_MALLOC(st, SymbolTable*, totalUniqueSymbolCount, sizeof(SymbolTable))

    for (register uint16_t i = 0, j = 0; i < MAX_SYMBOLS; i++) {
        if (freq[i]) {
            st[j].data = i;
            st[j].freq = freq[i];
            j++;
        }
    }

    MY_FREE(freq);
    return SUCCESS;
}


/**
 * huffman Encoding function
 * @param buffer buffer of symbols
 * @param totalSymbols number of symbols in buffer
 * @return error code
 */
uint8_t huffEncode(uint8_t *buffer, uint32_t totalSymbols) {
    if (!buffer || !totalSymbols) {
        fprintf(stderr, "HUFFMAN: Invalid arguments\n");
        return ERROR;
    }

    if (!computeFrequencies(buffer, totalSymbols)) { return 0; }

    htree = buildHuffmanTree();
    if (!htree) { return 0; }
    height = heightOf(htree);

    MY_MALLOC(ct, CodeTable*, MAX_SYMBOLS, sizeof(CodeTable))
    MY_CALLOC(code, uint8_t*, height, sizeof(uint8_t))

    for (register uint16_t i = 0; i < MAX_SYMBOLS; i++) {
        ct[i].symbol = (uint8_t) i;
        MY_CALLOC(ct[i].code, uint8_t*, height, sizeof(uint8_t))
    }


    initCodeTable(htree, 0);
    writeHeader(htree, totalSymbols);
    writeCodes(buffer, totalSymbols);

    return SUCCESS;
}


