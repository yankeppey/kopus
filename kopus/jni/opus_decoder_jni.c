
#include <jni.h>
#include <opus.h>
#include <stdlib.h>


#define PKG eu_buney_kopus
#define JNI_GLUE(pkg, cls, name) Java_##pkg##_##cls##_##name
#define JNI_FN(pkg, cls, name)   JNI_GLUE(pkg, cls, name)


JNIEXPORT jlong JNICALL
JNI_FN(PKG, OpusDecoder, nativeCreate)(JNIEnv* env, jobject thiz,
                                       jint sampleRate, jint channels)
{
    int err = 0;
    OpusDecoder* dec = opus_decoder_create(sampleRate, channels, &err);
    return (err == OPUS_OK && dec != NULL) ? (jlong)dec : 0L;
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusDecoder, nativeDestroy)(JNIEnv* env, jobject thiz, jlong handle)
{
    if (handle != 0L) {
        opus_decoder_destroy((OpusDecoder*)handle);
    }
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDecoder, nativeDecodeShortOffset)(JNIEnv* env, jobject thiz, jlong handle,
                                                  jbyteArray inData, jint inDataOffset, jint len,
                                                  jshortArray outPcm, jint outPcmOffset, jint frameSize, jint decodeFec)
{
    if (handle == 0L || outPcm == NULL) return -1;

    OpusDecoder* dec = (OpusDecoder*)handle;
    jbyte* inBuf = NULL;
    const unsigned char* pkt = NULL;

    if (inData != NULL) {
        inBuf = (*env)->GetByteArrayElements(env, inData, NULL);
        pkt = (const unsigned char*)(inBuf + inDataOffset);
    }

    jshort* outBuf = (*env)->GetShortArrayElements(env, outPcm, NULL);
    opus_int16* pcm = (opus_int16*)(outBuf + outPcmOffset);

    int samples = opus_decode(dec, pkt, len, pcm, frameSize, decodeFec);

    if (inBuf != NULL) {
        (*env)->ReleaseByteArrayElements(env, inData, inBuf, JNI_ABORT);
    }
    (*env)->ReleaseShortArrayElements(env, outPcm, outBuf, 0);

    return samples;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDecoder, nativeDecodeFloatOffset)(JNIEnv* env, jobject thiz, jlong handle,
                                                  jbyteArray inData, jint inDataOffset, jint len,
                                                  jfloatArray outPcm, jint outPcmOffset, jint frameSize, jint decodeFec)
{
    if (handle == 0L || outPcm == NULL) return -1;

    OpusDecoder* dec = (OpusDecoder*)handle;
    jbyte* inBuf = NULL;
    const unsigned char* pkt = NULL;

    if (inData != NULL) {
        inBuf = (*env)->GetByteArrayElements(env, inData, NULL);
        pkt = (const unsigned char*)(inBuf + inDataOffset);
    }

    jfloat* outBuf = (*env)->GetFloatArrayElements(env, outPcm, NULL);
    float* pcm = outBuf + outPcmOffset;

    int samples = opus_decode_float(dec, pkt, len, pcm, frameSize, decodeFec);

    if (inBuf != NULL) {
        (*env)->ReleaseByteArrayElements(env, inData, inBuf, JNI_ABORT);
    }
    (*env)->ReleaseFloatArrayElements(env, outPcm, outBuf, 0);

    return samples;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDecoder, nativeCtl)(JNIEnv *env, jobject thiz,
                                   jlong ptr, jint request, jint value)
{
    if (!ptr) return -1;
    OpusDecoder *decoder = (OpusDecoder *)ptr;
    return opus_decoder_ctl(decoder, request, value);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDecoder, nativeCtlQuery)(JNIEnv *env, jobject thiz,
                                        jlong ptr, jint request)
{
    if (!ptr) return -1;
    OpusDecoder *decoder = (OpusDecoder *)ptr;
    int value;
    int result = opus_decoder_ctl(decoder, request, &value);
    if (result >= 0) {
        return value;
    } else {
        return result;
    }
}
