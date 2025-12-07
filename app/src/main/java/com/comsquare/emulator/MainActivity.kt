package com.comsquare.emulator

import org.libsdl.app.SDLActivity
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.view.Gravity
import android.view.View
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.graphics.Color
import android.content.Intent
import android.app.Activity

class MainActivity : SDLActivity() {

    // Código de requisição para identificar o retorno do seletor de arquivos
    private val PICK_ROM_CODE = 123

    override fun getLibraries(): Array<String> {
        return arrayOf("comsquare")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Botão Flutuante (Overlay)
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
        params.topMargin = 150 // Margem para não ficar em cima da status bar
        
        addContentView(loadButton, params)

        loadButton.setOnClickListener {
            // Usa a API clássica de Intent, compatível com SDLActivity
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Pode filtrar por arquivos específicos se quiser
            }
            startActivityForResult(intent, PICK_ROM_CODE)
        }
        
        loadButton.tag = "btnLoad"
    }

    // Captura o resultado da seleção do arquivo
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == PICK_ROM_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                loadGameFromUri(uri)
            }
        }
    }

    private fun loadGameFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "game.sfc")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            // Passa o caminho absoluto para o C++
            loadRomNative(tempFile.absolutePath)
            
            // Esconde o botão após carregar
            val btn = window.decorView.findViewWithTag<Button>("btnLoad")
            btn?.visibility = View.GONE
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    external fun loadRomNative(path: String)
}
