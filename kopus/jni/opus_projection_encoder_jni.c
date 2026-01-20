/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */

#include <jni.h>
#include <opus.h>
#include <opus_multistream.h>
#include <opus_projection.h>
#include <stdlib.h>
#include <stdio.h>

#define PKG eu_buney_kopus
#define JNI_PASTE(p,c,m) Java_##p##_##c##_##m
#define JNI_FN(p,c,m)    JNI_PASTE(p,c,m)


/* Called from companion object via @JvmStatic */
JNIEXPORT jlongArray JNICALL
JNI_FN(PKG, OpusProjectionEncoder, nativeCreateAmbisonicsStatic)(JNIEnv* env, jclass clazz,
                                                                   jint sampleRate, jint channels,
                                                                   jint mappingFamily, jint application)
{
    int streams = 0;
    int coupledStreams = 0;
    int err = 0;

    OpusProjectionEncoder* enc = opus_projection_ambisonics_encoder_create(
        sampleRate, channels, mappingFamily,
        &streams, &coupledStreams,
        application, &err
    );

    if (err != OPUS_OK || enc == NULL) {
        // Throw exception with detailed error information
        char errMsg[256];
        snprintf(errMsg, sizeof(errMsg),
                 "opus_projection_ambisonics_encoder_create failed: error=%d (%s), sr=%d, ch=%d, mf=%d, app=%d",
                 err, opus_strerror(err), (int)sampleRate, (int)channels, (int)mappingFamily, (int)application);
        (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/IllegalStateException"), errMsg);
        return NULL;
    }

    // Return [handle, streams, coupledStreams]
    jlongArray result = (*env)->NewLongArray(env, 3);
    if (result == NULL) {
        opus_projection_encoder_destroy(enc);
        return NULL;
    }

    jlong resultData[3] = { (jlong)enc, (jlong)streams, (jlong)coupledStreams };
    (*env)->SetLongArrayRegion(env, result, 0, 3, resultData);

    return result;
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusProjectionEncoder, nativeDestroy)(JNIEnv* env, jobject thiz, jlong handle)
{
    if (handle != 0L) {
        opus_projection_encoder_destroy((OpusProjectionEncoder*)handle);
    }
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusProjectionEncoder, nativeEncodeShortOffset)(JNIEnv* env, jobject thiz,
                                                              jlong handle, jshortArray pcmArr,
                                                              jint pcmOffset, jint frameSize,
                                                              jbyteArray outArr, jint outOffset,
                                                              jint maxBytes)
{
    if (handle == 0L || pcmArr == NULL || outArr == NULL) return -1;

    OpusProjectionEncoder* enc = (OpusProjectionEncoder*)handle;
    jshort* pcm = (*env)->GetShortArrayElements(env, pcmArr, NULL);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);

    int n = opus_projection_encode(enc, (const opus_int16*)(pcm + pcmOffset), frameSize,
                                    (unsigned char*)(out + outOffset), maxBytes);

    (*env)->ReleaseShortArrayElements(env, pcmArr, pcm, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);

    return n;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusProjectionEncoder, nativeEncodeFloatOffset)(JNIEnv* env, jobject thiz,
                                                              jlong handle, jfloatArray pcmArr,
                                                              jint pcmOffset, jint frameSize,
                                                              jbyteArray outArr, jint outOffset,
                                                              jint maxBytes)
{
    if (handle == 0L || pcmArr == NULL || outArr == NULL) return -1;

    OpusProjectionEncoder* enc = (OpusProjectionEncoder*)handle;
    jfloat* pcm = (*env)->GetFloatArrayElements(env, pcmArr, NULL);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);

    int n = opus_projection_encode_float(enc, (const float*)(pcm + pcmOffset), frameSize,
                                          (unsigned char*)(out + outOffset), maxBytes);

    (*env)->ReleaseFloatArrayElements(env, pcmArr, pcm, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);

    return n;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusProjectionEncoder, nativeEncode24Offset)(JNIEnv* env, jobject thiz,
                                                           jlong handle, jintArray pcmArr,
                                                           jint pcmOffset, jint frameSize,
                                                           jbyteArray outArr, jint outOffset,
                                                           jint maxBytes)
{
    if (handle == 0L || pcmArr == NULL || outArr == NULL) return -1;

    OpusProjectionEncoder* enc = (OpusProjectionEncoder*)handle;
    jint* pcm = (*env)->GetIntArrayElements(env, pcmArr, NULL);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);

    int n = opus_projection_encode24(enc, (const opus_int32*)(pcm + pcmOffset), frameSize,
                                       (unsigned char*)(out + outOffset), maxBytes);

    (*env)->ReleaseIntArrayElements(env, pcmArr, pcm, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);

    return n;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusProjectionEncoder, nativeCtl)(JNIEnv* env, jobject thiz,
                                                jlong handle, jint request, jint value)
{
    if (handle == 0L) return -1;
    OpusProjectionEncoder* enc = (OpusProjectionEncoder*)handle;
    return opus_projection_encoder_ctl(enc, request, value);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusProjectionEncoder, nativeCtlQuery)(JNIEnv* env, jobject thiz,
                                                     jlong handle, jint request)
{
    if (handle == 0L) return -1;
    OpusProjectionEncoder* enc = (OpusProjectionEncoder*)handle;
    int value;
    int result = opus_projection_encoder_ctl(enc, request, &value);
    if (result >= 0) {
        return value;
    } else {
        return result;
    }
}

JNIEXPORT jbyteArray JNICALL
JNI_FN(PKG, OpusProjectionEncoder, nativeGetDemixingMatrix)(JNIEnv* env, jobject thiz, jlong handle)
{
    if (handle == 0L) return NULL;

    OpusProjectionEncoder* enc = (OpusProjectionEncoder*)handle;

    // First get the size
    opus_int32 size;
    int result = opus_projection_encoder_ctl(enc, OPUS_PROJECTION_GET_DEMIXING_MATRIX_SIZE_REQUEST, &size);
    if (result < 0 || size <= 0) {
        return NULL;
    }

    // Allocate buffer and get matrix
    unsigned char* matrix = (unsigned char*)malloc(size);
    if (matrix == NULL) {
        return NULL;
    }

    result = opus_projection_encoder_ctl(enc, OPUS_PROJECTION_GET_DEMIXING_MATRIX_REQUEST, matrix, size);
    if (result < 0) {
        free(matrix);
        return NULL;
    }

    // Create Java byte array and copy data
    jbyteArray jMatrix = (*env)->NewByteArray(env, size);
    if (jMatrix == NULL) {
        free(matrix);
        return NULL;
    }

    (*env)->SetByteArrayRegion(env, jMatrix, 0, size, (jbyte*)matrix);
    free(matrix);

    return jMatrix;
}
