#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include <SDL.h>
#include <SDL_main.h>
#include <SDL_system.h> // Necessário para interagir com Android

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
bool g_requestFilePicker = false;

// Função chamada pelo Java quando o arquivo é selecionado
extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_onRomSelectedNative(JNIEnv* env, jobject, jstring path) {
    const char *nativePath = env->GetStringUTFChars(path, 0);
    g_romPath = std::string(nativePath);
    LOGI("ROM Path recebido no C++: %s", nativePath);
    
    // Tenta carregar imediatamente
    if (g_snes) {
        try {
            g_snes->loadRom(g_romPath);
            g_romLoaded = true;
            LOGI("ROM carregada com sucesso!");
        } catch (const std::exception& e) {
            LOGE("Erro ao carregar ROM: %s", e.what());
        }
    }
    env->ReleaseStringUTFChars(path, nativePath);
}

// Helper para chamar o método Java 'openFileBrowser'
void callJavaOpenFilePicker() {
    JNIEnv* env = (JNIEnv*)SDL_AndroidGetJNIEnv();
    jobject activity = (jobject)SDL_AndroidGetActivity();
    jclass clazz = env->GetObjectClass(activity);
    jmethodID methodID = env->GetMethodID(clazz, "openFileBrowser", "()V");
    
    if (methodID) {
        env->CallVoidMethod(activity, methodID);
    } else {
        LOGE("Não foi possível encontrar o método openFileBrowser no Java");
    }
    
    env->DeleteLocalRef(activity);
    env->DeleteLocalRef(clazz);
}

int main(int argc, char* argv[]) {
    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO) < 0) {
        LOGE("SDL_Init falhou: %s", SDL_GetError());
        return 1;
    }

    SDL_Window* window = SDL_CreateWindow("ComSquare", 0, 0, 0, 0, SDL_WINDOW_FULLSCREEN_DESKTOP | SDL_WINDOW_OPENGL);
    SDL_Renderer* renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED);
    
    // Textura para o emulador
    SDL_Texture* texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ABGR8888, SDL_TEXTUREACCESS_STREAMING, 1024, 1024);

    // Inicializa Core
    g_renderer = std::make_unique<ComSquare::Renderer::AndroidRenderer>();
    // Não usamos putPixel no update loop do SDL direto, 
    // mas precisamos que o SNES tenha uma referencia válida se ele chamar algo internamente.
    
    g_snes = std::make_unique<ComSquare::SNES>(*g_renderer);

    bool running = true;
    SDL_Event event;

    while (running) {
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_QUIT || event.type == SDL_APP_TERMINATING) {
                running = false;
            }
            
            // Se tocar na tela e não tiver ROM, abre o seletor
            if (event.type == SDL_FINGERDOWN || event.type == SDL_MOUSEBUTTONDOWN) {
                if (!g_romLoaded) {
                    LOGI("Toque detectado. Abrindo seletor de arquivos...");
                    callJavaOpenFilePicker();
                }
            }
        }

        if (g_romLoaded) {
            // --- MODO EMULAÇÃO ---
            try {
                g_snes->update();
                
                // Renderiza o frame do emulador
                // Copia do buffer interno do AndroidRenderer para a textura SDL
                SDL_UpdateTexture(texture, nullptr, g_renderer->getPixels(), 1024 * 4);
                SDL_RenderClear(renderer);
                SDL_RenderCopy(renderer, texture, nullptr, nullptr);
                SDL_RenderPresent(renderer);
                
            } catch (...) {}
        } else {
            // --- MODO ESPERA (Tela Azul) ---
            // Azul = Aguardando Toque para carregar ROM
            SDL_SetRenderDrawColor(renderer, 0, 0, 200, 255); 
            SDL_RenderClear(renderer);
            SDL_RenderPresent(renderer);
        }
        
        SDL_Delay(16); // ~60 FPS cap
    }

    SDL_Quit();
    return 0;
}
