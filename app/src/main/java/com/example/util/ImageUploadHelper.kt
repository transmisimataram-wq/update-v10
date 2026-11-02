package com.example.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageUploadHelper {

    /**
     * Copies the content URI image data to app internal storage under custom_icons/
     * Returns the absolute path of the saved file or null on failure.
     */
    fun saveImageToInternalStorage(context: Context, uri: Uri, prefix: String = "icon"): String? {
        return try {
            val dir = File(context.filesDir, "custom_icons")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
