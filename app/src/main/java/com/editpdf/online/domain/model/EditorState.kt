/**
 * Purpose: Editor state model for the PDF editor
 * Caller: EditorViewModel
 * Dependencies: PdfEditObject
 * Main Functions: Defines editor state, tool modes, undo stack
 * Side Effects: None (pure data model)
 */
package com.editpdf.online.domain.model

/**
 * Available editing tools in the editor toolbar.
 */
enum class EditorTool {
    NONE,
    TEXT,
    COVER,
    REPLACE,
    SIGN,
    CHECKMARK
}

/**
 * Represents the current state of the PDF editor.
 */
data class EditorState(
    val pdfUri: String = "",
    val fileName: String = "",
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val activeTool: EditorTool = EditorTool.NONE,
    val editObjects: Map<Int, List<PdfEditObject>> = emptyMap(),
    val selectedObjectId: String? = null,
    val undoStack: List<UndoAction> = emptyList(),
    val redoStack: List<UndoAction> = emptyList(),
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val showTextInput: Boolean = false,
    val showReplaceFlow: Boolean = false,
    val showExportDialog: Boolean = false,
    val exportSuccess: Boolean = false,
    val instructionText: String? = null
) {
    /**
     * Get edit objects for the current page.
     */
    val currentPageObjects: List<PdfEditObject>
        get() = editObjects[currentPage] ?: emptyList()

    /**
     * Check if there are any unsaved changes.
     */
    val hasChanges: Boolean
        get() = editObjects.values.any { it.isNotEmpty() }
}

/**
 * Represents an undoable/redoable action in the editor.
 */
sealed class UndoAction {
    data class AddObject(val obj: PdfEditObject) : UndoAction()
    data class RemoveObject(val obj: PdfEditObject) : UndoAction()
    data class ModifyObject(
        val oldObj: PdfEditObject,
        val newObj: PdfEditObject
    ) : UndoAction()
}
