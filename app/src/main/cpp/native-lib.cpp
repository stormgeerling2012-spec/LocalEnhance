#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <cstring>
#include <string>
#include <memory>
#include <mutex>
#include "stable-diffusion.h"

#define LOG_TAG "ZoomEnhance"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static sd_ctx_t* g_sd_ctx = nullptr;
static std::mutex g_sd_mutex;

static uint8_t* bitmap_to_rgb(JNIEnv* env, jobject bitmap, int* out_w, int* out_h) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_getInfo failed");
        return nullptr;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format must be RGBA_8888, got %d", info.format);
        return nullptr;
    }
    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("AndroidBitmap_lockPixels failed");
        return nullptr;
    }
    int width = static_cast<int>(info.width);
    int height = static_cast<int>(info.height);
    int stride = static_cast<int>(info.stride);
    uint8_t* rgb = new (std::nothrow) uint8_t[width * height * 3];
    if (!rgb) {
        LOGE("Failed to allocate RGB tensor");
        AndroidBitmap_unlockPixels(env, bitmap);
        return nullptr;
    }
    const uint8_t* src = static_cast<const uint8_t*>(pixels);
    for (int y = 0; y < height; ++y) {
        const uint8_t* src_row = src + y * stride;
        uint8_t* dst_row = rgb + y * width * 3;
        for (int x = 0; x < width; ++x) {
            dst_row[x * 3 + 0] = src_row[x * 4 + 0];
            dst_row[x * 3 + 1] = src_row[x * 4 + 1];
            dst_row[x * 3 + 2] = src_row[x * 4 + 2];
        }
    }
    AndroidBitmap_unlockPixels(env, bitmap);
    *out_w = width;
    *out_h = height;
    return rgb;
}

static jobject rgb_to_bitmap(JNIEnv* env, const uint8_t* rgb, int width, int height) {
    jclass bitmap_cls = env->FindClass("android/graphics/Bitmap");
    jmethodID create_bitmap = env->GetStaticMethodID(bitmap_cls, "createBitmap",
        "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jclass config_cls = env->FindClass("android/graphics/Bitmap$Config");
    jfieldID argb_field = env->GetStaticFieldID(config_cls, "ARGB_8888",
        "Landroid/graphics/Bitmap$Config;");
    jobject argb_config = env->GetStaticObjectField(config_cls, argb_field);
    jobject bitmap = env->CallStaticObjectMethod(bitmap_cls, create_bitmap,
        width, height, argb_config);
    if (!bitmap) {
        env->DeleteLocalRef(bitmap_cls);
        env->DeleteLocalRef(config_cls);
        env->DeleteLocalRef(argb_config);
        return nullptr;
    }
    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        env->DeleteLocalRef(bitmap);
        env->DeleteLocalRef(bitmap_cls);
        env->DeleteLocalRef(config_cls);
        env->DeleteLocalRef(argb_config);
        return nullptr;
    }
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, bitmap, &info);
    int dst_stride = static_cast<int>(info.stride);
    uint8_t* dst = static_cast<uint8_t*>(pixels);
    for (int y = 0; y < height; ++y) {
        uint8_t* dst_row = dst + y * dst_stride;
        const uint8_t* src_row = rgb + y * width * 3;
        for (int x = 0; x < width; ++x) {
            dst_row[x * 4 + 0] = src_row[x * 3 + 0];
            dst_row[x * 4 + 1] = src_row[x * 3 + 1];
            dst_row[x * 4 + 2] = src_row[x * 3 + 2];
            dst_row[x * 4 + 3] = 0xFF;
        }
    }
    AndroidBitmap_unlockPixels(env, bitmap);
    env->DeleteLocalRef(bitmap_cls);
    env->DeleteLocalRef(config_cls);
    env->DeleteLocalRef(argb_config);
    return bitmap;
}

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_example_zoomenhance_NativeLib_initModel(JNIEnv* env, jclass clazz,
    jstring j_model_path, jstring j_taesd_path) {
    std::lock_guard<std::mutex> lock(g_sd_mutex);
    if (g_sd_ctx) { LOGI("Context exists"); return JNI_TRUE; }
    const char* model_path = env->GetStringUTFChars(j_model_path, nullptr);
    const char* taesd_path = env->GetStringUTFChars(j_taesd_path, nullptr);
    LOGI("Loading SD-Turbo: %s", model_path);
    g_sd_ctx = new_sd_ctx(model_path, nullptr, taesd_path, nullptr, nullptr,
        nullptr, nullptr, false, true, false, 4, SD_TYPE_Q4_0,
        STD_DEFAULT_RNG, KARRAS, false, false, false);
    env->ReleaseStringUTFChars(j_model_path, model_path);
    env->ReleaseStringUTFChars(j_taesd_path, taesd_path);
    if (!g_sd_ctx) { LOGE("new_sd_ctx failed"); return JNI_FALSE; }
    LOGI("Model loaded");
    return JNI_TRUE;
}

JNIEXPORT jobject JNICALL
Java_com_example_zoomenhance_NativeLib_enhanceImage(JNIEnv* env, jclass clazz, jobject bitmap) {
    std::lock_guard<std::mutex> lock(g_sd_mutex);
    if (!g_sd_ctx) { LOGE("Model not initialized"); return nullptr; }
    int width = 0, height = 0;
    uint8_t* rgb_input = bitmap_to_rgb(env, bitmap, &width, &height);
    if (!rgb_input) return nullptr;
    sd_image_t input_image = { static_cast<uint32_t>(width), static_cast<uint32_t>(height), 3, rgb_input };
    const char* prompt = "photorealistic, hyperdetailed micro textures, sharp focus, 8k";
    const char* negative = "blurry, pixelated, artifacts, noise";
    LOGI("img2img: 4 steps, strength=0.25");
    sd_image_t* output = img2img(g_sd_ctx, input_image, prompt, negative,
        1.0f, 4, 0.25f, 42, EULER_A, false, 0.9f, false, nullptr, nullptr);
    delete[] rgb_input;
    if (!output) { LOGE("img2img failed"); return nullptr; }
    jobject result = rgb_to_bitmap(env, output->data, output->width, output->height);
    free_sd_image(*output);
    delete output;
    LOGI("Done");
    return result;
}

JNIEXPORT void JNICALL
Java_com_example_zoomenhance_NativeLib_releaseModel(JNIEnv* env, jclass clazz) {
    std::lock_guard<std::mutex> lock(g_sd_mutex);
    if (g_sd_ctx) { free_sd_ctx(g_sd_ctx); g_sd_ctx = nullptr; LOGI("Released"); }
}

}
