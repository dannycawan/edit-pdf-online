/**
 * Purpose: Sealed class hierarchy for PDF edit overlay objects
 * Caller: EditorViewModel, PdfExportManager
 * Dependencies: None (pure data model)
 * Main Functions: Defines TextObject, CoverObject, SignatureObject, CheckmarkObject
 * Side Effects: None
 */
package com.editpdf.online.domain.model

import java.util.UUID

/**
 * Represents an edit overlay object placed on a PDF page.
 * All objects are visual overlays exported on top of the original PDF.
 */
sealed class PdfEditObject {
    abstract val id: String
    abstract val pageIndex: Int
    abstract val x: Float
    abstract val y: Float

    /**
     * Text overlay placed on a PDF page.
     * Used for Add Text, Replace Text (new text part), and Fill Form text fields.
     */
    data class TextObject(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val text: String,
        override val x: Float,
        override val y: Float,
        val width: Float = 200f,
        val height: Float = 40f,
        val fontSize: Float = 14f,
        val color: Int = 0xFF000000.toInt(),
        val fontFamily: String = "default",
        val isBold: Boolean = false,
        val isItalic: Boolean = false
    ) : PdfEditObject()

    /**
     * White rectangle overlay used to cover/hide existing text on a PDF page.
     * Used for Cover Old Text and Replace Text (cover part).
     */
    data class CoverObject(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val x: Float,
        override val y: Float,
        val width: Float,
        val height: Float,
        val color: Int = 0xFFFFFFFF.toInt()
    ) : PdfEditObject()

    /**
     * Signature image overlay placed on a PDF page.
     * The signature is stored as a transparent PNG file path.
     */
    data class SignatureObject(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val imagePath: String,
        override val x: Float,
        override val y: Float,
        val width: Float = 200f,
        val height: Float = 80f
    ) : PdfEditObject()

    /**
     * Checkmark overlay for form filling.
     * Renders a ✓ character at the specified position.
     */
    data class CheckmarkObject(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        override val x: Float,
        override val y: Float,
        val size: Float = 24f,
        val color: Int = 0xFF000000.toInt()
    ) : PdfEditObject()
}
