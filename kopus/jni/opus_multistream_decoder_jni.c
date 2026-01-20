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
JNI_FN(PKG, OpusMultistreamDecoder, nativeCreate)(JNIEnv* env, jobject thiz,
                                                   jint sampleRate, jint channels,
                                                   jint streams, jint coupledStreams,
                                                   jbyteArray mappingArr)
{
    if (mappingArr == NULL) return 0L;

    jbyte* mapping = (*env)->GetByteArrayElements(env, mappingArr, NULL);
    if (mapping == NULL) return 0L;

    int err = 0;
    OpusMSDecoder* dec = opus_multistream_decoder_create(
        sampleRate, channels, streams, coupledStreams,
        (const unsigned char*)mapping, &err
    );

    (*env)->ReleaseByteArrayElements(env, mappingArr, mapping, JNI_ABORT);

    return (err == OPUS_OK && dec != NULL) ? (jlong)dec : 0L;
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusMultistreamDecoder, nativeDestroy)(JNIEnv* env, jobject thiz, jlong handle)
{
    if (handle != 0L) {
        opus_multistream_decoder_destroy((OpusMSDecoder*)handle);
    }
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamDecoder, nativeDecodeShortOffset)(JNIEnv* env, jobject thiz,
                                                              jlong handle, jbyteArray inData,
                                                              jint inDataOffset, jint len,
                                                              jshortArray outPcm, jint outPcmOffset,
                                                              jint frameSize, jint decodeFec)
{
    if (handle == 0L || outPcm == NULL) return -1;

    OpusMSDecoder* dec = (OpusMSDecoder*)handle;
    jbyte* inBuf = NULL;
    const unsigned char* pkt = NULL;

    if (inData != NULL) {
        inBuf = (*env)->GetByteArrayElements(env, inData, NULL);
        pkt = (const unsigned char*)(inBuf + inDataOffset);
    }

    jshort* outBuf = (*env)->GetShortArrayElements(env, outPcm, NULL);
    opus_int16* pcm = (opus_int16*)(outBuf + outPcmOffset);

    int samples = opus_multistream_decode(dec, pkt, len, pcm, frameSize, decodeFec);

    if (inBuf != NULL) {
        (*env)->ReleaseByteArrayElements(env, inData, inBuf, JNI_ABORT);
    }
    (*env)->ReleaseShortArrayElements(env, outPcm, outBuf, 0);

    return samples;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamDecoder, nativeDecodeFloatOffset)(JNIEnv* env, jobject thiz,
                                                              jlong handle, jbyteArray inData,
                                                              jint inDataOffset, jint len,
                                                              jfloatArray outPcm, jint outPcmOffset,
                                                              jint frameSize, jint decodeFec)
{
    if (handle == 0L || outPcm == NULL) return -1;

    OpusMSDecoder* dec = (OpusMSDecoder*)handle;
    jbyte* inBuf = NULL;
    const unsigned char* pkt = NULL;

    if (inData != NULL) {
        inBuf = (*env)->GetByteArrayElements(env, inData, NULL);
        pkt = (const unsigned char*)(inBuf + inDataOffset);
    }

    jfloat* outBuf = (*env)->GetFloatArrayElements(env, outPcm, NULL);
    float* pcm = outBuf + outPcmOffset;

    int samples = opus_multistream_decode_float(dec, pkt, len, pcm, frameSize, decodeFec);

    if (inBuf != NULL) {
        (*env)->ReleaseByteArrayElements(env, inData, inBuf, JNI_ABORT);
    }
    (*env)->ReleaseFloatArrayElements(env, outPcm, outBuf, 0);

    return samples;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamDecoder, nativeCtl)(JNIEnv* env, jobject thiz,
                                                jlong handle, jint request, jint value)
{
    if (handle == 0L) return -1;
    OpusMSDecoder* dec = (OpusMSDecoder*)handle;
    return opus_multistream_decoder_ctl(dec, request, value);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusMultistreamDecoder, nativeCtlQuery)(JNIEnv* env, jobject thiz,
                                                     jlong handle, jint request)
{
    if (handle == 0L) return -1;
    OpusMSDecoder* dec = (OpusMSDecoder*)handle;
    int value;
    int result = opus_multistream_decoder_ctl(dec, request, &value);
    if (result >= 0) {
        return value;
    } else {
        return result;
    }
}
