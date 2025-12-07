#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include "SNES.hpp"
#include "AndroidRenderer.hpp"

// Globais
std::unique_ptr<ComSquare::Renderer::AndroidRenderer> g_renderer;
std::unique_ptr<ComSquare::SNES> g_snes;
ANativeWindow* g_window = nullptr;

// Resolução nativa do SNES (geralmente 256x224, mas o buffer interno é 1024)
// Vamos configurar o buffer da janela para 256x224 para o Android fazer o upscale via Hardware
const int SNES_WIDTH = 256;
const int SNES_HEIGHT = 224;

extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_initNative(JNIEnv* env, jobject) {
    g_renderer = std::make_unique<ComSquare::Renderer::AndroidRenderer>();
    g_snes = std::make_unique<ComSquare::SNES>(*g_renderer);
    __android_log_print(ANDROID_LOG_INFO, "ComSquare", "Core Initialized");
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
Java_com_comsquare_emulator_MainActivity_setSurface(JNIEnv* env, jobject, jobject surface) {
    // Se já existe uma janela, libera
    if (g_window) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }

    // Se uma nova superfície foi passada, configura
    if (surface) {
        g_window = ANativeWindow_fromSurface(env, surface);
        // Define o tamanho do buffer interno. O Android estica isso para o tamanho da tela.
        // Isso é muito mais rápido do que fazer scaling via software.
        ANativeWindow_setBuffersGeometry(g_window, 1024, 1024, WINDOW_FORMAT_RGBA_8888);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_runFrame(JNIEnv* env, jobject) {
    if (!g_snes || !g_renderer) return;

    // 1. Atualiza Lógica do Emulador
    try { 
        g_snes->update(); 
    } catch(...) { return; }

    // 2. Renderização Direta (Sem JNI Bitmap Copy)
    if (g_window) {
        ANativeWindow_Buffer buffer;
        if (ANativeWindow_lock(g_window, &buffer, nullptr) == 0) {
            
            const uint32_t* src = g_renderer->getPixels();
            uint32_t* dst = (uint32_t*)buffer.bits;
            
            // Copia linha a linha considerando o stride (largura real da memória)
            // O PPU original desenha em um buffer de 1024x1024
            // Como definimos a geometria da janela para 1024x1024, podemos copiar direto.
            
            // Otimização: Copia apenas a área relevante se possível, 
            // mas aqui vamos copiar o buffer todo para garantir que funcione primeiro.
            // src size = 1024 * 1024
            
            // Cuidado: buffer.stride pode ser maior que width
            int lineSize = 1024 * 4; // 1024 pixels * 4 bytes
            for (int i = 0; i < 1024; ++i) {
                memcpy(dst + (i * buffer.stride), src + (i * 1024), lineSize);
            }

            ANativeWindow_unlockAndPost(g_window);
        }
    }
}
