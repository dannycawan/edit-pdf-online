/**
 * Purpose: Utility functions for file operations with SAF
 * Caller: EditorViewModel, HomeViewModel, export flows
 * Dependencies: Android ContentResolver, DocumentsContract
 * Main Functions: getFileName, getFileSize, createOutputFile
 * Side Effects: File I/O operations
 */
package com.editpdf.online.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    private val PDF_HEADER = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D)

    /**
     * Gets the display name of a file from its URI.
     */
    fun getFileName(context: Context, uri: Uri): String {
        var name = "document"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = cursor.getString(nameIndex) ?: name
                    }
                }
            }
        } catch (_: Exception) {}
        return name
    }

    /**
     * Gets the file size in bytes from its URI.
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        size = cursor.getLong(sizeIndex)
                    }
                }
            }
        } catch (_: Exception) {}
        return size
    }

    /**
     * Generates a default output filename based on the original filename.
     */
    fun generateOutputFileName(originalName: String): String {
        val baseName = originalName.removeSuffix(".pdf").removeSuffix(".PDF")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${baseName}_edited_$timestamp.pdf"
    }

    /**
     * Creates a temporary output file for PDF export.
     */
    fun createTempOutputFile(context: Context, fileName: String): File {
        val outputDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        return File(outputDir, fileName)
    }

    /**
     * Formats file size for display.
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    /**
     * Checks if a file is likely a PDF.
     *
     * SAF providers do not always expose streams in a way that makes a single read reliable,
     * so we accept either a PDF header or trusted picker metadata and let PdfRenderer do
     * the final validation when opening the document.
     */
    fun isPdfFile(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        val hasPdfMetadata = mimeType.equals("application/pdf", ignoreCase = true) ||
            getFileName(context, uri).endsWith(".pdf", ignoreCase = true)

        return hasPdfHeader(context, uri) || hasPdfMetadata
    }

    private fun hasPdfHeader(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(PDF_HEADER.size)
                var offset = 0
                while (offset < header.size) {
                    val bytesRead = input.read(header, offset, header.size - offset)
                    if (bytesRead == -1) break
                    offset += bytesRead
                }
                offset == PDF_HEADER.size && header.contentEquals(PDF_HEADER)
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the file size exceeds the maximum limit.
     */
    fun isFileTooLarge(context: Context, uri: Uri, maxSizeMb: Int = 50): Boolean {
        val size = getFileSize(context, uri)
        return size > maxSizeMb * 1024L * 1024L
    }
}
