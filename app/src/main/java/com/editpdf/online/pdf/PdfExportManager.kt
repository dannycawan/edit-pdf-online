/**
 * Purpose: PDF export engine using PdfBox-Android
 * Caller: EditorViewModel (export flow)
 * Dependencies: PdfBox-Android, PdfEditObject models
 * Main Functions: exportPdf - applies all overlay objects to create a new PDF
 * Side Effects: Creates new PDF file on disk, reads original PDF
 */
package com.editpdf.online.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.editpdf.online.analytics.CrashReporter
import com.editpdf.online.domain.model.PdfEditObject
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class PdfExportManager(private val context: Context) {

    /**
     * Exports the edited PDF by applying all overlay objects to the original PDF.
     *
     * @param sourceUri URI of the original PDF file
     * @param editObjects Map of page index to list of edit objects
     * @param outputStream OutputStream to write the new PDF to
     * @return Result with success or failure
     */
    suspend fun exportPdf(
        sourceUri: Uri,
        editObjects: Map<Int, List<PdfEditObject>>,
        outputStream: OutputStream
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            // Load original PDF
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open source PDF"))

            document = PDDocument.load(inputStream)
            inputStream.close()

            // Apply edit objects to each page
            for ((pageIndex, objects) in editObjects) {
                if (pageIndex < 0 || pageIndex >= document.numberOfPages) continue
                val page = document.getPage(pageIndex)
                val mediaBox = page.mediaBox
                val pageHeight = mediaBox.height

                // Create content stream to append content
                val contentStream = PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true
                )

                for (obj in objects) {
                    when (obj) {
                        is PdfEditObject.CoverObject -> {
                            drawCoverObject(contentStream, obj, pageHeight, mediaBox)
                        }
                        is PdfEditObject.TextObject -> {
                            drawTextObject(contentStream, obj, pageHeight, mediaBox)
                        }
                        is PdfEditObject.SignatureObject -> {
                            drawSignatureObject(document, contentStream, obj, pageHeight, mediaBox)
                        }
                        is PdfEditObject.CheckmarkObject -> {
                            drawCheckmarkObject(contentStream, obj, pageHeight, mediaBox)
                        }
                    }
                }

                contentStream.close()
            }

            // Save to output stream
            document.save(outputStream)
            Result.success(Unit)
        } catch (e: Exception) {
            CrashReporter.logError(e, "PdfExportManager.exportPdf")
            Result.failure(Exception("Export failed: ${e.message}"))
        } finally {
            try {
                document?.close()
            } catch (_: Exception) {}
        }
    }

    /**
     * Exports to a file and returns the file path.
     */
    suspend fun exportToFile(
        sourceUri: Uri,
        editObjects: Map<Int, List<PdfEditObject>>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val outputStream = FileOutputStream(outputFile)
            val result = exportPdf(sourceUri, editObjects, outputStream)
            outputStream.close()

            if (result.isSuccess) {
                Result.success(outputFile)
            } else {
                outputFile.delete()
                Result.failure(result.exceptionOrNull() ?: Exception("Export failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Draws a white cover rectangle on the PDF page.
     * Coordinates are converted from screen space (top-left origin) to PDF space (bottom-left origin).
     */
    private fun drawCoverObject(
        contentStream: PDPageContentStream,
        obj: PdfEditObject.CoverObject,
        pageHeight: Float,
        mediaBox: PDRectangle
    ) {
        val scaleX = mediaBox.width / mediaBox.width // normalized
        val scaleY = mediaBox.height / mediaBox.height

        // Convert screen coordinates to PDF coordinates
        // Screen: origin top-left, Y increases downward
        // PDF: origin bottom-left, Y increases upward
        val pdfX = obj.x
        val pdfY = pageHeight - obj.y - obj.height

        val r = ((obj.color shr 16) and 0xFF) / 255f
        val g = ((obj.color shr 8) and 0xFF) / 255f
        val b = (obj.color and 0xFF) / 255f

        contentStream.setNonStrokingColor(r, g, b)
        contentStream.addRect(pdfX, pdfY, obj.width, obj.height)
        contentStream.fill()
    }

    /**
     * Draws text on the PDF page.
     */
    private fun drawTextObject(
        contentStream: PDPageContentStream,
        obj: PdfEditObject.TextObject,
        pageHeight: Float,
        mediaBox: PDRectangle
    ) {
        val font = if (obj.isBold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA

        // Convert color
        val r = ((obj.color shr 16) and 0xFF) / 255f
        val g = ((obj.color shr 8) and 0xFF) / 255f
        val b = (obj.color and 0xFF) / 255f

        // Convert screen coordinates to PDF coordinates
        val pdfX = obj.x
        val pdfY = pageHeight - obj.y - obj.fontSize

        contentStream.beginText()
        contentStream.setFont(font, obj.fontSize)
        contentStream.setNonStrokingColor(r, g, b)
        contentStream.newLineAtOffset(pdfX, pdfY)

        // Handle multi-line text
        val lines = obj.text.split("\n")
        for ((index, line) in lines.withIndex()) {
            if (index > 0) {
                contentStream.newLineAtOffset(0f, -obj.fontSize * 1.2f)
            }
            contentStream.showText(line)
        }

        contentStream.endText()
    }

    /**
     * Draws a signature image on the PDF page.
     */
    private fun drawSignatureObject(
        document: PDDocument,
        contentStream: PDPageContentStream,
        obj: PdfEditObject.SignatureObject,
        pageHeight: Float,
        mediaBox: PDRectangle
    ) {
        try {
            val bitmap = BitmapFactory.decodeFile(obj.imagePath) ?: return
            val pdImage = LosslessFactory.createFromImage(document, bitmap)

            val pdfX = obj.x
            val pdfY = pageHeight - obj.y - obj.height

            contentStream.drawImage(pdImage, pdfX, pdfY, obj.width, obj.height)
            bitmap.recycle()
        } catch (e: Exception) {
            CrashReporter.logError(e, "PdfExportManager.drawSignatureObject")
            // Skip signature if image cannot be loaded
        }
    }

    /**
     * Draws a checkmark character on the PDF page.
     */
    private fun drawCheckmarkObject(
        contentStream: PDPageContentStream,
        obj: PdfEditObject.CheckmarkObject,
        pageHeight: Float,
        mediaBox: PDRectangle
    ) {
        val r = ((obj.color shr 16) and 0xFF) / 255f
        val g = ((obj.color shr 8) and 0xFF) / 255f
        val b = (obj.color and 0xFF) / 255f

        val pdfX = obj.x
        val pdfY = pageHeight - obj.y - obj.size

        // Draw checkmark using lines
        contentStream.setStrokingColor(r, g, b)
        contentStream.setLineWidth(obj.size / 10f)

        // Checkmark path: short line down-right, then long line up-right
        val startX = pdfX
        val startY = pdfY + obj.size * 0.5f
        val midX = pdfX + obj.size * 0.3f
        val midY = pdfY + obj.size * 0.2f
        val endX = pdfX + obj.size * 0.9f
        val endY = pdfY + obj.size * 0.85f

        contentStream.moveTo(startX, startY)
        contentStream.lineTo(midX, midY)
        contentStream.lineTo(endX, endY)
        contentStream.stroke()
    }
}
