#pragma once
#include "Renderer/IRenderer.hpp"
#include <vector>
#include <android/log.h>

namespace ComSquare::Renderer {
    class AndroidRenderer : public IRenderer {
    public:
        std::vector<uint32_t> pixelBuffer;
        AndroidRenderer() { pixelBuffer.resize(1024 * 1024); }
        
        void setWindowName(std::string &name) override {}
        void drawScreen() override {} // O loop Android controla isso
        
        void putPixel(unsigned y, unsigned x, uint32_t rgba) override {
            if (x < 1024 && y < 1024) {
                // Converte RGBA para ARGB se necessário, mas SFML e Android geralmente se entendem
                pixelBuffer[y * 1024 + x] = rgba;
            }
        }
        
        void createWindow(SNES &snes, int maxFPS) override {}
        void playAudio(std::span<int16_t> samples) override {}
        
        const uint32_t* getPixels() const { return pixelBuffer.data(); }
    };
}
