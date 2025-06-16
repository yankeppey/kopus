#include <jni.h>
#include <opus.h>
#include <stdlib.h>

#define PKG eu_buney_kopus
#define JNI_PASTE(p,c,m) Java_##p##_##c##_##m
#define JNI_FN(p,c,m)    JNI_PASTE(p,c,m)

JNIEXPORT jlong JNICALL
JNI_FN(PKG, OpusEncoder, nativeCreate)(JNIEnv* env, jobject thiz,
                                       jint sampleRate, jint channels, jint application)
{
    int err = 0;
    OpusEncoder* enc = opus_encoder_create(sampleRate, channels, application, &err);
    return (err == OPUS_OK && enc) ? (jlong)enc : 0L;
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusEncoder, nativeDestroy)(JNIEnv* env, jobject thiz, jlong ptr)
{
    if (ptr) opus_encoder_destroy((OpusEncoder*)ptr);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusEncoder, nativeEncodeShortOffset)(JNIEnv* env, jobject thiz,
                                                  jlong ptr, jshortArray pcmArr, jint pcmOffset, jint frame,
                                                  jbyteArray outArr, jint outOffset, jint maxBytes)
{
    if (!ptr || !pcmArr || !outArr) return -1;
    OpusEncoder* enc = (OpusEncoder*)ptr;
    jshort* pcm = (*env)->GetShortArrayElements(env, pcmArr, NULL);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);
    int n = opus_encode(enc, (const opus_int16*)(pcm + pcmOffset), frame,
                       (unsigned char*)(out + outOffset), maxBytes);
    (*env)->ReleaseShortArrayElements(env, pcmArr, pcm, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);
    return n;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusEncoder, nativeEncodeFloatOffset)(JNIEnv* env, jobject thiz,
                                                  jlong ptr, jfloatArray pcmArr, jint pcmOffset, jint frame,
                                                  jbyteArray outArr, jint outOffset, jint maxBytes)
{
    if (!ptr || !pcmArr || !outArr) return -1;
    OpusEncoder* enc = (OpusEncoder*)ptr;
    jfloat* pcm = (*env)->GetFloatArrayElements(env, pcmArr, NULL);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);
    int n = opus_encode_float(enc, (const float*)(pcm + pcmOffset), frame,
                             (unsigned char*)(out + outOffset), maxBytes);
    (*env)->ReleaseFloatArrayElements(env, pcmArr, pcm, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);
    return n;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusEncoder, nativeCtl)(JNIEnv *env, jobject thiz,
                                   jlong ptr, jint request, jint value)
{
    if (!ptr) return -1;
    OpusEncoder *encoder = (OpusEncoder *)ptr;
    return opus_encoder_ctl(encoder, request, value);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusEncoder, nativeCtlQuery)(JNIEnv *env, jobject thiz,
                                        jlong ptr, jint request)
{
    if (!ptr) return -1;
    OpusEncoder *encoder = (OpusEncoder *)ptr;
    int value;
    int result = opus_encoder_ctl(encoder, request, &value);
    if (result >= 0) {
        return value;
    } else {
        return result;
    }
}