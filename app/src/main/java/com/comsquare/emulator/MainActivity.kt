package com.comsquare.emulator

import org.libsdl.app.SDLActivity
import android.content.Intent
import android.app.Activity
import android.net.Uri
import android.os.Bundle
import java.io.File
import java.io.FileOutputStream
import android.util.Log

class MainActivity : SDLActivity() {

    private val PICK_ROM_CODE = 555

    override fun getLibraries(): Array<String> {
        return arrayOf("comsquare")
    }

    // Método chamado pelo C++ quando o usuário toca na tela
    fun openFileBrowser() {
        runOnUiThread {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*" // Aceita qualquer extensão
            }
            startActivityForResult(intent, PICK_ROM_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == PICK_ROM_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val path = copyRomToCache(uri)
                if (path != null) {
                    Log.i("ComSquare", "ROM copiada para: $path")
                    // Envia o caminho para o C++
                    onRomSelectedNative(path)
                }
            }
        }
    }

    private fun copyRomToCache(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(cacheDir, "game.sfc")
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Método nativo implementado no C++
    external fun onRomSelectedNative(path: String)
}
