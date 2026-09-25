#include <jni.h>
#include <android/log.h>
#define LOG_TAG "DJProDemucs"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" {
    JNIEXPORT jboolean JNICALL
    Java_com_djpro_native_DemucsBridge_isModelReady(JNIEnv*, jobject) {
        return false;
    }
    JNIEXPORT jstring JNICALL
    Java_com_djpro_native_DemucsBridge_separate(JNIEnv* env, jobject, jstring path) {
        (void)path;
        return env->NewStringUTF("not_implemented");
    }
}
