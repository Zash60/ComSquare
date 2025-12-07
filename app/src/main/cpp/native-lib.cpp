#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include <SDL.h>
#include <SDL_main.h>
#include <SDL_system.h>

#include "SNES.hpp"
#include "AndroidRenderer.hpp"

#define LOG_TAG "ComSquare_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Globais
std::unique_ptr<ComSquare::Renderer::AndroidRenderer> g_renderer;
std::unique_ptr<ComSquare::SNES> g_snes;
std::string g_romPath = "";
bool g_romLoaded = false;

// JNI: Recebe caminho da ROM
extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_onRomSelectedNative(JNIEnv* env, jobject, jstring path) {
    const char *nativePath = env->GetStringUTFChars(path, 0);
    g_romPath = std::string(nativePath);
    LOGI("ROM Path recebido: %s", nativePath);
    
    if (g_snes) {
        try {
            // Reinicia o emulador ao carregar nova ROM para limpar estado
            // g_snes->reset(); // Se existir método reset
            g_snes->loadRom(g_romPath);
            g_romLoaded = true;
            LOGI("ROM carregada com sucesso!");
        } catch (const std::exception& e) {
            LOGE("Erro ao carregar ROM: %s", e.what());
        }
    }
    env->ReleaseStringUTFChars(path, nativePath);
}

void callJavaOpenFilePicker() {
    JNIEnv* env = (JNIEnv*)SDL_AndroidGetJNIEnv();
    jobject activity = (jobject)SDL_AndroidGetActivity();
    jclass clazz = env->GetObjectClass(activity);
    jmethodID methodID = env->GetMethodID(clazz, "openFileBrowser", "()V");
    if (methodID) env->CallVoidMethod(activity, methodID);
    env->DeleteLocalRef(activity);
    env->DeleteLocalRef(clazz);
}

int main(int argc, char* argv[]) {
    LOGI("Inicializando SDL...");

    // 1. Configura atributos GL ANTES de criar a janela para evitar erro libEGL
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_ES);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 2);
    SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 0);
    SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);
    SDL_GL_SetAttribute(SDL_GL_DEPTH_SIZE, 24);

    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO) < 0) {
        LOGE("SDL_Init falhou: %s", SDL_GetError());
        return 1;
    }

    // 2. Cria janela em tela cheia e redimensionável (importante para Android)
    SDL_Window* window = SDL_CreateWindow("ComSquare", 
        0, 0, 
        0, 0, // Ignorado no Android fullscreen
        SDL_WINDOW_FULLSCREEN | SDL_WINDOW_RESIZABLE | SDL_WINDOW_OPENGL | SDL_WINDOW_ALLOW_HIGHDPI);

    if (!window) {
        LOGE("Erro ao criar janela: %s", SDL_GetError());
        return 1;
    }

    // 3. Cria renderer. O VSync ajuda a sincronizar com a tela do celular (60Hz)
    SDL_Renderer* renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);
    
    // 4. Mágica de Escala: Dizemos ao SDL que nossa "resolução lógica" é 256x224 (SNES).
    // O SDL vai esticar isso automaticamente para encher a tela do celular, mantendo aspecto se quisermos.
    // Como estamos usando uma textura de 1024x1024 (buffer interno), vamos configurar para isso por enquanto.
    SDL_RenderSetLogicalSize(renderer, 1024, 1024);

    // Cria textura de streaming
    SDL_Texture* texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ABGR8888, SDL_TEXTUREACCESS_STREAMING, 1024, 1024);

    // Inicializa Core
    g_renderer = std::make_unique<ComSquare::Renderer::AndroidRenderer>();
    // Passa referências (opcional, dependendo de como AndroidRenderer foi implementado)
    g_renderer->window = window;
    g_renderer->renderer = renderer;
    g_renderer->texture = texture;

    g_snes = std::make_unique<ComSquare::SNES>(*g_renderer);

    bool running = true;
    SDL_Event event;
    int frameCount = 0;

    while (running) {
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_QUIT || event.type == SDL_APP_TERMINATING) {
                running = false;
            }
            if (event.type == SDL_RENDER_DEVICE_RESET) {
                LOGI("Dispositivo gráfico resetado, recriando textura...");
                // Tratar perda de contexto se necessário
            }
            
            // Toque na tela para abrir ROM
            if (event.type == SDL_FINGERDOWN && !g_romLoaded) {
                callJavaOpenFilePicker();
            }
        }

        // Limpa a tela
        SDL_RenderClear(renderer);

        if (g_romLoaded) {
            try {
                g_snes->update();
                
                // Atualiza textura
                void* pixels;
                int pitch;
                if (SDL_LockTexture(texture, nullptr, &pixels, &pitch) == 0) {
                    // Copia do buffer do emulador para a textura
                    // g_renderer->getPixels() retorna const void*
                    memcpy(pixels, g_renderer->getPixels(), 1024 * 1024 * 4);
                    SDL_UnlockTexture(texture);
                }
                
                // Desenha a textura esticada na tela
                SDL_RenderCopy(renderer, texture, nullptr, nullptr);
                
            } catch (...) {}
        } else {
            // Tela de Espera (Azul)
            SDL_SetRenderDrawColor(renderer, 0, 0, 200, 255);
            SDL_RenderFillRect(renderer, nullptr);
        }

        // Mostra na tela
        SDL_RenderPresent(renderer);
        
        // Pequeno delay para não fritar a CPU se o VSync falhar
        // Se VSync estiver ativo no CreateRenderer, isso é redundante mas seguro
        // SDL_Delay(0); 
    }

    SDL_Quit();
    return 0;
}
