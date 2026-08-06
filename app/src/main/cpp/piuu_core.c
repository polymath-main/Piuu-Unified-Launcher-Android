#include "piuu_core.h"

static MemNode* g_head = NULL;
static pthread_mutex_t g_mem_mutex = PTHREAD_MUTEX_INITIALIZER;
static size_t g_total_allocated = 0;

// CPU Calculation State variables
static unsigned long long g_prev_user = 0, g_prev_nice = 0, g_prev_system = 0;
static unsigned long long g_prev_idle = 0, g_prev_iowait = 0, g_prev_irq = 0, g_prev_softirq = 0;

JNIEXPORT jboolean JNICALL
Java_com_piuu_launcher_repository_LibC_nativeInit(JNIEnv* env, jobject thiz) {
    LOGI("Piuu Native Core Engine initialized successfully [POSIX C runtime].");
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_com_piuu_launcher_repository_LibC_nativeMalloc(JNIEnv* env, jobject thiz, jstring jid, jint sizeInBytes) {
    if (!jid || sizeInBytes <= 0) return JNI_FALSE;

    const char* id = (*env)->GetStringUTFChars(env, jid, NULL);
    if (!id) return JNI_FALSE;

    pthread_mutex_lock(&g_mem_mutex);

    // If key exists, free old allocation first
    MemNode* curr = g_head;
    MemNode* prev = NULL;
    while (curr) {
        if (strcmp(curr->key, id) == 0) {
            if (prev) prev->next = curr->next;
            else g_head = curr->next;

            if (g_total_allocated >= curr->size) {
                g_total_allocated -= curr->size;
            }
            free(curr->ptr);
            free(curr);
            break;
        }
        prev = curr;
        curr = curr->next;
    }

    void* memory = malloc((size_t)sizeInBytes);
    if (!memory) {
        pthread_mutex_unlock(&g_mem_mutex);
        (*env)->ReleaseStringUTFChars(env, jid, id);
        LOGE("nativeMalloc failed for %s (%d bytes)", id, sizeInBytes);
        return JNI_FALSE;
    }

    memset(memory, 0, (size_t)sizeInBytes);

    MemNode* new_node = (MemNode*)malloc(sizeof(MemNode));
    if (!new_node) {
        free(memory);
        pthread_mutex_unlock(&g_mem_mutex);
        (*env)->ReleaseStringUTFChars(env, jid, id);
        return JNI_FALSE;
    }

    strncpy(new_node->key, id, sizeof(new_node->key) - 1);
    new_node->key[sizeof(new_node->key) - 1] = '\0';
    new_node->size = (size_t)sizeInBytes;
    new_node->ptr = memory;
    new_node->next = g_head;
    g_head = new_node;

    g_total_allocated += (size_t)sizeInBytes;

    pthread_mutex_unlock(&g_mem_mutex);

    LOGD("nativeMalloc(%s, %d bytes) - Native heap allocation success at %p", id, sizeInBytes, memory);

    (*env)->ReleaseStringUTFChars(env, jid, id);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_piuu_launcher_repository_LibC_nativeFree(JNIEnv* env, jobject thiz, jstring jid) {
    if (!jid) return;
    const char* id = (*env)->GetStringUTFChars(env, jid, NULL);
    if (!id) return;

    pthread_mutex_lock(&g_mem_mutex);

    MemNode* curr = g_head;
    MemNode* prev = NULL;

    while (curr) {
        if (strcmp(curr->key, id) == 0) {
            if (prev) prev->next = curr->next;
            else g_head = curr->next;

            if (g_total_allocated >= curr->size) {
                g_total_allocated -= curr->size;
            }
            free(curr->ptr);
            free(curr);
            LOGD("nativeFree(%s) - Successfully freed native memory", id);
            break;
        }
        prev = curr;
        curr = curr->next;
    }

    pthread_mutex_unlock(&g_mem_mutex);
    (*env)->ReleaseStringUTFChars(env, jid, id);
}

JNIEXPORT jlong JNICALL
Java_com_piuu_launcher_repository_LibC_nativeGetTotalAllocatedMemory(JNIEnv* env, jobject thiz) {
    pthread_mutex_lock(&g_mem_mutex);
    jlong total = (jlong)g_total_allocated;
    pthread_mutex_unlock(&g_mem_mutex);
    return total;
}

JNIEXPORT jdouble JNICALL
Java_com_piuu_launcher_repository_LibC_nativeGetCpuUsage(JNIEnv* env, jobject thiz) {
    FILE* file = fopen("/proc/stat", "r");
    if (!file) {
        return -1.0;
    }

    char buffer[256];
    if (!fgets(buffer, sizeof(buffer), file)) {
        fclose(file);
        return -1.0;
    }
    fclose(file);

    unsigned long long user = 0, nice = 0, system = 0, idle = 0, iowait = 0, irq = 0, softirq = 0;
    if (sscanf(buffer, "cpu  %llu %llu %llu %llu %llu %llu %llu",
               &user, &nice, &system, &idle, &iowait, &irq, &softirq) < 4) {
        return -1.0;
    }

    unsigned long long prev_idle_sum = g_prev_idle + g_prev_iowait;
    unsigned long long idle_sum = idle + iowait;

    unsigned long long prev_non_idle = g_prev_user + g_prev_nice + g_prev_system + g_prev_irq + g_prev_softirq;
    unsigned long long non_idle = user + nice + system + irq + softirq;

    unsigned long long prev_total = prev_idle_sum + prev_non_idle;
    unsigned long long total = idle_sum + non_idle;

    unsigned long long total_diff = total - prev_total;
    unsigned long long idle_diff = idle_sum - prev_idle_sum;

    g_prev_user = user;
    g_prev_nice = nice;
    g_prev_system = system;
    g_prev_idle = idle;
    g_prev_iowait = iowait;
    g_prev_irq = irq;
    g_prev_softirq = softirq;

    if (total_diff == 0) {
        return 0.0;
    }

    double cpu = (double)(total_diff - idle_diff) / (double)total_diff * 100.0;
    if (cpu < 0.0) cpu = 0.0;
    if (cpu > 100.0) cpu = 100.0;

    return cpu;
}

JNIEXPORT jdoubleArray JNICALL
Java_com_piuu_launcher_repository_LibC_nativeGetMemInfo(JNIEnv* env, jobject thiz) {
    struct sysinfo info;
    jdoubleArray resultArray = (*env)->NewDoubleArray(env, 3);
    if (!resultArray) return NULL;

    jdouble result[3] = {0.0, 0.0, 0.0};

    if (sysinfo(&info) == 0) {
        double unit = (double)info.mem_unit;
        double totalMb = ((double)info.totalram * unit) / (1024.0 * 1024.0);
        double freeMb = (((double)info.freeram + (double)info.bufferram) * unit) / (1024.0 * 1024.0);
        double usedMb = (totalMb - freeMb > 0.0) ? (totalMb - freeMb) : 0.0;

        result[0] = totalMb / 1024.0; // Total GB
        result[1] = usedMb / 1024.0;  // Used GB
        result[2] = freeMb / 1024.0;  // Free GB
    }

    (*env)->SetDoubleArrayRegion(env, resultArray, 0, 3, result);
    return resultArray;
}

JNIEXPORT jint JNICALL
Java_com_piuu_launcher_repository_LibC_nativeGetThreadCount(JNIEnv* env, jobject thiz) {
    int threadCount = 0;
    DIR* dir = opendir("/proc/self/task");
    if (dir) {
        struct dirent* entry;
        while ((entry = readdir(dir)) != NULL) {
            if (entry->d_name[0] != '.') {
                threadCount++;
            }
        }
        closedir(dir);
    }
    return threadCount > 0 ? threadCount : 1;
}

JNIEXPORT jint JNICALL
Java_com_piuu_launcher_repository_LibC_nativeKillProcess(JNIEnv* env, jobject thiz, jstring jpackageName, jint pid) {
    if (pid > 0) {
        LOGI("nativeKillProcess sending SIGTERM (15) to PID %d", pid);
        kill(pid, SIGTERM);
        return 1;
    }
    return 0;
}
