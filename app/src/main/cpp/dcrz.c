/**
 * dcrz encode/decode a given stream of symbols
 */

#include "dcrz.h"
#include <jni.h>
/**
 * dcrz function to encode or decode stream
 * @param env java environment
 * @param obj Current java object
 * @param _mode compression or decompression mode
 * @param _input input file name
 * @return error code. 0 for no error
 */
JNIEXPORT jint JNICALL Java_pebble_shrink_Compression_dcrz
        (JNIEnv *env, jobject obj, jboolean _mode, jstring _input) {

    const char *input = (*env)->GetStringUTFChars(env, _input, 0);
    int16_t inputLength = strlen(input);
    int error = 0;

    in = fopen(input, "rb");
    MY_MALLOC(ibuffer, uint8_t*, BUFFER_SIZE, sizeof(uint8_t))

    if (_mode) {
        char output[inputLength + 6];  // 6 = length of ".dcrz" + one NULL char
        strcpy(output, input);
        output[inputLength++] = '.';
        output[inputLength++] = 'd';
        output[inputLength++] = 'c';
        output[inputLength++] = 'r';
        output[inputLength++] = 'z';
        output[inputLength] = '\0';

        out = fopen(output, "wb");

        if (!in || !out || !ibuffer) {
            fprintf(stderr, "File open failed in: %p out: %p\ninput: %s\n output: %s buffer %p\n",
                    in, out, input, output, ibuffer);
            (*env)->ReleaseStringUTFChars(env, _input, input);
            return ERROR;
        }
        error = dcrzEncode();

    } else {
        int8_t j = 5, k = 5;
        bool isExt = false;
        char output[inputLength + 2];
        if (input[inputLength - j--] == '.')
            isExt = true;
        else
            isExt = false;
        if (input[inputLength - j--] == 'd')
            isExt = true;
        else
            isExt = false;
        if (input[inputLength - j--] == 'c')
            isExt = true;
        else
            isExt = false;
        if (input[inputLength - j--] == 'r')
            isExt = true;
        else
            isExt = false;
        if (input[inputLength - j--] == 'z')
            isExt = true;
        else
            isExt = false;

        if (isExt) {
            strncpy(output, input, inputLength - k);
            output[inputLength - k] = '\0';
        } else {
            strcpy(output, input);
            output[inputLength] = '_';
            output[inputLength + 1] = '1';
            output[inputLength + 2] = '\0';
        }

        out = fopen(output, "wb");
        if (!in || !out || !ibuffer) {
            fprintf(stderr, "File open failed in: %p out: %p\ninput: %s\n output: %s buffer %p\n",
                    in, out, input, output, ibuffer);
            (*env)->ReleaseStringUTFChars(env, _input, input);
            return ERROR;
        }

        error = dcrzDecode();
    }

    MY_FREE(ibuffer);
    fclose(in);
    fclose(out);
    (*env)->ReleaseStringUTFChars(env, _input, input);

    return error;

}
