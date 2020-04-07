#include <jni.h>

JNIEXPORT JNICALL jobject Java_fat_jca_dll_TestWrapper_echo(JNIEnv *env, jobject thisObj, jobject str) {
    return str;
}