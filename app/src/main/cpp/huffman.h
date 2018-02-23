/**
 * Huffman encoding and decoding (huffman.h)
 *
 */

#ifndef HUFFMAN_H
#define HUFFMAN_H

#ifdef _cplusplus
extern "C" {
#endif /* _cplusplus */

#include "dcrz.h"

#define POW_2(x) 1 << x

typedef struct Node {
    uint8_t data;
    uint32_t freq;
    struct Node *left, *right;
} Node;


typedef struct Heap {
    uint32_t size;
    struct Node **array;
} Heap;

typedef struct SymbolTable {
    uint8_t data;
    uint32_t freq;
} SymbolTable;

typedef struct CodeTable {
    uint8_t symbol;
    uint8_t codeLength;
    uint8_t *code;
} CodeTable;

Node *htree;
Heap *heap;
SymbolTable *st;
CodeTable *ct;

uint8_t *code;
uint32_t ctIndex;
uint16_t totalUniqueSymbolCount;

uint32_t *freq;
uint16_t height;

uint8_t bitBuffer;
int8_t wCursor;

// Function prototypes
Node *newEmptyNode();

void printTree(Node *);

uint16_t heightOf(Node *);

void freeNode(Node *);

// Huffman Decode
extern bool isLastBlock;
uint16_t leafCount;
uint8_t *hBuffer;
uint32_t hBufferIndex;

int8_t rCursor;

// Devoid condition variable
bool isHUFFused;
uint64_t hcount;

#ifdef _cplusplus
}
#endif /* _cplusplus */

#endif /* HUFFMAN_H */
