package com.comsquare.emulator

import android.app.Activity
import android.content.Intent
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
    
    // Controle de estado volátil para threads
    @Volatile
    private var isRunning = false
    @Volatile
    private var isSurfaceReady = false
    
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

        initNative()

        btnLoadRom.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // Define o formato de pixel para evitar avisos do HWUI
                holder.setFormat(PixelFormat.RGBA_8888)
                isSurfaceReady = true
                if (isRunning) {
                    startEmulatorLoop(holder)
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                renderDst.set(0, 0, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isSurfaceReady = false
                // Não cancelamos o job aqui para não matar o estado do emulador ao rotacionar,
                // apenas paramos de desenhar. O loop checa isSurfaceReady.
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
            
            tvStatus.visibility = View.GONE
            btnLoadRom.visibility = View.GONE
            
            isRunning = true
            // Se a superfície já existe, inicia. Se não, o callback surfaceCreated iniciará.
            if (isSurfaceReady) {
                startEmulatorLoop(surfaceView.holder)
            }
            
            Toast.makeText(this, "ROM Loaded!", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            tvStatus.text = "Error: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun startEmulatorLoop(holder: SurfaceHolder) {
        emulatorJob?.cancel()
        
        emulatorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isRunning) {
                val startTime = System.currentTimeMillis()
                
                // 1. Atualiza Core C++
                updateFrame()
                
                // 2. Copia pixels para Bitmap
                renderToBitmap(renderBitmap)
                
                // 3. Desenha na tela (BLINDADO)
                if (isSurfaceReady) {
                    var canvas: Canvas? = null
                    try {
                        canvas = holder.lockCanvas()
                        if (canvas != null) {
                            // Limpa o fundo (ajuda com artefatos de rotação)
                            canvas.drawColor(Color.BLACK)
                            // Desenha o jogo escalado
                            canvas.drawBitmap(renderBitmap, renderSrc, renderDst, null)
                            holder.unlockCanvasAndPost(canvas)
                        }
                    } catch (e: Exception) {
                        // Loga o erro, mas não crashta o app. 
                        // É comum Surface falhar durante rotação/minimizar.
                        e.printStackTrace()
                    }
                }

                // Limitador de FPS (~60 FPS)
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
