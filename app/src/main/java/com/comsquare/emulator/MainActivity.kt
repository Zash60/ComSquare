package com.comsquare.emulator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private var emulatorJob: Job? = null
    private val renderBitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
    private var isRunning = false
    private val renderSrc = Rect(0, 0, 256, 224)
    private val renderDst = Rect()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        initNative()

        // Carrega ROM de exemplo (placeholder)
        // No mundo real, use um FilePicker
        val romPath = File(cacheDir, "game.smc").absolutePath 
        // loadRomNative(romPath) // Descomente quando tiver uma ROM real

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                isRunning = true
                startEmulatorLoop(holder)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                renderDst.set(0, 0, width, height)
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isRunning = false
                emulatorJob?.cancel()
            }
        })
    }

    private fun startEmulatorLoop(holder: SurfaceHolder) {
        emulatorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isRunning) {
                val startTime = System.currentTimeMillis()
                
                // 1. Atualiza Core C++
                updateFrame()
                
                // 2. Copia pixels para Bitmap
                renderToBitmap(renderBitmap)
                
                // 3. Desenha na tela
                val canvas: Canvas? = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(android.graphics.Color.BLACK)
                    canvas.drawBitmap(renderBitmap, renderSrc, renderDst, null)
                    holder.unlockCanvasAndPost(canvas)
                }

                val diff = System.currentTimeMillis() - startTime
                if (diff < 16) delay(16 - diff)
            }
        }
    }

    external fun initNative()
    external fun loadRomNative(path: String)
    external fun updateFrame()
    external fun renderToBitmap(bitmap: Bitmap)

    companion object {
        init { System.loadLibrary("comsquare") }
    }
}
