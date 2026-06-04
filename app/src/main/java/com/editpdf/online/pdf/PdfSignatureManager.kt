/**
 * Purpose: Signature management - save/load signature bitmaps as transparent PNGs
 * Caller: SignatureViewModel, EditorViewModel
 * Dependencies: Android Bitmap, File I/O
 * Main Functions: saveSignature, loadSignature, deleteSignature
 * Side Effects: Creates/deletes PNG files in app internal storage
 */
package com.editpdf.online.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfSignatureManager(private val context: Context) {

    private val signatureDir: File
        get() = File(context.filesDir, "signatures").also { it.mkdirs() }

    /**
     * Saves a signature bitmap as a transparent PNG file.
     *
     * @param bitmap The signature bitmap to save
     * @param name Optional name for the signature file
     * @return The file path of the saved signature
     */
    suspend fun saveSignature(
        bitmap: Bitmap,
        name: String = "signature_${System.currentTimeMillis()}"
    ): String = withContext(Dispatchers.IO) {
        val file = File(signatureDir, "$name.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        file.absolutePath
    }

    /**
     * Loads a signature bitmap from a file path.
     */
    suspend fun loadSignature(path: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            BitmapFactory.decodeFile(path)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Gets all saved signature file paths.
     */
    suspend fun getSavedSignatures(): List<String> = withContext(Dispatchers.IO) {
        signatureDir.listFiles()
            ?.filter { it.extension == "png" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.absolutePath }
            ?: emptyList()
    }

    /**
     * Deletes a signature file.
     */
    suspend fun deleteSignature(path: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(path).delete()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Deletes all saved signatures.
     */
    suspend fun deleteAllSignatures() = withContext(Dispatchers.IO) {
        signatureDir.listFiles()?.forEach { it.delete() }
    }
}
