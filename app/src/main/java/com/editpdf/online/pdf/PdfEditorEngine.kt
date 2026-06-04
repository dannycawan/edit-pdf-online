/**
 * Purpose: Coordinate mapping between screen overlay space and PDF page space
 * Caller: PdfExportManager, EditorViewModel
 * Dependencies: PdfRendererManager (page dimensions)
 * Main Functions: screenToPdf, pdfToScreen coordinate transforms
 * Side Effects: None (pure utility)
 */
package com.editpdf.online.pdf

/**
 * Handles coordinate conversion between screen space (used by Compose overlays)
 * and PDF page space (used by PdfBox-Android for export).
 *
 * Screen space: origin at top-left, Y increases downward, pixels
 * PDF space: origin at bottom-left, Y increases upward, points (72 points/inch)
 */
object PdfEditorEngine {

    /**
     * Converts screen coordinates to PDF coordinates.
     *
     * @param screenX X position in screen pixels
     * @param screenY Y position in screen pixels
     * @param viewWidth Width of the rendered view in pixels
     * @param viewHeight Height of the rendered view in pixels
     * @param pdfPageWidth Width of the PDF page in points
     * @param pdfPageHeight Height of the PDF page in points
     * @return Pair of (pdfX, pdfY) in points
     */
    fun screenToPdf(
        screenX: Float,
        screenY: Float,
        viewWidth: Float,
        viewHeight: Float,
        pdfPageWidth: Float,
        pdfPageHeight: Float
    ): Pair<Float, Float> {
        val scaleX = pdfPageWidth / viewWidth
        val scaleY = pdfPageHeight / viewHeight

        val pdfX = screenX * scaleX
        val pdfY = screenY * scaleY // Y-flip is handled in export methods

        return Pair(pdfX, pdfY)
    }

    /**
     * Converts PDF coordinates to screen coordinates.
     */
    fun pdfToScreen(
        pdfX: Float,
        pdfY: Float,
        viewWidth: Float,
        viewHeight: Float,
        pdfPageWidth: Float,
        pdfPageHeight: Float
    ): Pair<Float, Float> {
        val scaleX = viewWidth / pdfPageWidth
        val scaleY = viewHeight / pdfPageHeight

        val screenX = pdfX * scaleX
        val screenY = pdfY * scaleY

        return Pair(screenX, screenY)
    }

    /**
     * Converts a size value from screen pixels to PDF points.
     */
    fun screenSizeToPdf(
        screenSize: Float,
        viewDimension: Float,
        pdfDimension: Float
    ): Float = screenSize * (pdfDimension / viewDimension)

    /**
     * Converts a size value from PDF points to screen pixels.
     */
    fun pdfSizeToScreen(
        pdfSize: Float,
        viewDimension: Float,
        pdfDimension: Float
    ): Float = pdfSize * (viewDimension / pdfDimension)
}
