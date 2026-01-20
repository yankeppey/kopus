#include <jni.h>
#include <opus.h>
#include <opus_defines.h>
#include <stdlib.h>

#define PKG eu_buney_kopus
#define JNI_PASTE(p,c,m) Java_##p##_##c##_##m
#define JNI_FN(p,c,m)    JNI_PASTE(p,c,m)

/* ---------- OpusPacket inspection functions ---------- */

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativeGetBandwidth)(JNIEnv* env, jobject thiz, jbyteArray data) {
    if (data == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    int result = opus_packet_get_bandwidth((const unsigned char*)buf);
    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativeGetSamplesPerFrame)(JNIEnv* env, jobject thiz,
                                                   jbyteArray data, jint sampleRate) {
    if (data == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    int result = opus_packet_get_samples_per_frame((const unsigned char*)buf, sampleRate);
    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativeGetNbChannels)(JNIEnv* env, jobject thiz, jbyteArray data) {
    if (data == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    int result = opus_packet_get_nb_channels((const unsigned char*)buf);
    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativeGetNbFrames)(JNIEnv* env, jobject thiz,
                                            jbyteArray packet, jint len) {
    if (packet == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, packet, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    int result = opus_packet_get_nb_frames((const unsigned char*)buf, len);
    (*env)->ReleaseByteArrayElements(env, packet, buf, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativeGetNbSamples)(JNIEnv* env, jobject thiz,
                                             jbyteArray packet, jint len, jint sampleRate) {
    if (packet == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, packet, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    int result = opus_packet_get_nb_samples((const unsigned char*)buf, len, sampleRate);
    (*env)->ReleaseByteArrayElements(env, packet, buf, JNI_ABORT);
    return result;
}

JNIEXPORT jboolean JNICALL
JNI_FN(PKG, OpusPacket, nativeHasLbrr)(JNIEnv* env, jobject thiz,
                                        jbyteArray packet, jint len) {
    if (packet == NULL) return JNI_FALSE;

    jbyte* buf = (*env)->GetByteArrayElements(env, packet, NULL);
    if (buf == NULL) return JNI_FALSE;

    int result = opus_packet_has_lbrr((const unsigned char*)buf, len);
    (*env)->ReleaseByteArrayElements(env, packet, buf, JNI_ABORT);
    return (result == 1) ? JNI_TRUE : JNI_FALSE;
}

/* ---------- OpusPacket padding functions ---------- */

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativePad)(JNIEnv* env, jobject thiz,
                                    jbyteArray data, jint len, jint newLen) {
    if (data == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    int result = opus_packet_pad((unsigned char*)buf, len, newLen);
    (*env)->ReleaseByteArrayElements(env, data, buf, result == OPUS_OK ? 0 : JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativeUnpad)(JNIEnv* env, jobject thiz,
                                      jbyteArray data, jint len) {
    if (data == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    opus_int32 result = opus_packet_unpad((unsigned char*)buf, len);
    (*env)->ReleaseByteArrayElements(env, data, buf, result > 0 ? 0 : JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativePadMultistream)(JNIEnv* env, jobject thiz,
                                               jbyteArray data, jint len,
                                               jint newLen, jint nbStreams) {
    if (data == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    int result = opus_multistream_packet_pad((unsigned char*)buf, len, newLen, nbStreams);
    (*env)->ReleaseByteArrayElements(env, data, buf, result == OPUS_OK ? 0 : JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusPacket, nativeUnpadMultistream)(JNIEnv* env, jobject thiz,
                                                 jbyteArray data, jint len, jint nbStreams) {
    if (data == NULL) return OPUS_BAD_ARG;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (buf == NULL) return OPUS_BAD_ARG;

    opus_int32 result = opus_multistream_packet_unpad((unsigned char*)buf, len, nbStreams);
    (*env)->ReleaseByteArrayElements(env, data, buf, result > 0 ? 0 : JNI_ABORT);
    return result;
}

/* ---------- OpusPacket parse function ---------- */

JNIEXPORT jobject JNICALL
JNI_FN(PKG, OpusPacket, nativeParse)(JNIEnv* env, jobject thiz,
                                      jbyteArray packet, jint len) {
    if (packet == NULL || len < 1) return NULL;

    jbyte* data = (*env)->GetByteArrayElements(env, packet, NULL);
    if (data == NULL) return NULL;

    unsigned char toc;
    const unsigned char *frames[48];
    opus_int16 sizes[48];
    int payload_offset;

    int num_frames = opus_packet_parse(
        (const unsigned char*)data, len,
        &toc, frames, sizes, &payload_offset
    );

    if (num_frames < 1) {
        (*env)->ReleaseByteArrayElements(env, packet, data, JNI_ABORT);
        return NULL;
    }

    /* Convert frame pointers to offsets */
    jint offsets[48];
    jint jsizes[48];
    for (int i = 0; i < num_frames; i++) {
        offsets[i] = (jint)(frames[i] - (const unsigned char*)data);
        jsizes[i] = (jint)sizes[i];
    }

    (*env)->ReleaseByteArrayElements(env, packet, data, JNI_ABORT);

    /* Create IntArrays for offsets and sizes */
    jintArray frameOffsetsArray = (*env)->NewIntArray(env, num_frames);
    jintArray frameSizesArray = (*env)->NewIntArray(env, num_frames);
    if (frameOffsetsArray == NULL || frameSizesArray == NULL) return NULL;

    (*env)->SetIntArrayRegion(env, frameOffsetsArray, 0, num_frames, offsets);
    (*env)->SetIntArrayRegion(env, frameSizesArray, 0, num_frames, jsizes);

    /* Find and instantiate PacketFrameInfo class */
    jclass clazz = (*env)->FindClass(env, "eu/buney/kopus/PacketFrameInfo");
    if (clazz == NULL) return NULL;

    jmethodID constructor = (*env)->GetMethodID(env, clazz, "<init>", "(BI[I[II)V");
    if (constructor == NULL) return NULL;

    jobject result = (*env)->NewObject(env, clazz, constructor,
        (jbyte)toc,
        (jint)num_frames,
        frameOffsetsArray,
        frameSizesArray,
        (jint)payload_offset
    );

    return result;
}
