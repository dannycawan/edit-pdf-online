/**
 * Purpose: Manages Android PdfRenderer for rendering PDF pages to Bitmaps
 * Caller: EditorViewModel, EditorScreen
 * Dependencies: Android PdfRenderer, ParcelFileDescriptor
 * Main Functions: openPdf, renderPage, closePdf, getPageCount
 * Side Effects: Opens file descriptors, allocates Bitmaps
 */
package com.editpdf.online.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class PdfRendererManager(private val context: Context) {

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPage: PdfRenderer.Page? = null
    private val mutex = Mutex()

    /**
     * Total number of pages in the currently opened PDF.
     */
    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    /**
     * Opens a PDF file from the given URI.
     * Copies the file to a temporary location to avoid SAF file descriptor issues.
     *
     * @throws IOException if the file cannot be opened
     * @throws SecurityException if the PDF is password-protected
     */
    suspend fun openPdf(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                closePdfInternal()

                // Copy to temp file to avoid SAF fd issues with PdfRenderer
                val tempFile = copyToTempFile(uri)
                val fd = ParcelFileDescriptor.open(
                    tempFile,
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
                fileDescriptor = fd

                val renderer = PdfRenderer(fd)
                pdfRenderer = renderer

                if (renderer.pageCount == 0) {
                    return@withContext Result.failure(IOException("PDF has no pages"))
                }

                Result.success(renderer.pageCount)
            } catch (e: SecurityException) {
                Result.failure(SecurityException("PDF may be password-protected"))
            } catch (e: IOException) {
                Result.failure(IOException("Unable to open PDF file: ${e.message}"))
            } catch (e: Exception) {
                Result.failure(Exception("Unable to open PDF file: ${e.message}"))
            }
        }
    }

    /**
     * Renders the specified page to a Bitmap.
     *
     * @param pageIndex Zero-based page index
     * @param scale Scale factor for rendering quality (1.0 = 72dpi, 2.0 = 144dpi)
     * @return Bitmap of the rendered page, or null on failure
     */
    suspend fun renderPage(pageIndex: Int, scale: Float = 2.0f): Bitmap? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val renderer = pdfRenderer ?: return@withContext null
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                        return@withContext null
                    }

                    // Close any previously opened page
                    currentPage?.close()

                    val page = renderer.openPage(pageIndex)
                    currentPage = page

                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    page.close()
                    currentPage = null

                    bitmap
                } catch (e: Exception) {
                    null
                }
            }
        }

    /**
     * Gets the dimensions of a specific page.
     *
     * @return Pair of (width, height) in points, or null if unavailable
     */
    suspend fun getPageDimensions(pageIndex: Int): Pair<Int, Int>? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    val renderer = pdfRenderer ?: return@withContext null
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                        return@withContext null
                    }

                    currentPage?.close()
                    val page = renderer.openPage(pageIndex)
                    val dimensions = Pair(page.width, page.height)
                    page.close()
                    currentPage = null

                    dimensions
                } catch (e: Exception) {
                    null
                }
            }
        }

    /**
     * Closes the current PDF and releases all resources.
     */
    suspend fun closePdf() {
        mutex.withLock {
            closePdfInternal()
        }
    }

    private fun closePdfInternal() {
        try {
            currentPage?.close()
        } catch (_: Exception) {}
        currentPage = null

        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        pdfRenderer = null

        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        fileDescriptor = null
    }

    /**
     * Copies a SAF URI file to a temporary file for PdfRenderer compatibility.
     */
    private fun copyToTempFile(uri: Uri): File {
        val tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Cannot open input stream for URI")
        return tempFile
    }

    /**
     * Cleans up temporary PDF files in cache directory.
     */
    suspend fun cleanupTempFiles() = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.listFiles()?.filter {
                it.name.startsWith("temp_pdf_") && it.name.endsWith(".pdf")
            }?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}
