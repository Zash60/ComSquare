package com.comsquare.emulator

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var tvStatus: TextView
    private lateinit var btnLoadRom: Button
    
    private var emulatorJob: Job? = null
    private val renderBitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
    
    @Volatile private var isRunning = false
    @Volatile private var isSurfaceValid = false
    
    private val renderDst = Rect()
    private val renderSrc = Rect(0, 0, 256, 224)

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { loadGameFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.emulatorSurface)
        tvStatus = findViewById(R.id.tvStatus)
        btnLoadRom = findViewById(R.id.btnLoadRom)

        // Carrega biblioteca C++
        initNative()

        // Configura transparência e formato para evitar bugs visuais
        surfaceView.setZOrderOnTop(false) 
        surfaceView.holder.setFormat(PixelFormat.RGBA_8888)

        btnLoadRom.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                isSurfaceValid = true
                if (isRunning) startEmulatorLoop(holder)
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                renderDst.set(0, 0, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isSurfaceValid = false
                // Não cancelamos o job aqui para não perder o estado do emulador, apenas paramos o desenho
            }
        })
    }

    private fun loadGameFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "game.sfc")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            loadRomNative(tempFile.absolutePath)
            
            // UI Update
            tvStatus.visibility = View.GONE
            btnLoadRom.visibility = View.GONE
            
            isRunning = true
            if (isSurfaceValid) startEmulatorLoop(surfaceView.holder)
            
            Toast.makeText(this, "Game Loaded!", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            tvStatus.text = "Error: ${e.message}"
            tvStatus.setTextColor(Color.RED)
        }
    }

    private fun startEmulatorLoop(holder: SurfaceHolder) {
        emulatorJob?.cancel()
        
        emulatorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isRunning) {
                val startTime = System.currentTimeMillis()
                
                // 1. C++ Core Update
                updateFrame()
                
                // 2. Render Check
                if (isSurfaceValid && holder.surface.isValid) {
                    try {
                        // Lock Canvas (Kotlin warning fixed: direct assignment)
                        val canvas = holder.lockCanvas()
                        if (canvas != null) {
                            canvas.drawColor(Color.BLACK)
                            canvas.drawBitmap(renderBitmap, renderSrc, renderDst, null)
                            holder.unlockCanvasAndPost(canvas)
                        }
                    } catch (e: Exception) {
                        // Ignora erros de surface perdida durante rotação
                    }
                }

                // 3. FPS Limiter (~60 FPS)
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
