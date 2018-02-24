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
JNIEXPORT jint JNICALL Java_pebble_shrink_Compression_dcrzCompress
        (JNIEnv *env, jobject obj, jstring _input, jstring _output) {

    const char *input = (*env)->GetStringUTFChars(env, _input, 0);
    const char *output = (*env)->GetStringUTFChars(env, _output, 0);

    int error = 0;

    in = fopen(input, "rb");
    MY_MALLOC(ibuffer, uint8_t*, BUFFER_SIZE, sizeof(uint8_t))
    out = fopen(output, "ab");

    if (!in || !out || !ibuffer) {
        fprintf(stderr, "File open failed in: %p out: %p\ninput: %s\n output: %s buffer %p\n",
                in, out, input, output, ibuffer);
        (*env)->ReleaseStringUTFChars(env, _input, input);
        (*env)->ReleaseStringUTFChars(env,_output, output);
        return ERROR;
    }
    error = dcrzEncode();

    MY_FREE(ibuffer);
    fclose(in);
    fclose(out);
    (*env)->ReleaseStringUTFChars(env, _output, output);
    (*env)->ReleaseStringUTFChars(env, _input, input);

    return error;
}

/**
 * dcrz function to encode or decode stream
 * @param env java environment
 * @param obj Current java object
 * @param _mode compression or decompression mode
 * @param _input input file name
 * @return error code. 0 for no error
 */
JNIEXPORT jint JNICALL Java_pebble_shrink_Compression_dcrzDecompress
        (JNIEnv *env, jobject obj, jstring _input, jstring _output) {
    // Decompression
    const char *input = (*env)->GetStringUTFChars(env, _input, 0);
    const char *output = (*env)->GetStringUTFChars(env,_output,0);
    int error = 0;

    in = fopen(input, "rb");
    MY_MALLOC(ibuffer, uint8_t*, BUFFER_SIZE, sizeof(uint8_t))
    out = fopen(output, "wb");

    if (!in || !out || !ibuffer) {
        fprintf(stderr, "File open failed in: %p out: %p\ninput: %s\n output: %s buffer %p\n",
                in, out, input, output, ibuffer);
        (*env)->ReleaseStringUTFChars(env, _input, input);
        (*env)->ReleaseStringUTFChars(env, _output, output);
        return ERROR;
    }

    error = dcrzDecode();

    MY_FREE(ibuffer);
    fclose(in);
    fclose(out);
    (*env)->ReleaseStringUTFChars(env, _input, input);
    (*env)->ReleaseStringUTFChars(env, _output, output);

    return error;
}