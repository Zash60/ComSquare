#pragma once

#include "Renderer/IRenderer.hpp"
#include <SDL.h>
#include <vector>
#include <android/log.h>
#include <span>

namespace ComSquare::Renderer {

    class AndroidRenderer : public IRenderer {
    public:
        SDL_Window* window = nullptr;
        SDL_Renderer* renderer = nullptr;
        SDL_Texture* texture = nullptr;
        std::vector<uint32_t> pixelBuffer;

        // Buffer interno do SNES (geralmente 1024x1024 no core atual)
        const int BUFFER_WIDTH = 1024;
        const int BUFFER_HEIGHT = 1024;

        AndroidRenderer() {
            pixelBuffer.resize(BUFFER_WIDTH * BUFFER_HEIGHT, 0xFF000000);
        }

        void initSDL() {
            // SDL já foi iniciado no main, aqui pegamos a janela se necessário ou criamos recursos
            // No Android, a janela é criada automaticamente pelo SDL_Init
            // Criamos apenas o renderer e a textura aqui
            
            // Usa a janela principal do SDL
            window = SDL_CreateWindow("ComSquare", 0, 0, 0, 0, SDL_WINDOW_FULLSCREEN_DESKTOP | SDL_WINDOW_OPENGL);
            renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);
            
            // Textura para o buffer do SNES (Streaming para atualização constante)
            // ABGR8888 é geralmente o formato nativo mais rápido em GPUs Android (GLES)
            texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_ABGR8888, SDL_TEXTUREACCESS_STREAMING, BUFFER_WIDTH, BUFFER_HEIGHT);
            
            // Limpa a tela
            SDL_SetRenderDrawColor(renderer, 0, 0, 0, 255);
            SDL_RenderClear(renderer);
            SDL_RenderPresent(renderer);
        }

        void setWindowName(std::string &name) override {}

        void drawScreen() override {
            // O desenho real é feito no loop principal do SDL (native-lib.cpp)
            // Este método é chamado pelo SNES::update().
            // Não fazemos o SDL_RenderPresent aqui para não bloquear a thread de emulação.
        }

        void putPixel(unsigned y, unsigned x, uint32_t rgba) override {
            if (x < BUFFER_WIDTH && y < BUFFER_HEIGHT) {
                // O SNES envia pixels no formato 0xAABBGGRR (little endian uint32) ou similar.
                // Precisamos garantir que bata com a textura SDL (ABGR8888).
                // Se as cores ficarem invertidas (Vermelho <-> Azul), trocamos aqui ou na criação da textura.
                pixelBuffer[y * BUFFER_WIDTH + x] = rgba;
            }
        }

        void createWindow(SNES &snes, int maxFPS) override {
            // Stub
        }

        void playAudio(std::span<int16_t> samples) override {
            // Stub (SDL Audio pode ser adicionado aqui depois)
        }
        
        // Helper para o loop principal acessar os dados brutos
        const void* getPixels() const {
            return pixelBuffer.data();
        }
    };
}
