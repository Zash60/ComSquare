package com.comsquare.emulator
import org.libsdl.app.SDLActivity
class MainActivity : SDLActivity() {
    override fun getLibraries(): Array<String> {
        return arrayOf("comsquare")
    }
}
