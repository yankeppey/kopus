/**
 * JNI bindings for Opus DRED (Deep Redundancy) functionality.
 *
 * These functions are only available when Opus is compiled with --enable-dred.
 * The kopus-full artifact includes these functions; the base kopus artifact does not.
 */

#include <jni.h>
#include <opus.h>
#include <stdlib.h>

#define PKG eu_buney_kopus
#define JNI_PASTE(p,c,m) Java_##p##_##c##_##m
#define JNI_FN(p,c,m)    JNI_PASTE(p,c,m)

/* ========================================================================== */
/* OpusDRED JNI functions                                                     */
/* ========================================================================== */

JNIEXPORT jlong JNICALL
JNI_FN(PKG, OpusDRED, nativeAlloc)(JNIEnv* env, jobject thiz)
{
    int err = 0;
    OpusDRED* dred = opus_dred_alloc(&err);
    return (err == OPUS_OK && dred) ? (jlong)dred : 0L;
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusDRED, nativeFree)(JNIEnv* env, jobject thiz, jlong ptr)
{
    if (ptr) opus_dred_free((OpusDRED*)ptr);
}

/* ========================================================================== */
/* OpusDREDDecoder JNI functions                                              */
/* ========================================================================== */

JNIEXPORT jlong JNICALL
JNI_FN(PKG, OpusDREDDecoder, nativeCreate)(JNIEnv* env, jobject thiz)
{
    int err = 0;
    OpusDREDDecoder* dec = opus_dred_decoder_create(&err);
    return (err == OPUS_OK && dec) ? (jlong)dec : 0L;
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusDREDDecoder, nativeDestroy)(JNIEnv* env, jobject thiz, jlong ptr)
{
    if (ptr) opus_dred_decoder_destroy((OpusDREDDecoder*)ptr);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDREDDecoder, nativeParse)(JNIEnv* env, jobject thiz,
                                          jlong ptr, jlong dredPtr,
                                          jbyteArray dataArr, jint dataOffset, jint len,
                                          jint maxDredSamples, jint samplingRate,
                                          jintArray dredEndArr, jint deferProcessing)
{
    if (!ptr || !dredPtr || !dataArr || !dredEndArr) return OPUS_BAD_ARG;

    OpusDREDDecoder* dec = (OpusDREDDecoder*)ptr;
    OpusDRED* dred = (OpusDRED*)dredPtr;

    jbyte* data = (*env)->GetByteArrayElements(env, dataArr, NULL);
    jint* dredEnd = (*env)->GetIntArrayElements(env, dredEndArr, NULL);

    int dredEndVal = 0;
    int result = opus_dred_parse(dec, dred,
                                  (const unsigned char*)(data + dataOffset), len,
                                  maxDredSamples, samplingRate,
                                  &dredEndVal, deferProcessing);

    dredEnd[0] = dredEndVal;

    (*env)->ReleaseByteArrayElements(env, dataArr, data, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, dredEndArr, dredEnd, 0);

    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDREDDecoder, nativeProcess)(JNIEnv* env, jobject thiz,
                                            jlong ptr, jlong srcDred, jlong dstDred)
{
    if (!ptr || !srcDred || !dstDred) return OPUS_BAD_ARG;

    OpusDREDDecoder* dec = (OpusDREDDecoder*)ptr;
    return opus_dred_process(dec, (const OpusDRED*)srcDred, (OpusDRED*)dstDred);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDREDDecoder, nativeCtl)(JNIEnv* env, jobject thiz,
                                        jlong ptr, jint request, jint value)
{
    if (!ptr) return OPUS_BAD_ARG;
    OpusDREDDecoder* dec = (OpusDREDDecoder*)ptr;
    return opus_dred_decoder_ctl(dec, request, value);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDREDDecoder, nativeCtlQuery)(JNIEnv* env, jobject thiz,
                                             jlong ptr, jint request)
{
    if (!ptr) return OPUS_BAD_ARG;
    OpusDREDDecoder* dec = (OpusDREDDecoder*)ptr;
    int value;
    int result = opus_dred_decoder_ctl(dec, request, &value);
    return (result >= 0) ? value : result;
}

/* ========================================================================== */
/* OpusDecoder DRED decode functions                                          */
/* ========================================================================== */

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDecoder, nativeDecodeDredShort)(JNIEnv* env, jobject thiz,
                                                 jlong ptr, jlong dredPtr,
                                                 jint dredOffset,
                                                 jshortArray outPcmArr, jint outPcmOffset,
                                                 jint frameSize)
{
    if (!ptr || !dredPtr || !outPcmArr) return OPUS_BAD_ARG;

    OpusDecoder* dec = (OpusDecoder*)ptr;
    OpusDRED* dred = (OpusDRED*)dredPtr;

    jshort* outPcm = (*env)->GetShortArrayElements(env, outPcmArr, NULL);

    int result = opus_decoder_dred_decode(dec, dred, dredOffset,
                                          (opus_int16*)(outPcm + outPcmOffset), frameSize);

    (*env)->ReleaseShortArrayElements(env, outPcmArr, outPcm, 0);

    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDecoder, nativeDecodeDredFloat)(JNIEnv* env, jobject thiz,
                                                 jlong ptr, jlong dredPtr,
                                                 jint dredOffset,
                                                 jfloatArray outPcmArr, jint outPcmOffset,
                                                 jint frameSize)
{
    if (!ptr || !dredPtr || !outPcmArr) return OPUS_BAD_ARG;

    OpusDecoder* dec = (OpusDecoder*)ptr;
    OpusDRED* dred = (OpusDRED*)dredPtr;

    jfloat* outPcm = (*env)->GetFloatArrayElements(env, outPcmArr, NULL);

    int result = opus_decoder_dred_decode_float(dec, dred, dredOffset,
                                                 (float*)(outPcm + outPcmOffset), frameSize);

    (*env)->ReleaseFloatArrayElements(env, outPcmArr, outPcm, 0);

    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusDecoder, nativeDecodeDred24)(JNIEnv* env, jobject thiz,
                                              jlong ptr, jlong dredPtr,
                                              jint dredOffset,
                                              jintArray outPcmArr, jint outPcmOffset,
                                              jint frameSize)
{
    if (!ptr || !dredPtr || !outPcmArr) return OPUS_BAD_ARG;

    OpusDecoder* dec = (OpusDecoder*)ptr;
    OpusDRED* dred = (OpusDRED*)dredPtr;

    jint* outPcm = (*env)->GetIntArrayElements(env, outPcmArr, NULL);

    int result = opus_decoder_dred_decode24(dec, dred, dredOffset,
                                             (opus_int32*)(outPcm + outPcmOffset), frameSize);

    (*env)->ReleaseIntArrayElements(env, outPcmArr, outPcm, 0);

    return result;
}
