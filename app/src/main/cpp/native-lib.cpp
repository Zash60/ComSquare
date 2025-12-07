#include <jni.h>
#include <string>
#include <memory>
#include <android/log.h>
#include <SDL.h>
#include <SDL_main.h>

#include "SNES.hpp"
#include "AndroidRenderer.hpp"

// Globais
std::unique_ptr<ComSquare::Renderer::AndroidRenderer> g_renderer;
std::unique_ptr<ComSquare::SNES> g_snes;
std::string g_romPath = "";
bool g_romLoaded = false;

// Função JNI para receber o caminho da ROM do Kotlin
extern "C" JNIEXPORT void JNICALL
Java_com_comsquare_emulator_MainActivity_loadRomNative(JNIEnv* env, jobject, jstring path) {
    const char *nativePath = env->GetStringUTFChars(path, 0);
    g_romPath = std::string(nativePath);
    __android_log_print(ANDROID_LOG_INFO, "ComSquare", "ROM Path received: %s", nativePath);
    env->ReleaseStringUTFChars(path, nativePath);
}

// Ponto de entrada SDL
int main(int argc, char* argv[]) {
    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_AUDIO) < 0) {
        __android_log_print(ANDROID_LOG_ERROR, "ComSquare", "SDL_Init failed: %s", SDL_GetError());
        return 1;
    }

    g_renderer = std::make_unique<ComSquare::Renderer::AndroidRenderer>();
    g_renderer->initSDL(); // Cria janela e renderer SDL
    
    g_snes = std::make_unique<ComSquare::SNES>(*g_renderer);

    bool running = true;
    SDL_Event event;

    while (running) {
        while (SDL_PollEvent(&event)) {
            if (event.type == SDL_QUIT) {
                running = false;
            }
            if (event.type == SDL_APP_TERMINATING) {
                running = false;
            }
        }

        // Lógica de Carga de ROM
        if (!g_romLoaded && !g_romPath.empty()) {
            try {
                g_snes->loadRom(g_romPath);
                g_romLoaded = true;
                __android_log_print(ANDROID_LOG_INFO, "ComSquare", "ROM Loaded Successfully in Core");
            } catch (const std::exception& e) {
                __android_log_print(ANDROID_LOG_ERROR, "ComSquare", "Load Error: %s", e.what());
                g_romPath = ""; // Tenta de novo se o usuário mandar outro
            }
        }

        if (g_romLoaded) {
            try {
                g_snes->update();
                // O update do SNES chama g_renderer->drawScreen() internamente
            } catch (...) {
                 // Evita crash
            }
        } else {
            // Se não tem ROM, desenha tela preta ou de espera
            SDL_SetRenderDrawColor(g_renderer->renderer, 20, 20, 20, 255);
            SDL_RenderClear(g_renderer->renderer);
            SDL_RenderPresent(g_renderer->renderer);
            SDL_Delay(100);
        }
    }

    SDL_Quit();
    return 0;
}
