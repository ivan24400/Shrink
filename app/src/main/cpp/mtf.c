/**
 * move to front encoder and decoder
 */

#include "mtf.h"

/**
 * Function to release all the acquired resources
 */
void devoidMTF() {
    if (isMTFused) {
        while (head) {
            LinkedList *tmp = head;
            head = head->next;
            MY_FREE(tmp);
        }
        isMTFused = false;
    }
}


/**
 * It creates a new node initialized with input data only.
 * @param symbol data to be stored in a double linked list node.
 * @return a double linked list node
 */
LinkedList *newListNode(uint8_t symbol) {
    LinkedList *node;
    MY_MALLOC(node, LinkedList*, 1, sizeof(LinkedList))
    node->symbol = symbol;
    node->next = NULL;
    node->prev = NULL;
    return node;
}

/**
 * Function to initialize a buffer and character set
 * @param head pointer to head node of double linked list
 * @param symbolCount total number of symbols present in buffer
 * @return error
 */
uint8_t initCharset(LinkedList *head, uint32_t symbolCount) {

    MY_MALLOC(mbuffer, uint8_t*, symbolCount, sizeof(uint8_t))

    mbufferIndex = 0;
    isMTFused = true;

    for (register uint32_t i = 1; i < MAX_SYMBOLS + 1; i++) {
        LinkedList *node = newListNode(i);
        if (!node) { return ERROR; }

        LinkedList *tmp = head;
        while (tmp->next) {
            tmp = tmp->next;
        }
        tmp->next = node;
        node->prev = tmp;
    }
    return SUCCESS;
}

/**
 * Function used during encoding to remove and push 
 * given symbol in front of the list
 * @param node head node of the list
 * @param symbol symbol to be pushed on front of list
 * @return new head of the list
 */
LinkedList *seeChar(LinkedList *node, uint8_t symbol) {

    LinkedList *tmp = node;
    uint16_t index = 0;
    while (tmp->symbol != symbol) {
        index++;
        tmp = tmp->next;
    }
    mbuffer[mbufferIndex++] = index;
    if (tmp->prev) {
        tmp->prev->next = tmp->next;
        tmp->next->prev = tmp->prev;
        tmp->next = node;
        node->prev = tmp;
        tmp->prev = NULL;
    }

    return tmp;
}

/**
 * Function used while decoding to output corresponding
 * symbol from the input index.
 * @param node head of the list
 * @param symbol index of the symbol
 * @return new head of the list
 */
LinkedList *seeIndex(LinkedList *node, uint8_t index) {

    LinkedList *tmp = node;
    while (index) {
        index--;
        tmp = tmp->next;
    }
    mbuffer[mbufferIndex++] = tmp->symbol;
    if (tmp->prev) {
        tmp->prev->next = tmp->next;
        tmp->next->prev = tmp->prev;
        tmp->next = node;
        node->prev = tmp;
        tmp->prev = NULL;
    }

    return tmp;
}

/**
 * Function to encode a given input into move to front format
 * @param buffer input buffer
 * @param symbolCount total number of symbols in buffer
 * @return mtf encoded buffer
 */
uint8_t *mtfEncode(uint8_t *buffer, uint32_t symbolCount) {
    if (!buffer || !symbolCount) {
        fprintf(stderr, "MTF_E: Invalid arguments\n");
        return NULL;
    }
    if (!(head = newListNode(0))) { return NULL; }
    if (!initCharset(head, symbolCount)) { return NULL; }

    for (register uint32_t i = 0; i < symbolCount; i++) {
        head = seeChar(head, buffer[i]);
    }

    return mbuffer;
}

/**
 * Function to decode a given input from move to front format
 * @param buffer input buffer
 * @param symbolCount total number of symbols in buffer
 * @return mtf decoded buffer
 */
uint8_t *mtfDecode(uint8_t *buffer, uint32_t symbolCount) {
    if (!buffer || !symbolCount) {
        fprintf(stderr, "MTF_D: Invalid arguments\n");
        return NULL;
    }

    if (!(head = newListNode(0))) { return NULL; }
    if (!initCharset(head, symbolCount)) { return NULL; }

    for (register uint32_t i = 0; i < symbolCount; i++) {
        head = seeIndex(head, buffer[i]);
    }
    return mbuffer;
}

