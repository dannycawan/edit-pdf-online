/**
 * Purpose: Signature drawing screen for creating transparent PNG signatures
 * Caller: AppNavigation (Routes.SIGNATURE)
 * Dependencies: PdfSignatureManager, Compose Canvas
 * Main Functions: Draw, clear, save signature
 * Side Effects: Saves PNG file to app internal storage
 */
package com.editpdf.online.ui.signature

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.editpdf.online.R
import com.editpdf.online.analytics.AnalyticsTracker
import com.editpdf.online.pdf.PdfSignatureManager
import com.editpdf.online.ui.theme.Background
import com.editpdf.online.ui.theme.ErrorRed
import com.editpdf.online.ui.theme.OnPrimary
import com.editpdf.online.ui.theme.Outline
import com.editpdf.online.ui.theme.PrimaryNavy
import com.editpdf.online.ui.theme.SecondaryBlue
import com.editpdf.online.ui.theme.Surface
import com.editpdf.online.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignatureScreen(
    onNavigateBack: () -> Unit,
    onSignatureSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val signatureManager = remember { PdfSignatureManager(context) }
    val analyticsTracker = remember { AnalyticsTracker(context) }

    var strokes by remember { mutableStateOf<List<List<Offset>>>(emptyList()) }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.signature_title),
                        color = OnPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = OnPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryNavy)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.signature_instruction),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                SignaturePad(
                    strokes = strokes,
                    currentStroke = currentStroke,
                    onCanvasSizeChanged = { canvasSize = it },
                    onStrokeStarted = { currentStroke = listOf(it) },
                    onStrokeMoved = { currentStroke = currentStroke + it },
                    onStrokeFinished = {
                        if (currentStroke.size > 1) {
                            strokes = strokes + listOf(currentStroke)
                        }
                        currentStroke = emptyList()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            strokes = emptyList()
                            currentStroke = emptyList()
                        },
                        enabled = !isSaving && (strokes.isNotEmpty() || currentStroke.isNotEmpty()),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = stringResource(R.string.action_clear),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.signature_clear))
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                errorMessage = null
                                runCatching {
                                    val bitmap = createSignatureBitmap(strokes, canvasSize)
                                    signatureManager.saveSignature(bitmap)
                                }.fold(
                                    onSuccess = { path ->
                                        analyticsTracker.trackSignatureClicked()
                                        onSignatureSaved(path)
                                    },
                                    onFailure = {
                                        errorMessage = context.getString(R.string.error_save_signature)
                                    }
                                )
                                isSaving = false
                            }
                        },
                        enabled = !isSaving && strokes.isNotEmpty() && canvasSize.width > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = stringResource(R.string.action_save_signature),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(stringResource(R.string.signature_save))
                    }
                }
            }

            errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = ErrorRed,
                    contentColor = OnPrimary
                ) {
                    Text(message)
                }
            }
        }
    }
}

@Composable
private fun SignaturePad(
    strokes: List<List<Offset>>,
    currentStroke: List<Offset>,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onStrokeStarted: (Offset) -> Unit,
    onStrokeMoved: (Offset) -> Unit,
    onStrokeFinished: () -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(3f)
            .background(Surface, RoundedCornerShape(8.dp))
            .border(1.dp, Outline, RoundedCornerShape(8.dp))
            .onSizeChanged(onCanvasSizeChanged)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = onStrokeStarted,
                    onDragEnd = onStrokeFinished,
                    onDragCancel = onStrokeFinished
                ) { change, _ ->
                    change.consume()
                    onStrokeMoved(change.position)
                }
            }
    ) {
        (strokes + listOf(currentStroke)).forEach { stroke ->
            if (stroke.size > 1) {
                val path = Path().apply {
                    moveTo(stroke.first().x, stroke.first().y)
                    stroke.drop(1).forEach { point -> lineTo(point.x, point.y) }
                }
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 6f)
                )
            }
        }
    }
}

private fun createSignatureBitmap(
    strokes: List<List<Offset>>,
    canvasSize: IntSize
): Bitmap {
    val bitmap = Bitmap.createBitmap(canvasSize.width, canvasSize.height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.TRANSPARENT)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    strokes.forEach { stroke ->
        stroke.zipWithNext { start, end ->
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }

    return bitmap
}
