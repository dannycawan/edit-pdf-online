/**
 * Purpose: PDF merge and page tools using PdfBox-Android
 * Caller: ToolsViewModel
 * Dependencies: PdfBox-Android
 * Main Functions: mergePdfs, splitPdf, rotatePage, deletePages
 * Side Effects: Creates new PDF files on disk
 */
package com.editpdf.online.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PdfMergeManager(private val context: Context) {

    /**
     * Merges multiple PDF files into a single PDF.
     */
    suspend fun mergePdfs(uris: List<Uri>, outputFile: File): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                val merger = PDFMergerUtility()
                merger.destinationFileName = outputFile.absolutePath

                for (uri in uris) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure(Exception("Cannot open PDF"))
                    merger.addSource(inputStream)
                }

                merger.mergeDocuments(null)
                Result.success(outputFile)
            } catch (e: Exception) {
                Result.failure(Exception("Merge failed: ${e.message}"))
            }
        }
}

class PdfPageToolManager(private val context: Context) {

    /**
     * Splits a PDF by extracting a range of pages.
     */
    suspend fun splitPdf(
        sourceUri: Uri,
        pageRanges: List<IntRange>,
        outputDir: File
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open PDF"))

            document = PDDocument.load(inputStream)
            inputStream.close()

            val outputFiles = mutableListOf<File>()

            for ((index, range) in pageRanges.withIndex()) {
                val newDoc = PDDocument()
                for (pageIndex in range) {
                    if (pageIndex < document.numberOfPages) {
                        newDoc.importPage(document.getPage(pageIndex))
                    }
                }

                val outputFile = File(outputDir, "split_${index + 1}.pdf")
                newDoc.save(FileOutputStream(outputFile))
                newDoc.close()
                outputFiles.add(outputFile)
            }

            Result.success(outputFiles)
        } catch (e: Exception) {
            Result.failure(Exception("Split failed: ${e.message}"))
        } finally {
            document?.close()
        }
    }

    /**
     * Rotates specified pages by the given degrees (90, 180, 270).
     */
    suspend fun rotatePages(
        sourceUri: Uri,
        pageIndices: List<Int>,
        degrees: Int,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open PDF"))

            document = PDDocument.load(inputStream)
            inputStream.close()

            for (pageIndex in pageIndices) {
                if (pageIndex < document.numberOfPages) {
                    val page = document.getPage(pageIndex)
                    page.rotation = (page.rotation + degrees) % 360
                }
            }

            document.save(FileOutputStream(outputFile))
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(Exception("Rotate failed: ${e.message}"))
        } finally {
            document?.close()
        }
    }

    /**
     * Deletes specified pages from a PDF.
     */
    suspend fun deletePages(
        sourceUri: Uri,
        pageIndices: List<Int>,
        outputFile: File
    ): Result<File> = withContext(Dispatchers.IO) {
        var document: PDDocument? = null
        try {
            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return@withContext Result.failure(Exception("Cannot open PDF"))

            document = PDDocument.load(inputStream)
            inputStream.close()

            // Delete in reverse order to avoid index shifting
            val sortedIndices = pageIndices.sortedDescending()
            for (pageIndex in sortedIndices) {
                if (pageIndex < document.numberOfPages) {
                    document.removePage(pageIndex)
                }
            }

            document.save(FileOutputStream(outputFile))
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(Exception("Delete pages failed: ${e.message}"))
        } finally {
            document?.close()
        }
    }
}
