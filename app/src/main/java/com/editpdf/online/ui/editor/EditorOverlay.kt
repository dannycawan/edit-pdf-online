/**
 * Purpose: Overlay composable for rendering PdfEditObjects on the PDF canvas
 * Caller: EditorScreen
 * Dependencies: PdfEditObject, PdfEditorEngine
 * Main Functions: Renders Text/Cover/Signature/Checkmark overlays with selection + drag
 * Side Effects: None (pure UI)
 */
package com.editpdf.online.ui.editor

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editpdf.online.domain.model.PdfEditObject
import com.editpdf.online.pdf.PdfEditorEngine
import com.editpdf.online.ui.theme.HandleColor
import com.editpdf.online.ui.theme.SelectionBorder
import kotlin.math.roundToInt

/**
 * Renders a single PdfEditObject as a Compose overlay on the PDF canvas.
 *
 * @param obj The edit object to render
 * @param isSelected Whether the object is currently selected
 * @param viewWidth Width of the PDF view in pixels
 * @param viewHeight Height of the PDF view in pixels
 * @param pdfPageWidth PDF page width in points
 * @param pdfPageHeight PDF page height in points
 * @param onTap Called when the object is tapped
 * @param onDrag Called when the object is dragged to a new position (screenX, screenY)
 */
@Composable
fun EditorOverlayObject(
    obj: PdfEditObject,
    isSelected: Boolean,
    viewWidth: Float,
    viewHeight: Float,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    onTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
    onResize: (Float, Float) -> Unit
) {
    val density = LocalDensity.current

    // Convert PDF coordinates to screen coordinates
    val (screenX, screenY) = PdfEditorEngine.pdfToScreen(
        obj.x, obj.y, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight
    )

    var offsetX by remember(obj.id, obj.x) { mutableStateOf(screenX) }
    var offsetY by remember(obj.id, obj.y) { mutableStateOf(screenY) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(obj.id) {
                detectTapGestures { onTap() }
            }
            .pointerInput(obj.id) {
                detectDragGestures(
                    onDragEnd = {
                        onDrag(offsetX, offsetY)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        when (obj) {
            is PdfEditObject.TextObject -> {
                TextOverlay(obj, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight, isSelected)
            }
            is PdfEditObject.CoverObject -> {
                CoverOverlay(obj, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight, isSelected)
            }
            is PdfEditObject.SignatureObject -> {
                SignatureOverlay(
                    obj, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight,
                    isSelected, onResize
                )
            }
            is PdfEditObject.CheckmarkObject -> {
                CheckmarkOverlay(obj, viewWidth, viewHeight, pdfPageWidth, pdfPageHeight, isSelected)
            }
        }
    }
}

@Composable
private fun TextOverlay(
    obj: PdfEditObject.TextObject,
    viewWidth: Float,
    viewHeight: Float,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    isSelected: Boolean
) {
    val screenFontSize = PdfEditorEngine.pdfSizeToScreen(obj.fontSize, viewHeight, pdfPageHeight)
    val color = Color(obj.color)

    val selectionModifier = if (isSelected) {
        Modifier.border(1.5.dp, SelectionBorder)
    } else Modifier

    Box(modifier = selectionModifier.padding(2.dp)) {
        Text(
            text = obj.text,
            color = color,
            fontSize = screenFontSize.sp,
            fontWeight = if (obj.isBold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 10,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (isSelected) {
        // Resize handles at corners
        SelectionHandles()
    }
}

@Composable
private fun CoverOverlay(
    obj: PdfEditObject.CoverObject,
    viewWidth: Float,
    viewHeight: Float,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    isSelected: Boolean
) {
    val screenWidth = PdfEditorEngine.pdfSizeToScreen(obj.width, viewWidth, pdfPageWidth)
    val screenHeight = PdfEditorEngine.pdfSizeToScreen(obj.height, viewHeight, pdfPageHeight)
    val color = Color(obj.color)

    val selectionModifier = if (isSelected) {
        Modifier.border(1.5.dp, SelectionBorder)
    } else Modifier

    Box(
        modifier = selectionModifier
            .width(screenWidth.dp)
            .height(screenHeight.dp)
            .background(color)
    )

    if (isSelected) {
        SelectionHandles()
    }
}

@Composable
private fun SignatureOverlay(
    obj: PdfEditObject.SignatureObject,
    viewWidth: Float,
    viewHeight: Float,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    isSelected: Boolean,
    onResize: (Float, Float) -> Unit
) {
    val screenWidth = PdfEditorEngine.pdfSizeToScreen(obj.width, viewWidth, pdfPageWidth)
    val screenHeight = PdfEditorEngine.pdfSizeToScreen(obj.height, viewHeight, pdfPageHeight)
    val density = LocalDensity.current
    val minWidthPx = with(density) { 48.dp.toPx() }
    val aspectRatio = (screenWidth / screenHeight).takeIf { it.isFinite() && it > 0f } ?: 2.5f
    var currentWidthPx by remember(obj.id, obj.width) { mutableFloatStateOf(screenWidth) }
    var currentHeightPx by remember(obj.id, obj.height) { mutableFloatStateOf(screenHeight) }

    val selectionModifier = if (isSelected) {
        Modifier.border(1.5.dp, SelectionBorder)
    } else Modifier

    val bitmap = remember(obj.imagePath) {
        try {
            BitmapFactory.decodeFile(obj.imagePath)?.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = selectionModifier
            .width(with(density) { currentWidthPx.toDp() })
            .height(with(density) { currentHeightPx.toDp() })
    ) {
        bitmap?.let { bmp ->
            Image(
                bitmap = bmp,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 8.dp, y = 8.dp)
                    .size(20.dp)
                    .background(HandleColor, CircleShape)
                    .pointerInput(obj.id) {
                        detectDragGestures(
                            onDragEnd = { onResize(currentWidthPx, currentHeightPx) }
                        ) { change, dragAmount ->
                            change.consume()
                            currentWidthPx = (currentWidthPx + dragAmount.x).coerceAtLeast(minWidthPx)
                            currentHeightPx = currentWidthPx / aspectRatio
                        }
                    }
            )
        }
    }
}

@Composable
private fun CheckmarkOverlay(
    obj: PdfEditObject.CheckmarkObject,
    viewWidth: Float,
    viewHeight: Float,
    pdfPageWidth: Float,
    pdfPageHeight: Float,
    isSelected: Boolean
) {
    val screenSize = PdfEditorEngine.pdfSizeToScreen(obj.size, viewHeight, pdfPageHeight)
    val color = Color(obj.color)

    val selectionModifier = if (isSelected) {
        Modifier.border(1.5.dp, SelectionBorder)
    } else Modifier

    Box(modifier = selectionModifier.padding(2.dp)) {
        Text(
            text = "✓",
            color = color,
            fontSize = screenSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SelectionHandles() {
    // Simple selection indicator - corner dots
    Box(
        modifier = Modifier
            .size(8.dp)
            .offset(x = (-4).dp, y = (-4).dp)
            .background(HandleColor, CircleShape)
    )
}
