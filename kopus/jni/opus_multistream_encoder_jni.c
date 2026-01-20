/*
 * Copyright (c) 2025 Andrei Buneyeu
 *
 * This file is part of Kopus and is released under the MIT License.
 * See the LICENSE file for details.
 */

#include <jni.h>
#include <opus.h>
#include <opus_multistream.h>
#include <stdlib.h>

#define PKG eu_buney_kopus
#define JNI_PASTE(p,c,m) Java_##p##_##c##_##m
#define JNI_FN(p,c,m)    JNI_PASTE(p,c,m)


JNIEXPORT jlong JNICALL
JNI_FN(PKG, OpusMultistreamEncoder, nativeCreate)(JNIEnv* env, jobject thiz,
                                                   jint sampleRate, jint channels,
                                                   jint streams, jint coupledStreams,
                                                   jbyteArray mappingArr, jint application)
{
    if (mappingArr == NULL) return 0L;

    jbyte* mapping = (*env)->GetByteArrayElements(env, mappingArr, NULL);
    if (mapping == NULL) return 0L;

    int err = 0;
    OpusMSEncoder* enc = opus_multistream_encoder_create(
        sampleRate, channels, streams, coupledStreams,
        (const unsigned char*)mapping, application, &err
    );

    (*env)->ReleaseByteArrayElements(env, mappingArr, mapping, JNI_ABORT);

    return (err == OPUS_OK && enc != NULL) ? (jlong)enc : 0L;
}

/* Called from companion object via @JvmStatic */
JNIEXPORT jlongArray JNICALL
JNI_FN(PKG, OpusMultistreamEncoder, nativeCreateSurroundStatic)(JNIEnv* env, jclass clazz,
                                                                 jint sampleRate, jint channels,
                                                                 jint mappingFamily,
                                                                 jbyteArray mappingArr, jint application)
{
    if (mappingArr == NULL) return NULL;

    jbyte* mapping = (*env)->GetByteArrayElements(env, mappingArr, NULL);
    if (mapping == NULL) return NULL;

    int streams = 0;
    int coupledStreams = 0;
    int err = 0;

    OpusMSEncoder* enc = opus_multistream_surround_encoder_create(
        sampleRate, channels, mappingFamily,
        &streams, &coupledStreams,
        (unsigned char*)mapping, application, &err
    );

    (*env)->ReleaseByteArrayElements(env, mappingArr, mapping, 0);  // Write back mapping

    if (err != OPUS_OK || enc == NULL) {
        return NULL;
    }

    // Return [handle, streams, coupledStreams]
    jlongArray result = (*env)->NewLongArray(env, 3);
    if (result == NULL) {
        opus_multistream_encoder_destroy(enc);
        return NULL;
    }

    jlong resultData[3] = { (jlong)enc, (jlong)streams, (jlong)coupledStreams };
    (*env)->SetLongArrayRegion(env, result, 0, 3, resultData);

    return result;
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusMultistreamEncoder, nativeDestroy)(JNIEnv* env, jobject thiz, jlong handle)
{
    if (handle != 0L) {
        opus_multistream_encoder_destroy((OpusMSEncoder*)handle);
    }
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamEncoder, nativeEncodeShortOffset)(JNIEnv* env, jobject thiz,
                                                              jlong handle, jshortArray pcmArr,
                                                              jint pcmOffset, jint frameSize,
                                                              jbyteArray outArr, jint outOffset,
                                                              jint maxBytes)
{
    if (handle == 0L || pcmArr == NULL || outArr == NULL) return -1;

    OpusMSEncoder* enc = (OpusMSEncoder*)handle;
    jshort* pcm = (*env)->GetShortArrayElements(env, pcmArr, NULL);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);

    int n = opus_multistream_encode(enc, (const opus_int16*)(pcm + pcmOffset), frameSize,
                                    (unsigned char*)(out + outOffset), maxBytes);

    (*env)->ReleaseShortArrayElements(env, pcmArr, pcm, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);

    return n;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamEncoder, nativeEncodeFloatOffset)(JNIEnv* env, jobject thiz,
                                                              jlong handle, jfloatArray pcmArr,
                                                              jint pcmOffset, jint frameSize,
                                                              jbyteArray outArr, jint outOffset,
                                                              jint maxBytes)
{
    if (handle == 0L || pcmArr == NULL || outArr == NULL) return -1;

    OpusMSEncoder* enc = (OpusMSEncoder*)handle;
    jfloat* pcm = (*env)->GetFloatArrayElements(env, pcmArr, NULL);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);

    int n = opus_multistream_encode_float(enc, (const float*)(pcm + pcmOffset), frameSize,
                                          (unsigned char*)(out + outOffset), maxBytes);

    (*env)->ReleaseFloatArrayElements(env, pcmArr, pcm, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);

    return n;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamEncoder, nativeEncode24Offset)(JNIEnv* env, jobject thiz,
                                                           jlong handle, jintArray pcmArr,
                                                           jint pcmOffset, jint frameSize,
                                                           jbyteArray outArr, jint outOffset,
                                                           jint maxBytes)
{
    if (handle == 0L || pcmArr == NULL || outArr == NULL) return -1;

    OpusMSEncoder* enc = (OpusMSEncoder*)handle;
    jint* pcm = (*env)->GetIntArrayElements(env, pcmArr, NULL);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);

    int n = opus_multistream_encode24(enc, (const opus_int32*)(pcm + pcmOffset), frameSize,
                                       (unsigned char*)(out + outOffset), maxBytes);

    (*env)->ReleaseIntArrayElements(env, pcmArr, pcm, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);

    return n;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamEncoder, nativeCtl)(JNIEnv* env, jobject thiz,
                                                jlong handle, jint request, jint value)
{
    if (handle == 0L) return -1;
    OpusMSEncoder* enc = (OpusMSEncoder*)handle;
    return opus_multistream_encoder_ctl(enc, request, value);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamEncoder, nativeCtlQuery)(JNIEnv* env, jobject thiz,
                                                     jlong handle, jint request)
{
    if (handle == 0L) return -1;
    OpusMSEncoder* enc = (OpusMSEncoder*)handle;
    int value;
    int result = opus_multistream_encoder_ctl(enc, request, &value);
    if (result >= 0) {
        return value;
    } else {
        return result;
    }
}
