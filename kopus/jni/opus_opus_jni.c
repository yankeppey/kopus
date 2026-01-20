#include <jni.h>
#include <opus.h>
#include <opus_defines.h>
#include <stdlib.h>

#define PKG eu_buney_kopus
#define JNI_PASTE(p,c,m) Java_##p##_##c##_##m
#define JNI_FN(p,c,m)    JNI_PASTE(p,c,m)

JNIEXPORT jstring JNICALL
JNI_FN(PKG, Opus, getOpusVersion)(JNIEnv* env, jobject thiz) {
    const char* version = opus_get_version_string();
    if (!version) {
        jclass exception = (*env)->FindClass(env, "java/lang/IllegalStateException");
        if (exception) {
            (*env)->ThrowNew(env, exception, "opus_get_version_string() returned NULL");
        }
        return NULL;
    }
    return (*env)->NewStringUTF(env, version);
}
