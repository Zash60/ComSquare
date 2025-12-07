#pragma once

#include "Renderer/IRenderer.hpp"
#include <SDL.h>
#include <vector>
#include <android/log.h>

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
            window = SDL_CreateWindow("ComSquare", 0, 0, 0, 0, SDL_WINDOW_FULLSCREEN_DESKTOP | SDL_WINDOW_OPENGL);
            renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC);
            // Textura para o buffer do SNES (Streaming para atualização constante)
            texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_RGBA8888, SDL_TEXTUREACCESS_STREAMING, BUFFER_WIDTH, BUFFER_HEIGHT);
            
            // Limpa a tela
            SDL_SetRenderDrawColor(renderer, 0, 0, 0, 255);
            SDL_RenderClear(renderer);
            SDL_RenderPresent(renderer);
        }

        void setWindowName(std::string &name) override {}

        void drawScreen() override {
            if (!renderer || !texture) return;

            // Atualiza a textura com os pixels do buffer
            SDL_UpdateTexture(texture, nullptr, pixelBuffer.data(), BUFFER_WIDTH * sizeof(uint32_t));
            
            // Limpa
            SDL_SetRenderDrawColor(renderer, 0, 0, 0, 255);
            SDL_RenderClear(renderer);
            
            // Desenha a textura esticada na tela (mantendo aspecto se possível, aqui estica tudo)
            SDL_RenderCopy(renderer, texture, nullptr, nullptr);
            
            // Mostra
            SDL_RenderPresent(renderer);
        }

        void putPixel(unsigned y, unsigned x, uint32_t rgba) override {
            if (x < BUFFER_WIDTH && y < BUFFER_HEIGHT) {
                // RGBA8888 no SDL vs uint32 do core. Pode precisar de ajuste de endianness (ABGR vs ARGB)
                // O Core geralmente manda 0xRRGGBBAA ou similar. 
                // Vamos assumir compatibilidade direta por enquanto.
                pixelBuffer[y * BUFFER_WIDTH + x] = rgba;
            }
        }

        void createWindow(SNES &snes, int maxFPS) override {
            // Stub
        }

        void playAudio(std::span<int16_t> samples) override {
            // Stub (SDL Audio pode ser adicionado aqui depois)
        }
    };
}
