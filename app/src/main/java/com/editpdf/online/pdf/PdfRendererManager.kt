package com.editpdf.online.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.editpdf.online.analytics.CrashReporter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class PdfRendererManager(private val context: Context) {

    private var pdfRenderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var currentPage: PdfRenderer.Page? = null

    // PdfBox fallback — used for validation when PdfRenderer fails
    private var pdfBoxDocument: PDDocument? = null
    private var usingPdfBox = false

    private var cachedPdfFile: File? = null
    private val mutex = Mutex()

    val pageCount: Int
        get() = pdfRenderer?.pageCount ?: pdfBoxDocument?.numberOfPages ?: 0

    /**
     * Opens a PDF from the given URI.
     *
     * Strategy:
     *   1. Copy to temp file + Android PdfRenderer (preferred for rendering)
     *   2. Direct SAF FD + PdfRenderer
     *   3. PdfBox validation (page count only) — if PdfRenderer fails but
     *      the file is valid, we still let the editor open (no preview).
     *      Export via PdfBox will still work.
     */
    suspend fun openPdf(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                closePdfInternal()
                usingPdfBox = false

                val tempFile = copyToTempFileWithRetry(uri)
                val copySucceeded = tempFile != null && tempFile.length() > 0

                // Strategy 1: temp file + PdfRenderer
                if (copySucceeded) {
                    cachedPdfFile = tempFile
                    val fileResult = openFromFile(tempFile)
                    if (fileResult != null) {
                        if (fileResult.isSuccess) return@withContext fileResult
                    }
                    closeRendererOnly()
                }

                // Strategy 2: direct SAF FD + PdfRenderer
                val directResult = openFromSafDirect(uri)
                if (directResult != null) {
                    if (directResult.isSuccess) return@withContext directResult
                    // SecurityException from PdfRenderer — try PdfBox fallback
                    if (directResult.exceptionOrNull() is SecurityException && tempFile != null) {
                        val pdfBoxResult = openWithPdfBox(tempFile)
                        if (pdfBoxResult != null) return@withContext pdfBoxResult
                    }
                    closeRendererOnly()
                }

                // Strategy 3: PdfBox fallback (validation only, no preview rendering)
                if (tempFile != null && tempFile.exists()) {
                    val pdfBoxResult = openWithPdfBox(tempFile)
                    if (pdfBoxResult != null) return@withContext pdfBoxResult
                }

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
            Result.failure(e)
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
            Result.failure(e)
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
     * PdfBox fallback: validate the file and get page count.
     * No rendering, but lets PdfExportManager (which uses PdfBox) work.
     */
    private fun openWithPdfBox(file: File): Result<Int>? {
        return try {
            val doc = PDDocument.load(file)
            if (doc.numberOfPages == 0) {
                doc.close()
                return Result.failure(IOException("PDF has no pages"))
            }
            pdfBoxDocument = doc
            usingPdfBox = true
            closeRendererOnly()
            Result.success(doc.numberOfPages)
        } catch (e: IOException) {
            CrashReporter.logError(e, "PdfRendererManager.openWithPdfBox IOException")
            try { pdfBoxDocument?.close() } catch (_: Exception) {}
            pdfBoxDocument = null
            usingPdfBox = false
            null
        } catch (e: Exception) {
            CrashReporter.logError(e, "PdfRendererManager.openWithPdfBox")
            try { pdfBoxDocument?.close() } catch (_: Exception) {}
            pdfBoxDocument = null
            usingPdfBox = false
            null
        }
    }

    suspend fun renderPage(pageIndex: Int, scale: Float = 2.0f): Bitmap? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    if (usingPdfBox) return@withLock null
                    val renderer = pdfRenderer ?: return@withLock null
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null

                    currentPage?.close()
                    currentPage = null

                    val page = renderer.openPage(pageIndex)
                    currentPage = page

                    val width = (page.width * scale).toInt().coerceAtLeast(1)
                    val height = (page.height * scale).toInt().coerceAtLeast(1)

                    val bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)

                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    currentPage = null
                    bitmap
                } catch (e: Exception) {
                    CrashReporter.logError(e, "PdfRendererManager.renderPage($pageIndex)")
                    try { currentPage?.close() } catch (_: Exception) {}
                    currentPage = null
                    null
                }
            }
        }

    suspend fun getPageDimensions(pageIndex: Int): Pair<Int, Int>? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                try {
                    if (usingPdfBox) {
                        return@withLock getPageDimensionsPdfBox(pageIndex)
                    }
                    val renderer = pdfRenderer ?: return@withLock null
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withLock null

                    currentPage?.close()
                    currentPage = null

                    val page = renderer.openPage(pageIndex)
                    val dimensions = Pair(page.width, page.height)
                    page.close()
                    currentPage = null
                    dimensions
                } catch (e: Exception) {
                    CrashReporter.logError(e, "PdfRendererManager.getPageDimensions($pageIndex)")
                    try { currentPage?.close() } catch (_: Exception) {}
                    currentPage = null
                    null
                }
            }
        }

    private fun getPageDimensionsPdfBox(pageIndex: Int): Pair<Int, Int>? {
        return try {
            val doc = pdfBoxDocument ?: return null
            if (pageIndex < 0 || pageIndex >= doc.numberOfPages) return null
            val page = doc.getPage(pageIndex)
            val mediaBox = page.mediaBox
            Pair(mediaBox.width.toInt(), mediaBox.height.toInt())
        } catch (e: Exception) {
            CrashReporter.logError(e, "PdfRendererManager.getPageDimensionsPdfBox($pageIndex)")
            null
        }
    }

    suspend fun closePdf() {
        mutex.withLock { closePdfInternal() }
    }

    private fun closePdfInternal() {
        closeRendererOnly()
        try { pdfBoxDocument?.close() } catch (_: Exception) {}
        pdfBoxDocument = null
        usingPdfBox = false
        try { cachedPdfFile?.delete() } catch (_: Exception) {}
        cachedPdfFile = null
    }

    private fun closeRendererOnly() {
        try { currentPage?.close() } catch (_: Exception) {}
        currentPage = null
        try { pdfRenderer?.close() } catch (_: Exception) {}
        pdfRenderer = null
        try { fileDescriptor?.close() } catch (_: Exception) {}
        fileDescriptor = null
    }

    private suspend fun copyToTempFileWithRetry(uri: Uri, maxAttempts: Int = 3): File? {
        for (attempt in 1..maxAttempts) {
            try {
                if (attempt == 1) delay(100L)

                val tempFile = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}_$attempt.pdf")
                val copied = copyUriToFile(uri, tempFile)
                if (copied && tempFile.exists() && tempFile.length() > 0) {
                    return tempFile
                }
                tempFile.delete()
                CrashReporter.logMessage("PDF temp copy empty on attempt $attempt")
                delay(300L * attempt)
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

    private fun copyUriToFile(uri: Uri, destination: File): Boolean {
        val stream = openInputStreamForCopy(uri) ?: return false
        stream.use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
        return true
    }

    private fun openInputStreamForCopy(uri: Uri): InputStream? {
        return try {
            context.contentResolver.openInputStream(uri)
        } catch (e: Exception) {
            CrashReporter.logError(e, "PdfRendererManager.openInputStreamForCopy openInputStream")
            null
        } ?: try {
            val fd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
            ParcelFileDescriptor.AutoCloseInputStream(fd)
        } catch (e: Exception) {
            CrashReporter.logError(e, "PdfRendererManager.openInputStreamForCopy openFileDescriptor")
            null
        }
    }

    suspend fun cleanupTempFiles() = withContext(Dispatchers.IO) {
        try {
            context.cacheDir.listFiles()
                ?.filter { it.name.startsWith("temp_pdf_") && it.name.endsWith(".pdf") }
                ?.forEach { it.delete() }
        } catch (_: Exception) {}
    }
}
