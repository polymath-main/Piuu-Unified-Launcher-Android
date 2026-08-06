#ifndef PIUU_CORE_H
#define PIUU_CORE_H

#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <sys/sysinfo.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <time.h>
#include <android/log.h>

#define LOG_TAG "PiuuNativeCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#ifdef __cplusplus
extern "C" {
#endif

// Core Allocation Node
typedef struct MemNode {
    char key[128];
    size_t size;
    void* ptr;
    struct MemNode* next;
} MemNode;

// Native Engine Function Headers
JNIEXPORT jboolean JNICALL Java_com_piuu_launcher_repository_LibC_nativeInit(JNIEnv* env, jobject thiz);
JNIEXPORT jboolean JNICALL Java_com_piuu_launcher_repository_LibC_nativeMalloc(JNIEnv* env, jobject thiz, jstring jid, jint sizeInBytes);
JNIEXPORT void JNICALL Java_com_piuu_launcher_repository_LibC_nativeFree(JNIEnv* env, jobject thiz, jstring jid);
JNIEXPORT jlong JNICALL Java_com_piuu_launcher_repository_LibC_nativeGetTotalAllocatedMemory(JNIEnv* env, jobject thiz);
JNIEXPORT jdouble JNICALL Java_com_piuu_launcher_repository_LibC_nativeGetCpuUsage(JNIEnv* env, jobject thiz);
JNIEXPORT jdoubleArray JNICALL Java_com_piuu_launcher_repository_LibC_nativeGetMemInfo(JNIEnv* env, jobject thiz);
JNIEXPORT jint JNICALL Java_com_piuu_launcher_repository_LibC_nativeGetThreadCount(JNIEnv* env, jobject thiz);
JNIEXPORT jint JNICALL Java_com_piuu_launcher_repository_LibC_nativeKillProcess(JNIEnv* env, jobject thiz, jstring jpackageName, jint pid);

#ifdef __cplusplus
}
#endif

#endif // PIUU_CORE_H
