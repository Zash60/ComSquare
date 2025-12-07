#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include <android/bitmap.h>
#include "SNES.hpp"
#include "AndroidRenderer.hpp"

std::unique_ptr<ComSquare::Renderer::AndroidRenderer> g_renderer;
std::unique_ptr<ComSquare::SNES> g_snes;

extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_initNative(JNIEnv* env, jobject) {
    g_renderer = std::make_unique<ComSquare::Renderer::AndroidRenderer>();
    g_snes = std::make_unique<ComSquare::SNES>(*g_renderer);
}

extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_loadRomNative(JNIEnv* env, jobject, jstring path) {
    const char *nativePath = env->GetStringUTFChars(path, 0);
    if (g_snes) {
        try {
            g_snes->loadRom(std::string(nativePath));
        } catch (...) {}
    }
    env->ReleaseStringUTFChars(path, nativePath);
}

extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_updateFrame(JNIEnv*, jobject) {
    if (g_snes) { try { g_snes->update(); } catch(...) {} }
}

extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_renderToBitmap(JNIEnv* env, jobject, jobject bitmap) {
    if (!g_renderer) return;
    AndroidBitmapInfo info;
    void* pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) return;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return;
    memcpy(pixels, g_renderer->getPixels(), info.width * info.height * 4);
    AndroidBitmap_unlockPixels(env, bitmap);
}
