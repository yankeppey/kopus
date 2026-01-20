#include <jni.h>
#include <opus.h>
#include <stdlib.h>
#include <string.h>

#define PKG eu_buney_kopus
#define JNI_PASTE(p,c,m) Java_##p##_##c##_##m
#define JNI_FN(p,c,m)    JNI_PASTE(p,c,m)

/**
 * Internal structure to hold repacketizer and copied packet data.
 * The Opus repacketizer stores pointers to input packet data, so we must
 * keep copies of all packets until init() or destroy() is called.
 */
typedef struct {
    OpusRepacketizer* rp;
    unsigned char** packets;
    int* packet_lens;
    int packet_count;
    int packet_capacity;
} RepacketizerState;

static void free_packet_copies(RepacketizerState* state) {
    if (state->packets) {
        for (int i = 0; i < state->packet_count; i++) {
            free(state->packets[i]);
        }
        free(state->packets);
        free(state->packet_lens);
        state->packets = NULL;
        state->packet_lens = NULL;
        state->packet_count = 0;
        state->packet_capacity = 0;
    }
}

JNIEXPORT jlong JNICALL
JNI_FN(PKG, OpusRepacketizer, nativeCreate)(JNIEnv* env, jobject thiz) {
    RepacketizerState* state = (RepacketizerState*)calloc(1, sizeof(RepacketizerState));
    if (!state) return 0L;

    state->rp = opus_repacketizer_create();
    if (!state->rp) {
        free(state);
        return 0L;
    }

    return (jlong)state;
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusRepacketizer, nativeDestroy)(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0L) return;

    RepacketizerState* state = (RepacketizerState*)handle;
    free_packet_copies(state);
    opus_repacketizer_destroy(state->rp);
    free(state);
}

JNIEXPORT void JNICALL
JNI_FN(PKG, OpusRepacketizer, nativeInit)(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0L) return;

    RepacketizerState* state = (RepacketizerState*)handle;
    free_packet_copies(state);
    opus_repacketizer_init(state->rp);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusRepacketizer, nativeCat)(JNIEnv* env, jobject thiz,
                                          jlong handle, jbyteArray data, jint len) {
    if (handle == 0L || data == NULL) return OPUS_BAD_ARG;

    RepacketizerState* state = (RepacketizerState*)handle;

    // Expand packet storage if needed
    if (state->packet_count >= state->packet_capacity) {
        int new_capacity = state->packet_capacity == 0 ? 8 : state->packet_capacity * 2;
        unsigned char** new_packets = (unsigned char**)realloc(
            state->packets, new_capacity * sizeof(unsigned char*));
        int* new_lens = (int*)realloc(state->packet_lens, new_capacity * sizeof(int));
        if (!new_packets || !new_lens) {
            free(new_packets);
            free(new_lens);
            return OPUS_ALLOC_FAIL;
        }
        state->packets = new_packets;
        state->packet_lens = new_lens;
        state->packet_capacity = new_capacity;
    }

    // Copy packet data
    unsigned char* packet_copy = (unsigned char*)malloc(len);
    if (!packet_copy) return OPUS_ALLOC_FAIL;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) {
        free(packet_copy);
        return OPUS_BAD_ARG;
    }
    memcpy(packet_copy, buf, len);
    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);

    // Add to repacketizer
    int result = opus_repacketizer_cat(state->rp, packet_copy, len);

    if (result == OPUS_OK) {
        state->packets[state->packet_count] = packet_copy;
        state->packet_lens[state->packet_count] = len;
        state->packet_count++;
    } else {
        free(packet_copy);
    }

    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusRepacketizer, nativeGetNbFrames)(JNIEnv* env, jobject thiz, jlong handle) {
    if (handle == 0L) return 0;
    RepacketizerState* state = (RepacketizerState*)handle;
    return opus_repacketizer_get_nb_frames(state->rp);
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusRepacketizer, nativeOutRange)(JNIEnv* env, jobject thiz,
                                               jlong handle, jint begin, jint end,
                                               jbyteArray data, jint maxLen) {
    if (handle == 0L || data == NULL) return OPUS_BAD_ARG;

    RepacketizerState* state = (RepacketizerState*)handle;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return OPUS_BAD_ARG;

    opus_int32 result = opus_repacketizer_out_range(
        state->rp, begin, end, (unsigned char*)buf, maxLen);

    (*env)->ReleaseByteArrayElements(env, data, buf, result > 0 ? 0 : JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
JNI_FN(PKG, OpusRepacketizer, nativeOut)(JNIEnv* env, jobject thiz,
                                          jlong handle, jbyteArray data, jint maxLen) {
    if (handle == 0L || data == NULL) return OPUS_BAD_ARG;

    RepacketizerState* state = (RepacketizerState*)handle;

    jbyte* buf = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return OPUS_BAD_ARG;

    opus_int32 result = opus_repacketizer_out(state->rp, (unsigned char*)buf, maxLen);

    (*env)->ReleaseByteArrayElements(env, data, buf, result > 0 ? 0 : JNI_ABORT);
    return result;
}
