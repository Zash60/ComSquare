package com.comsquare.emulator

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
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
    // Buffer de renderização (1024x1024 para segurança, o SNES usa 256x224 ou 512x448)
    private val renderBitmap = Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
    private var isRunning = false
    
    // Onde desenhar na tela do celular
    private val renderDst = Rect()
    // Área útil do SNES dentro do buffer 1024x1024
    private val renderSrc = Rect(0, 0, 256, 224)

    // Seletor de Arquivos
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { loadGameFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.emulatorSurface)
        tvStatus = findViewById(R.id.tvStatus)
        btnLoadRom = findViewById(R.id.btnLoadRom)

        // Inicializa o C++
        initNative()

        btnLoadRom.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*")) // Aceita qualquer arquivo por enquanto
        }

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                // Loop inicia, mas só processa se tiver ROM carregada
                startEmulatorLoop(holder)
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // Ajusta para manter proporção ou preencher (aqui preenche tudo)
                renderDst.set(0, 0, width, height)
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isRunning = false
                emulatorJob?.cancel()
            }
        })
    }

    private fun loadGameFromUri(uri: Uri) {
        // O C++ precisa de um caminho de arquivo real (File Path), não um Content URI.
        // Copiamos o arquivo para o cache do app.
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "game.sfc")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            // Manda o C++ carregar o arquivo do cache
            loadRomNative(tempFile.absolutePath)
            
            tvStatus.visibility = View.GONE
            btnLoadRom.visibility = View.GONE // Esconde o botão ao jogar
            isRunning = true
            
            Toast.makeText(this, "ROM Loaded!", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            tvStatus.text = "Error: ${e.message}"
            e.printStackTrace()
        }
    }

    private fun startEmulatorLoop(holder: SurfaceHolder) {
        // Cancela job anterior se existir
        emulatorJob?.cancel()
        isRunning = true // O loop roda, mas o updateFrame só faz algo se a ROM estiver carregada no C++

        emulatorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isRunning) {
                val startTime = System.currentTimeMillis()
                
                // 1. Atualiza Core C++ (Se não tiver ROM, o C++ deve tratar isso ou não fazer nada)
                updateFrame()
                
                // 2. Copia pixels para Bitmap
                renderToBitmap(renderBitmap)
                
                // 3. Desenha na tela
                val canvas: Canvas? = holder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(android.graphics.Color.BLACK)
                    // Desenha apenas se tivermos algo válido, ou desenha preto
                    canvas.drawBitmap(renderBitmap, renderSrc, renderDst, null)
                    holder.unlockCanvasAndPost(canvas)
                }

                // Limitador de FPS (~60 FPS)
                val diff = System.currentTimeMillis() - startTime
                if (diff < 16) delay(16 - diff)
            }
        }
    }

    // Métodos nativos (JNI)
    external fun initNative()
    external fun loadRomNative(path: String)
    external fun updateFrame()
    external fun renderToBitmap(bitmap: Bitmap)
    // external fun setButtonState(buttonId: Int, pressed: Boolean) // Para o futuro

    companion object {
        init { System.loadLibrary("comsquare") }
    }
}
