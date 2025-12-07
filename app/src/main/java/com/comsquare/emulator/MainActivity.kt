package com.comsquare.emulator

import org.libsdl.app.SDLActivity
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.view.Gravity
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.graphics.Color

class MainActivity : SDLActivity() {

    // Nome da biblioteca definida no CMake
    override fun getLibraries(): Array<String> {
        return arrayOf("comsquare")
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { loadGameFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // SDLActivity cria seu próprio layout. Vamos adicionar um botão por cima.
        val loadButton = Button(this)
        loadButton.text = "LOAD ROM"
        loadButton.setBackgroundColor(Color.WHITE)
        loadButton.setTextColor(Color.BLACK)
        loadButton.setPadding(50, 20, 50, 20)
        
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.topMargin = 100
        
        // O layout principal do SDL é um FrameLayout (mLayout)
        addContentView(loadButton, params)

        loadButton.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }
        
        // Salva referência para esconder depois
        loadButton.tag = "btnLoad"
    }

    private fun loadGameFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "game.sfc")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            // Passa o caminho para o C++
            loadRomNative(tempFile.absolutePath)
            
            // Esconde o botão (encontra pela tag ou guarda referência)
            val btn = window.decorView.findViewWithTag<Button>("btnLoad")
            btn?.visibility = View.GONE
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Função JNI implementada no native-lib.cpp
    external fun loadRomNative(path: String)
}
