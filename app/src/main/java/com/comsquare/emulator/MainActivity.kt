package com.comsquare.emulator

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Surface
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
    @Volatile private var isRunning = false
    
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
                // Passa a superfície nativa para o C++
                setSurface(holder.surface)
                if (isRunning) startEmulatorLoop()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                // C++ gerencia a escala automaticamente via ANativeWindow_setBuffersGeometry
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Avisa o C++ que a superfície morreu
                setSurface(null)
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
            
            // Inicia o loop se a superfície já estiver pronta (ou espera o callback)
            if (surfaceView.holder.surface.isValid) {
                setSurface(surfaceView.holder.surface)
                startEmulatorLoop()
            }
            
        } catch (e: Exception) {
            tvStatus.text = "Error: ${e.message}"
        }
    }

    private fun startEmulatorLoop() {
        emulatorJob?.cancel()
        emulatorJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && isRunning) {
                val start = System.currentTimeMillis()
                
                // Toda a mágica acontece no C++ agora (Update + Render)
                runFrame()
                
                val diff = System.currentTimeMillis() - start
                if (diff < 16) delay(16 - diff)
            }
        }
    }

    external fun initNative()
    external fun loadRomNative(path: String)
    external fun setSurface(surface: Surface?) // Novo método: Passa o Surface direto
    external fun runFrame() // Antigo updateFrame, agora faz tudo

    companion object {
        init { System.loadLibrary("comsquare") }
    }
}
