/**
 * Purpose: PDF/image conversion helpers for V1.5 PDF tools
 * Caller: ToolsViewModel
 * Dependencies: PdfBox-Android, Android PdfRenderer
 * Main Functions: imageToPdf, pdfToImages
 * Side Effects: Creates PDF/PNG files in cache output folders
 */
package com.editpdf.online.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfConversionManager(private val context: Context) {

    suspend fun imageToPdf(imageUris: List<Uri>, outputFile: File): Result<File> =
        withContext(Dispatchers.IO) {
            if (imageUris.isEmpty()) return@withContext Result.failure(Exception("No images selected"))

            var document: PDDocument? = null
            try {
                document = PDDocument()

                imageUris.forEach { uri ->
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    } ?: return@withContext Result.failure(Exception("Cannot open image"))

                    val page = PDPage(PDRectangle(bitmap.width.toFloat(), bitmap.height.toFloat()))
                    document.addPage(page)

                    val image = LosslessFactory.createFromImage(document, bitmap)
                    PDPageContentStream(document, page).use { contentStream ->
                        contentStream.drawImage(image, 0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
                    }
                    bitmap.recycle()
                }

                document.save(FileOutputStream(outputFile))
                Result.success(outputFile)
            } catch (e: Exception) {
                Result.failure(Exception("Image to PDF failed: ${e.message}"))
            } finally {
                document?.close()
            }
        }

    suspend fun pdfToImages(sourceUri: Uri, outputDir: File): Result<List<File>> =
        withContext(Dispatchers.IO) {
            outputDir.mkdirs()
            val tempFile = File(outputDir, "source_${System.currentTimeMillis()}.pdf")

            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(tempFile).use { output -> input.copyTo(output) }
                } ?: return@withContext Result.failure(Exception("Cannot open PDF"))

                val outputFiles = mutableListOf<File>()
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        for (pageIndex in 0 until renderer.pageCount) {
                            renderer.openPage(pageIndex).use { page ->
                                val scale = 2
                                val bitmap = Bitmap.createBitmap(
                                    page.width * scale,
                                    page.height * scale,
                                    Bitmap.Config.ARGB_8888
                                )
                                val matrix = Matrix().apply {
                                    postScale(scale.toFloat(), scale.toFloat())
                                }
                                page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val outputFile = File(outputDir, "page_${pageIndex + 1}.png")
                                FileOutputStream(outputFile).use { output ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                                }
                                bitmap.recycle()
                                outputFiles.add(outputFile)
                            }
                        }
                    }
                }

                Result.success(outputFiles)
            } catch (e: Exception) {
                Result.failure(Exception("PDF to image failed: ${e.message}"))
            } finally {
                tempFile.delete()
            }
        }
}
