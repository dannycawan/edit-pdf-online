/**
 * Purpose: ViewModel for PDF tools screen actions
 * Caller: ToolsScreen
 * Dependencies: PdfMergeManager, PdfPageToolManager, PdfConversionManager
 * Main Functions: merge, split, rotate, delete pages, image/pdf conversion
 * Side Effects: File I/O in app cache, analytics/ad action tracking
 */
package com.editpdf.online.ui.tools

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.editpdf.online.R
import com.editpdf.online.ads.AdFrequencyManager
import com.editpdf.online.analytics.AnalyticsTracker
import com.editpdf.online.config.RemoteConfigManager
import com.editpdf.online.pdf.PdfConversionManager
import com.editpdf.online.pdf.PdfMergeManager
import com.editpdf.online.pdf.PdfPageToolManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ToolsUiState(
    val isWorking: Boolean = false,
    val activeTool: String? = null,
    val selectedPdfUri: Uri? = null,
    val resultFiles: List<File> = emptyList(),
    val resultMessage: String? = null,
    val errorMessage: String? = null
)

class ToolsViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val mergeManager = PdfMergeManager(context)
    private val pageToolManager = PdfPageToolManager(context)
    private val conversionManager = PdfConversionManager(context)
    private val analyticsTracker = AnalyticsTracker(context)
    private val adFrequencyManager = AdFrequencyManager(RemoteConfigManager(context))

    private val _uiState = MutableStateFlow(ToolsUiState())
    val uiState: StateFlow<ToolsUiState> = _uiState.asStateFlow()

    fun prepareSinglePdfTool(toolId: String, uri: Uri) {
        _uiState.update {
            it.copy(
                activeTool = toolId,
                selectedPdfUri = uri,
                resultFiles = emptyList(),
                resultMessage = null,
                errorMessage = null
            )
        }
    }

    fun mergePdfs(uris: List<Uri>) {
        if (uris.size < 2) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.tools_error_select_two_pdfs)) }
            return
        }

        runTool("merge") {
            val outputFile = outputFile("merged_pdf.pdf")
            mergeManager.mergePdfs(uris, outputFile).map { listOf(it) }
        }
    }

    fun splitPdf(rangeText: String) {
        val sourceUri = _uiState.value.selectedPdfUri ?: return
        val ranges = parsePageRanges(rangeText)
        if (ranges.isEmpty()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.tools_error_page_ranges)) }
            return
        }

        runTool("split") {
            pageToolManager.splitPdf(sourceUri, ranges, outputDir("split")).map { it }
        }
    }

    fun rotatePdf(degrees: Int, pageText: String) {
        val sourceUri = _uiState.value.selectedPdfUri ?: return
        val pages = parsePages(pageText)
        if (pages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.tools_error_pages)) }
            return
        }

        runTool("rotate") {
            val outputFile = outputFile("rotated_pdf.pdf")
            pageToolManager.rotatePages(sourceUri, pages, degrees, outputFile).map { listOf(it) }
        }
    }

    fun deletePages(pageText: String) {
        val sourceUri = _uiState.value.selectedPdfUri ?: return
        val pages = parsePages(pageText)
        if (pages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.tools_error_pages)) }
            return
        }

        runTool("delete_pages") {
            val outputFile = outputFile("pages_removed.pdf")
            pageToolManager.deletePages(sourceUri, pages, outputFile).map { listOf(it) }
        }
    }

    fun imageToPdf(imageUris: List<Uri>) {
        if (imageUris.isEmpty()) {
            _uiState.update { it.copy(errorMessage = context.getString(R.string.tools_error_select_one_image)) }
            return
        }

        runTool("image_to_pdf") {
            val outputFile = outputFile("images_to_pdf.pdf")
            conversionManager.imageToPdf(imageUris, outputFile).map { listOf(it) }
        }
    }

    fun pdfToImages(uri: Uri) {
        runTool("pdf_to_image") {
            conversionManager.pdfToImages(uri, outputDir("pdf_images")).map { it }
        }
    }

    fun dismissMessages() {
        _uiState.update { it.copy(errorMessage = null, resultMessage = null) }
    }

    fun clearActiveDialog() {
        _uiState.update { it.copy(activeTool = null, selectedPdfUri = null) }
    }

    private fun runTool(toolName: String, block: suspend () -> Result<List<File>>) {
        viewModelScope.launch {
            analyticsTracker.trackToolUsed(toolName)
            _uiState.update {
                it.copy(
                    isWorking = true,
                    activeTool = toolName,
                    resultFiles = emptyList(),
                    resultMessage = null,
                    errorMessage = null
                )
            }

            val result = block()
            result.fold(
                onSuccess = { files ->
                    adFrequencyManager.recordSuccessfulAction()
                    analyticsTracker.trackToolSuccess(toolName)
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            resultFiles = files,
                            resultMessage = context.getString(R.string.tools_result_files_created, files.size),
                            activeTool = null,
                            selectedPdfUri = null
                        )
                    }
                },
                onFailure = { error ->
                    analyticsTracker.trackToolFailed(toolName, error.message ?: "unknown")
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            errorMessage = error.message ?: context.getString(R.string.tools_error_failed)
                        )
                    }
                }
            )
        }
    }

    private fun outputDir(name: String): File =
        File(context.cacheDir, "pdf_tools/$name/${System.currentTimeMillis()}").also { it.mkdirs() }

    private fun outputFile(name: String): File =
        File(outputDir("outputs"), "${System.currentTimeMillis()}_$name")

    private fun parsePages(text: String): List<Int> =
        text.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it > 0 }
            .map { it - 1 }
            .distinct()

    private fun parsePageRanges(text: String): List<IntRange> =
        text.split(",").mapNotNull { part ->
            val trimmed = part.trim()
            when {
                "-" in trimmed -> {
                    val pieces = trimmed.split("-", limit = 2)
                    val start = pieces.getOrNull(0)?.trim()?.toIntOrNull()
                    val end = pieces.getOrNull(1)?.trim()?.toIntOrNull()
                    if (start != null && end != null && start > 0 && end >= start) {
                        (start - 1)..(end - 1)
                    } else null
                }
                else -> trimmed.toIntOrNull()?.takeIf { it > 0 }?.let { page -> (page - 1)..(page - 1) }
            }
        }
}
