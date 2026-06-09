/**
 * Purpose: Manages Android PdfRenderer for rendering PDF pages to Bitmaps
 * Caller: EditorViewModel, EditorScreen
 * Dependencies: Android PdfRenderer, ParcelFileDescriptor
 * Main Functions: openPdf, renderPage, closePdf, getPageCount
 * Side Effects: Opens file descriptors, allocates Bitmaps, copies PDFs to cache
 */
package com.editpdf.online.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.editpdf.online.analytics.CrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private var cachedPdfFile: File? = null
    private val mutex = Mutex()

    /**
     * Total number of pages in the currently opened PDF.
     */
    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: 0

    /**
     * Opens a PDF file from the given URI.
     * Tries to copy to a temp file first (most reliable), then falls back to direct SAF FD.
     * Does NOT do an upfront canAccessUri() check — that check itself can fail spuriously
     * right after the SAF picker returns, causing false "expired" errors.
     */
    suspend fun openPdf(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                closePdfInternal()

                // Strategy 1: copy to temp file and open from local file
                val tempFile = copyToTempFileWithRetry(uri)
                if (tempFile != null && tempFile.length() > 0) {
                    cachedPdfFile = tempFile
                    val fileResult = openFromFile(tempFile)
                    if (fileResult != null) {
                        if (fileResult.isSuccess) return@withContext fileResult
                        if (fileResult.exceptionOrNull() is SecurityException) return@withContext fileResult
                    }
                    closeRendererOnly()
                }

                // Strategy 2: open directly via SAF file descriptor
                val directResult = openFromSafDirect(uri)
                if (directResult != null) return@withContext directResult
                closeRendererOnly()

                Result.failure(IOException("Unable to open this PDF. The file may be corrupted, encrypted, or unsupported."))
            } catch (e: SecurityException) {
                CrashReporter.logError(e, "PdfRendererManager.openPdf SecurityException")
                Result.failure(e)
            } catch (e: IOException) {
                CrashReporter.logError(e, "PdfRendererManager.openPdf IOException")
                Result.failure(IOException("Unable to open PDF file: ${e.message}"))
            } catch (e: Exception) {
                CrashReporter.logError(e, "PdfRendererManager.openPdf")
                Result.failure(Exception("Unable to open PDF file: ${e.message}"))
            }
        }
    }

    /**
     * Attempts to open a PdfRenderer from a local file.
     * Returns null if Android PdfRenderer rejects the file for non-password reasons.
     */
    private fun openFromFile(file: File): Result<Int>? {
        return try {
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor = fd

            val renderer = PdfRenderer(fd)
            pdfRenderer = renderer

            if (renderer.pageCount == 0) {
                closeRendererOnly()
                return Result.failure(IOException("PDF has no pages"))
            }

            Result.success(renderer.pageCount)
        } catch (e: SecurityException) {
            CrashReporter.logError(e, "PdfRendererManager.openFromFile SecurityException")
            closeRendererOnly()
            Result.failure(SecurityException("PDF may be password-protected"))
        } catch (e: IOException) {
            CrashReporter.logError(e, "PdfRendererManager.openFromFile IOException")
            closeRendererOnly()
            null
        } catch (e: Exception) {
            CrashReporter.logError(e, "PdfRendererManager.openFromFile")
            closeRendererOnly()
            null
        }
    }

    /**
     * Fallback: open the PDF directly from SAF content resolver file descriptor.
     */
    private fun openFromSafDirect(uri: Uri): Result<Int>? {
        return try {
            val afd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            fileDescriptor = afd

            val renderer = PdfRenderer(afd)
            pdfRenderer = renderer

            if (renderer.pageCount == 0) {
                closeRendererOnly()
                return Result.failure(IOException("PDF has no pages"))
            }

            Result.success(renderer.pageCount)
        } catch (e: SecurityException) {
            CrashReporter.logError(e, "PdfRendererManager.openFromSafDirect SecurityException")
            closeRendererOnly()
            Result.failure(SecurityException("PDF may be password-protected"))
        } catch (e: IOException) {
            CrashReporter.logError(e, "PdfRendererManager.openFromSafDirect IOException")
            closeRendererOnly()
            Result.failure(IOException("Unable to open PDF file: ${e.message}"))
        } catch (e: Exception) {
            CrashReporter.logError(e, "PdfRendererManager.openFromSafDirect")
            closeRendererOnly()
            null
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

                    currentPage?.close()
                    currentPage = null

                    val page = renderer.openPage(pageIndex)
                    currentPage = page

                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)

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
                    CrashReporter.logError(e, "PdfRendererManager.renderPage($pageIndex)")
                    try {
                        currentPage?.close()
                    } catch (_: Exception) {}
                    currentPage = null
                    null
                }
            }
        }

    /**
     * Gets the dimensions of a specific page.
     *
     * @return Pair of (width, height), or null if unavailable
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
                    currentPage = null

                    val page = renderer.openPage(pageIndex)
                    val dimensions = Pair(page.width, page.height)
                    page.close()
                    currentPage = null

                    dimensions
                } catch (e: Exception) {
                    CrashReporter.logError(e, "PdfRendererManager.getPageDimensions($pageIndex)")
                    try {
                        currentPage?.close()
                    } catch (_: Exception) {}
                    currentPage = null
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
        closeRendererOnly()
        try {
            cachedPdfFile?.delete()
        } catch (_: Exception) {}
        cachedPdfFile = null
    }

    private fun closeRendererOnly() {
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
     * Copies a SAF URI file to a temporary file with retry for SAF flakiness.
     * A short initial delay is used because on some devices the SAF provider needs
     * a moment after the picker result before the URI becomes fully readable.
     * Verifies the copy was successful by checking file size.
     */
    private suspend fun copyToTempFileWithRetry(uri: Uri, maxAttempts: Int = 3): File? {
        for (attempt in 1..maxAttempts) {
            try {
                // Give the SAF provider a moment to settle on first attempt
                if (attempt == 1) delay(100L)

                val tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}_$attempt.pdf")
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    delay(300L * attempt)
                } else {
                    inputStream.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        return tempFile
                    }

                    tempFile.delete()
                    CrashReporter.logMessage("PDF temp copy empty on attempt $attempt")
                    delay(300L * attempt)
                }
            } catch (e: SecurityException) {
                CrashReporter.logError(e, "PdfRendererManager.copyToTempFile SecurityException on attempt $attempt")
                if (attempt < maxAttempts) {
                    delay(500L * attempt)
                } else {
                    return null
                }
            } catch (e: Exception) {
                CrashReporter.logError(e, "PdfRendererManager.copyToTempFile attempt $attempt")
                if (attempt < maxAttempts) {
                    delay(300L * attempt)
                }
            }
        }
        return null
    }

    suspend fun cleanupTempFiles() = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.listFiles()
                ?.filter { it.name.startsWith("temp_pdf_") && it.name.endsWith(".pdf") }
                ?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}
