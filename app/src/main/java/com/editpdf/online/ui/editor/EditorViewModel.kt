/**
 * Purpose: ViewModel for the PDF editor screen
 * Caller: EditorScreen
 * Dependencies: PdfRendererManager, PdfExportManager, PdfEditorEngine, RecentFileRepository, FileUtils
 * Main Functions: PDF loading, page rendering, overlay management, undo/redo, export
 * Side Effects: File I/O, database updates, bitmap allocation
 */
package com.editpdf.online.ui.editor

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.editpdf.online.R
import com.editpdf.online.ads.AdFrequencyManager
import com.editpdf.online.ads.InterstitialAdManager
import com.editpdf.online.analytics.AnalyticsTracker
import com.editpdf.online.analytics.CrashReporter
import com.editpdf.online.config.RemoteConfigManager
import com.editpdf.online.data.repository.RecentFileRepository
import com.editpdf.online.domain.model.EditorState
import com.editpdf.online.domain.model.EditorTool
import com.editpdf.online.domain.model.PdfEditObject
import com.editpdf.online.domain.model.UndoAction
import com.editpdf.online.pdf.PdfEditorEngine
import com.editpdf.online.pdf.PdfExportManager
import com.editpdf.online.pdf.PdfRendererManager
import com.editpdf.online.pdf.PdfSignatureManager
import com.editpdf.online.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Managers
    val pdfRendererManager = PdfRendererManager(context)
    private val pdfExportManager = PdfExportManager(context)
    val signatureManager = PdfSignatureManager(context)
    private val recentFileRepository = RecentFileRepository(context)
    private val analyticsTracker = AnalyticsTracker(context)
    private val adFrequencyManager = AdFrequencyManager(RemoteConfigManager())
    private val interstitialAdManager = InterstitialAdManager(context, adFrequencyManager)

    // State
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    // Current page bitmap
    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap.asStateFlow()

    // PDF page dimensions (in points)
    private var pdfPageWidth: Float = 0f
    private var pdfPageHeight: Float = 0f

    /**
     * Loads a PDF from the given URI string.
     */
    fun loadPdf(uriString: String) {
        if (uriString.isBlank()) return
        val uri = Uri.parse(uriString)

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            // Collect metadata without blocking on failures — SAF metadata can be
            // unavailable immediately after the picker returns on some devices/providers.
            val fileName = try { FileUtils.getFileName(context, uri) } catch (_: Exception) { "document.pdf" }
            val fileSize = try { FileUtils.getFileSize(context, uri) } catch (_: Exception) { 0L }

            // Skip isPdfFile/isFileTooLarge pre-checks — they open a stream which can
            // spuriously fail right after the picker returns, producing the false
            // "Akses file kedaluwarsa" error.  PdfRendererManager does the real
            // validation during openPdf() with retry logic.

            val result = pdfRendererManager.openPdf(uri)
            result.fold(
                onSuccess = { pageCount ->
                    _state.update {
                        it.copy(
                            pdfUri = uriString,
                            fileName = fileName,
                            totalPages = pageCount,
                            currentPage = 0,
                            isLoading = false,
                            errorMessage = null
                        )
                    }

                    // Track analytics
                    analyticsTracker.trackPdfOpenSuccess(pageCount, fileSize / (1024f * 1024f))

                    // Render first page
                    renderCurrentPage()

                    // Update recent files
                    recentFileRepository.addOrUpdateRecentFile(
                        fileName = fileName,
                        fileUri = uriString,
                        fileSizeBytes = fileSize,
                        pageCount = pageCount
                    )
                },
                onFailure = { error ->
                    analyticsTracker.trackPdfOpenFailed(error.message ?: "unknown")
                    CrashReporter.logError(error, "EditorViewModel.loadPdf")

                    val userMessage = when (error) {
                        is SecurityException -> {
                            context.getString(R.string.error_password_pdf)
                        }
                        is java.io.IOException -> {
                            context.getString(R.string.error_open_pdf)
                        }
                        else -> {
                            context.getString(R.string.error_open_pdf)
                        }
                    }

                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = userMessage
                        )
                    }
                }
            )
        }
    }

    /**
     * Renders the current page to a bitmap.
     */
    private fun renderCurrentPage() {
        viewModelScope.launch {
            val pageIndex = _state.value.currentPage
            val bitmap = pdfRendererManager.renderPage(pageIndex)
            _pageBitmap.value = bitmap

            // Get page dimensions for coordinate mapping
            pdfRendererManager.getPageDimensions(pageIndex)?.let { (w, h) ->
                pdfPageWidth = w.toFloat()
                pdfPageHeight = h.toFloat()
            }
        }
    }

    /**
     * Navigates to the specified page.
     */
    fun goToPage(pageIndex: Int) {
        val totalPages = _state.value.totalPages
        if (pageIndex < 0 || pageIndex >= totalPages) return

        _state.update { it.copy(currentPage = pageIndex) }
        renderCurrentPage()
    }

    /**
     * Goes to the next page.
     */
    fun nextPage() = goToPage(_state.value.currentPage + 1)

    /**
     * Goes to the previous page.
     */
    fun previousPage() = goToPage(_state.value.currentPage - 1)

    /**
     * Sets the active editing tool.
     */
    fun selectTool(tool: EditorTool) {
        trackToolSelection(tool)
        _state.update {
            it.copy(
                activeTool = if (it.activeTool == tool) EditorTool.NONE else tool,
                selectedObjectId = null,
                instructionText = when (tool) {
                    EditorTool.TEXT -> context.getString(R.string.editor_tap_to_add_text)
                    EditorTool.COVER -> context.getString(R.string.editor_cover_instruction)
                    EditorTool.REPLACE -> context.getString(R.string.editor_replace_instruction)
                    EditorTool.SIGN -> context.getString(R.string.editor_signature_instruction)
                    EditorTool.CHECKMARK -> context.getString(R.string.editor_tap_to_add_text)
                    EditorTool.NONE -> null
                }
            )
        }
    }

    private fun trackToolSelection(tool: EditorTool) {
        when (tool) {
            EditorTool.TEXT -> analyticsTracker.trackAddTextClicked()
            EditorTool.COVER -> analyticsTracker.trackCoverOldTextClicked()
            EditorTool.REPLACE -> analyticsTracker.trackReplaceTextClicked()
            EditorTool.SIGN -> analyticsTracker.trackSignatureClicked()
            EditorTool.CHECKMARK -> analyticsTracker.trackFillFormClicked()
            EditorTool.NONE -> Unit
        }
    }

    /**
     * Adds a text object at the given screen position.
     */
    fun addTextObject(
        text: String,
        screenX: Float,
        screenY: Float,
        viewWidth: Float,
        viewHeight: Float,
        fontSize: Float = 14f,
        color: Int = 0xFF000000.toInt(),
        isBold: Boolean = false
    ) {
        if (text.isBlank()) return

        val (pdfX, pdfY) = PdfEditorEngine.screenToPdf(
            screenX, screenY, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight
        )

        val pdfFontSize = PdfEditorEngine.screenSizeToPdf(fontSize, viewHeight, pdfPageHeight)

        val obj = PdfEditObject.TextObject(
            pageIndex = _state.value.currentPage,
            text = text,
            x = pdfX,
            y = pdfY,
            fontSize = pdfFontSize,
            color = color,
            isBold = isBold
        )

        addObject(obj)
        _state.update { it.copy(showTextInput = false) }
    }

    /**
     * Adds a cover rectangle at the given screen position and size.
     */
    fun addCoverObject(
        screenX: Float,
        screenY: Float,
        screenWidth: Float,
        screenHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val (pdfX, pdfY) = PdfEditorEngine.screenToPdf(
            screenX, screenY, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight
        )
        val pdfW = PdfEditorEngine.screenSizeToPdf(screenWidth, viewWidth, pdfPageWidth)
        val pdfH = PdfEditorEngine.screenSizeToPdf(screenHeight, viewHeight, pdfPageHeight)

        val obj = PdfEditObject.CoverObject(
            pageIndex = _state.value.currentPage,
            x = pdfX,
            y = pdfY,
            width = pdfW,
            height = pdfH
        )

        addObject(obj)
    }

    /**
     * Completes a cover selection. Replace mode continues directly to the
     * text dialog and anchors the replacement text at the selected area.
     */
    fun onCoverAreaSelected(
        screenX: Float,
        screenY: Float,
        screenWidth: Float,
        screenHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        addCoverObject(screenX, screenY, screenWidth, screenHeight, viewWidth, viewHeight)
        if (_state.value.activeTool == EditorTool.REPLACE) {
            pendingTapX = screenX
            pendingTapY = screenY
            pendingViewWidth = viewWidth
            pendingViewHeight = viewHeight
            showTextInput(true)
        }
    }

    /**
     * Adds a signature object at the given screen position.
     */
    fun addSignatureObject(
        imagePath: String,
        screenX: Float,
        screenY: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val (pdfX, pdfY) = PdfEditorEngine.screenToPdf(
            screenX, screenY, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight
        )

        val obj = PdfEditObject.SignatureObject(
            pageIndex = _state.value.currentPage,
            imagePath = imagePath,
            x = pdfX,
            y = pdfY
        )

        addObject(obj)
    }

    /**
     * Stores where the next signature should be placed after the drawing screen returns.
     */
    fun storePendingTap(x: Float, y: Float, viewWidth: Float, viewHeight: Float) {
        pendingTapX = x
        pendingTapY = y
        pendingViewWidth = viewWidth
        pendingViewHeight = viewHeight
    }

    /**
     * Places a saved signature image at the last tapped PDF position.
     */
    fun placeSignatureAtPendingTap(imagePath: String) {
        if (imagePath.isBlank() || pendingViewWidth <= 0f || pendingViewHeight <= 0f) return
        addSignatureObject(
            imagePath = imagePath,
            screenX = pendingTapX,
            screenY = pendingTapY,
            viewWidth = pendingViewWidth,
            viewHeight = pendingViewHeight
        )
    }

    /**
     * Adds a checkmark at the given screen position.
     */
    fun addCheckmarkObject(
        screenX: Float,
        screenY: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val (pdfX, pdfY) = PdfEditorEngine.screenToPdf(
            screenX, screenY, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight
        )

        val obj = PdfEditObject.CheckmarkObject(
            pageIndex = _state.value.currentPage,
            x = pdfX,
            y = pdfY
        )

        addObject(obj)
    }

    /**
     * Core function to add an edit object and push undo action.
     */
    private fun addObject(obj: PdfEditObject) {
        _state.update { state ->
            val pageObjects = state.editObjects[state.currentPage]?.toMutableList() ?: mutableListOf()
            pageObjects.add(obj)
            val newEditObjects = state.editObjects.toMutableMap()
            newEditObjects[state.currentPage] = pageObjects

            state.copy(
                editObjects = newEditObjects,
                undoStack = state.undoStack + UndoAction.AddObject(obj),
                redoStack = emptyList()
            )
        }
    }

    /**
     * Selects an overlay object by ID.
     */
    fun selectObject(id: String?) {
        _state.update { it.copy(selectedObjectId = id) }
    }

    /**
     * Moves a selected object to a new screen position.
     */
    fun moveObject(
        objectId: String,
        newScreenX: Float,
        newScreenY: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val (pdfX, pdfY) = PdfEditorEngine.screenToPdf(
            newScreenX, newScreenY, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight
        )

        _state.update { state ->
            val page = state.currentPage
            val pageObjects = state.editObjects[page]?.toMutableList() ?: return@update state
            val index = pageObjects.indexOfFirst { it.id == objectId }
            if (index == -1) return@update state

            val oldObj = pageObjects[index]
            val newObj = when (oldObj) {
                is PdfEditObject.TextObject -> oldObj.copy(x = pdfX, y = pdfY)
                is PdfEditObject.CoverObject -> oldObj.copy(x = pdfX, y = pdfY)
                is PdfEditObject.SignatureObject -> oldObj.copy(x = pdfX, y = pdfY)
                is PdfEditObject.CheckmarkObject -> oldObj.copy(x = pdfX, y = pdfY)
            }

            pageObjects[index] = newObj
            val newEditObjects = state.editObjects.toMutableMap()
            newEditObjects[page] = pageObjects

            state.copy(editObjects = newEditObjects)
        }
    }

    /** Resizes a signature and records the change as one undoable action. */
    fun resizeSignatureObject(
        objectId: String,
        newScreenWidth: Float,
        newScreenHeight: Float,
        viewWidth: Float,
        viewHeight: Float
    ) {
        val pdfWidth = PdfEditorEngine.screenSizeToPdf(newScreenWidth, viewWidth, pdfPageWidth)
        val pdfHeight = PdfEditorEngine.screenSizeToPdf(newScreenHeight, viewHeight, pdfPageHeight)

        _state.update { state ->
            val page = state.currentPage
            val pageObjects = state.editObjects[page]?.toMutableList() ?: return@update state
            val index = pageObjects.indexOfFirst { it.id == objectId }
            if (index == -1) return@update state

            val oldObject = pageObjects[index] as? PdfEditObject.SignatureObject ?: return@update state
            val newObject = oldObject.copy(
                width = pdfWidth.coerceAtLeast(24f),
                height = pdfHeight.coerceAtLeast(12f)
            )
            if (oldObject == newObject) return@update state

            pageObjects[index] = newObject
            val newEditObjects = state.editObjects.toMutableMap()
            newEditObjects[page] = pageObjects
            state.copy(
                editObjects = newEditObjects,
                undoStack = state.undoStack + UndoAction.ModifyObject(oldObject, newObject),
                redoStack = emptyList()
            )
        }
    }

    /**
     * Deletes the currently selected object.
     */
    fun deleteSelectedObject() {
        val selectedId = _state.value.selectedObjectId ?: return

        _state.update { state ->
            val page = state.currentPage
            val pageObjects = state.editObjects[page]?.toMutableList() ?: return@update state
            val obj = pageObjects.find { it.id == selectedId } ?: return@update state

            pageObjects.remove(obj)
            val newEditObjects = state.editObjects.toMutableMap()
            newEditObjects[page] = pageObjects

            state.copy(
                editObjects = newEditObjects,
                selectedObjectId = null,
                undoStack = state.undoStack + UndoAction.RemoveObject(obj),
                redoStack = emptyList()
            )
        }
    }

    /**
     * Undo the last action.
     */
    fun undo() {
        _state.update { state ->
            if (state.undoStack.isEmpty()) return@update state

            val lastAction = state.undoStack.last()
            val newUndoStack = state.undoStack.dropLast(1)

            when (lastAction) {
                is UndoAction.AddObject -> {
                    // Remove the added object
                    val page = lastAction.obj.pageIndex
                    val pageObjects = state.editObjects[page]?.toMutableList() ?: return@update state
                    pageObjects.removeAll { it.id == lastAction.obj.id }
                    val newEditObjects = state.editObjects.toMutableMap()
                    newEditObjects[page] = pageObjects

                    state.copy(
                        editObjects = newEditObjects,
                        undoStack = newUndoStack,
                        redoStack = state.redoStack + lastAction,
                        selectedObjectId = null
                    )
                }
                is UndoAction.RemoveObject -> {
                    // Re-add the removed object
                    val page = lastAction.obj.pageIndex
                    val pageObjects = state.editObjects[page]?.toMutableList() ?: mutableListOf()
                    pageObjects.add(lastAction.obj)
                    val newEditObjects = state.editObjects.toMutableMap()
                    newEditObjects[page] = pageObjects

                    state.copy(
                        editObjects = newEditObjects,
                        undoStack = newUndoStack,
                        redoStack = state.redoStack + lastAction
                    )
                }
                is UndoAction.ModifyObject -> {
                    // Restore old object
                    val page = lastAction.oldObj.pageIndex
                    val pageObjects = state.editObjects[page]?.toMutableList() ?: return@update state
                    val index = pageObjects.indexOfFirst { it.id == lastAction.newObj.id }
                    if (index != -1) pageObjects[index] = lastAction.oldObj
                    val newEditObjects = state.editObjects.toMutableMap()
                    newEditObjects[page] = pageObjects

                    state.copy(
                        editObjects = newEditObjects,
                        undoStack = newUndoStack,
                        redoStack = state.redoStack + lastAction
                    )
                }
            }
        }
    }

    /**
     * Redo the last undone action.
     */
    fun redo() {
        _state.update { state ->
            if (state.redoStack.isEmpty()) return@update state

            val lastAction = state.redoStack.last()
            val newRedoStack = state.redoStack.dropLast(1)

            when (lastAction) {
                is UndoAction.AddObject -> {
                    // Re-add the object
                    val page = lastAction.obj.pageIndex
                    val pageObjects = state.editObjects[page]?.toMutableList() ?: mutableListOf()
                    pageObjects.add(lastAction.obj)
                    val newEditObjects = state.editObjects.toMutableMap()
                    newEditObjects[page] = pageObjects

                    state.copy(
                        editObjects = newEditObjects,
                        undoStack = state.undoStack + lastAction,
                        redoStack = newRedoStack
                    )
                }
                is UndoAction.RemoveObject -> {
                    // Remove again
                    val page = lastAction.obj.pageIndex
                    val pageObjects = state.editObjects[page]?.toMutableList() ?: return@update state
                    pageObjects.removeAll { it.id == lastAction.obj.id }
                    val newEditObjects = state.editObjects.toMutableMap()
                    newEditObjects[page] = pageObjects

                    state.copy(
                        editObjects = newEditObjects,
                        undoStack = state.undoStack + lastAction,
                        redoStack = newRedoStack,
                        selectedObjectId = null
                    )
                }
                is UndoAction.ModifyObject -> {
                    val page = lastAction.newObj.pageIndex
                    val pageObjects = state.editObjects[page]?.toMutableList() ?: return@update state
                    val index = pageObjects.indexOfFirst { it.id == lastAction.oldObj.id }
                    if (index != -1) pageObjects[index] = lastAction.newObj
                    val newEditObjects = state.editObjects.toMutableMap()
                    newEditObjects[page] = pageObjects

                    state.copy(
                        editObjects = newEditObjects,
                        undoStack = state.undoStack + lastAction,
                        redoStack = newRedoStack
                    )
                }
            }
        }
    }

    /**
     * Shows/hides the text input dialog.
     */
    fun showTextInput(show: Boolean) {
        _state.update { it.copy(showTextInput = show) }
    }

    /**
     * Shows/hides the export dialog.
     */
    fun showExportDialog(show: Boolean) {
        _state.update { it.copy(showExportDialog = show, exportSuccess = false) }
    }

    /**
     * Exports the edited PDF to a file.
     */
    fun exportPdf(outputFileName: String) {
        if (!_state.value.hasChanges) return

        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, errorMessage = null) }
            analyticsTracker.trackExportClicked()

            val sourceUri = Uri.parse(_state.value.pdfUri)
            val outputFile = FileUtils.createTempOutputFile(context, outputFileName)

            val result = pdfExportManager.exportToFile(
                sourceUri = sourceUri,
                editObjects = _state.value.editObjects,
                outputFile = outputFile
            )

            result.fold(
                onSuccess = {
                    // Update recent file as exported
                    recentFileRepository.markAsExported(_state.value.pdfUri)

                    // Track analytics
                    analyticsTracker.trackExportSuccess(_state.value.totalPages)

                    // Show interstitial ad with frequency cap
                    adFrequencyManager.recordSuccessfulAction()

                    _state.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = true,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    analyticsTracker.trackExportFailed(error.message ?: "unknown")
                    CrashReporter.logError(error, "EditorViewModel.exportPdf")
                    _state.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = false,
                            errorMessage = error.message ?: context.getString(R.string.error_export_pdf)
                        )
                    }
                }
            )
        }
    }

    /**
     * Exports PDF to a SAF-chosen location.
     */
    fun exportPdfToUri(outputUri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true, errorMessage = null) }

            val sourceUri = Uri.parse(_state.value.pdfUri)
            val outputStream = context.contentResolver.openOutputStream(outputUri)

            if (outputStream == null) {
                _state.update { it.copy(isExporting = false, errorMessage = context.getString(R.string.error_save_pdf)) }
                return@launch
            }

            val result = pdfExportManager.exportPdf(
                sourceUri = sourceUri,
                editObjects = _state.value.editObjects,
                outputStream = outputStream
            )
            outputStream.close()

            result.fold(
                onSuccess = {
                    recentFileRepository.markAsExported(_state.value.pdfUri)
                    analyticsTracker.trackExportSuccess(_state.value.totalPages)
                    adFrequencyManager.recordSuccessfulAction()
                    _state.update {
                        it.copy(isExporting = false, exportSuccess = true, errorMessage = null)
                    }
                },
                onFailure = { error ->
                    analyticsTracker.trackExportFailed(error.message ?: "unknown")
                    CrashReporter.logError(error, "EditorViewModel.exportPdfToUri")
                    _state.update {
                        it.copy(
                            isExporting = false,
                            exportSuccess = false,
                            errorMessage = error.message ?: context.getString(R.string.error_export_pdf)
                        )
                    }
                }
            )
        }
    }

    /**
     * Gets the exported file for sharing.
     */
    fun getExportedFile(): File? {
        val fileName = FileUtils.generateOutputFileName(_state.value.fileName)
        val file = FileUtils.createTempOutputFile(context, fileName)
        return if (file.exists()) file else null
    }

    /**
     * Dismisses error message.
     */
    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    /**
     * Stores the tap position for text input dialog.
     */
    private var pendingTapX: Float = 0f
    private var pendingTapY: Float = 0f
    private var pendingViewWidth: Float = 0f
    private var pendingViewHeight: Float = 0f

    fun onCanvasTap(x: Float, y: Float, viewWidth: Float, viewHeight: Float) {
        pendingTapX = x
        pendingTapY = y
        pendingViewWidth = viewWidth
        pendingViewHeight = viewHeight

        when (_state.value.activeTool) {
            EditorTool.TEXT -> showTextInput(true)
            EditorTool.CHECKMARK -> addCheckmarkObject(x, y, viewWidth, viewHeight)
            EditorTool.SIGN -> {
                storePendingTap(x, y, viewWidth, viewHeight)
            }
            else -> {
                // Deselect if tapping on empty area
                selectObject(null)
            }
        }
    }

    /**
     * Called when the text input dialog confirms text.
     */
    fun onTextInputConfirm(text: String, fontSize: Float, color: Int, isBold: Boolean) {
        addTextObject(text, pendingTapX, pendingTapY, pendingViewWidth, pendingViewHeight, fontSize, color, isBold)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            pdfRendererManager.closePdf()
            pdfRendererManager.cleanupTempFiles()
        }
    }
}
