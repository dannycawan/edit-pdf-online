/**
 * Purpose: Main PDF editor screen composable
 * Caller: AppNavigation (Routes.EDITOR)
 * Dependencies: EditorViewModel, EditorOverlay, TextInputDialog, ExportDialog
 * Main Functions: Full editor UI with PDF preview, overlays, toolbar, page navigation
 * Side Effects: Renders PDF bitmaps, handles touch gestures
 */
package com.editpdf.online.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editpdf.online.R
import com.editpdf.online.domain.model.EditorTool
import com.editpdf.online.ui.theme.*
import com.editpdf.online.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    uriString: String,
    onNavigateBack: () -> Unit,
    onNavigateToSignature: () -> Unit,
    signatureImagePath: String = "",
    onSignatureImagePathConsumed: () -> Unit = {},
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pageBitmap by viewModel.pageBitmap.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Load PDF on first composition
    LaunchedEffect(uriString) {
        if (uriString.isNotBlank()) {
            viewModel.loadPdf(uriString)
        }
    }

    LaunchedEffect(signatureImagePath) {
        if (signatureImagePath.isNotBlank()) {
            viewModel.placeSignatureAtPendingTap(signatureImagePath)
            onSignatureImagePathConsumed()
        }
    }

    // SAF launcher for Save As
    val saveAsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { viewModel.exportPdfToUri(it) }
    }

    // Canvas size for coordinate mapping
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Cover drawing state
    var coverStart by remember { mutableStateOf<Offset?>(null) }
    var coverEnd by remember { mutableStateOf<Offset?>(null) }

    Scaffold(
        topBar = {
            EditorTopBar(
                fileName = state.fileName,
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                canUndo = state.undoStack.isNotEmpty(),
                canRedo = state.redoStack.isNotEmpty(),
                hasChanges = state.hasChanges,
                onBack = onNavigateBack,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onSave = { viewModel.showExportDialog(true) }
            )
        },
        bottomBar = {
            EditorBottomToolbar(
                activeTool = state.activeTool,
                onSelectTool = viewModel::selectTool,
                onDeleteSelected = viewModel::deleteSelectedObject,
                hasSelectedObject = state.selectedObjectId != null
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SurfaceVariant)
        ) {
            if (state.isLoading) {
                // Loading state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = SecondaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.editor_loading_pdf),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            } else if (state.errorMessage != null && state.totalPages == 0) {
                // Error state
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        state.errorMessage ?: stringResource(R.string.error_generic),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.action_back))
                    }
                }
            } else {
                // Main editor content
                Column(modifier = Modifier.fillMaxSize()) {
                    // Instruction bar
                    state.instructionText?.let { instruction ->
                        InstructionBar(text = instruction)
                    }

                    // PDF Canvas with overlays
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        // PDF page image
                        pageBitmap?.let { bitmap ->
                            PdfCanvasWithOverlays(
                                bitmap = bitmap,
                                state = state,
                                canvasSize = canvasSize,
                                onCanvasSizeChanged = { canvasSize = it },
                                onCanvasTap = { offset ->
                                    if (state.activeTool == EditorTool.SIGN) {
                                        viewModel.storePendingTap(
                                            offset.x,
                                            offset.y,
                                            canvasSize.width.toFloat(),
                                            canvasSize.height.toFloat()
                                        )
                                        onNavigateToSignature()
                                    } else {
                                        viewModel.onCanvasTap(
                                            offset.x, offset.y,
                                            canvasSize.width.toFloat(),
                                            canvasSize.height.toFloat()
                                        )
                                    }
                                },
                                onCoverDraw = { start, end ->
                                    viewModel.addCoverObject(
                                        screenX = minOf(start.x, end.x),
                                        screenY = minOf(start.y, end.y),
                                        screenWidth = kotlin.math.abs(end.x - start.x),
                                        screenHeight = kotlin.math.abs(end.y - start.y),
                                        viewWidth = canvasSize.width.toFloat(),
                                        viewHeight = canvasSize.height.toFloat()
                                    )
                                },
                                onObjectTap = { id -> viewModel.selectObject(id) },
                                onObjectDrag = { id, x, y ->
                                    viewModel.moveObject(
                                        id, x, y,
                                        canvasSize.width.toFloat(),
                                        canvasSize.height.toFloat()
                                    )
                                },
                                viewModel = viewModel
                            )
                        }

                        if (pageBitmap == null && !state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = SecondaryBlue
                            )
                        }
                    }

                    // Page navigation
                    if (state.totalPages > 1) {
                        PageNavigationBar(
                            currentPage = state.currentPage,
                            totalPages = state.totalPages,
                            onPreviousPage = viewModel::previousPage,
                            onNextPage = viewModel::nextPage
                        )
                    }
                }
            }

            // Error snackbar
            if (state.errorMessage != null && state.totalPages > 0) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = ErrorRed,
                    contentColor = OnPrimary,
                    action = {
                        TextButton(onClick = viewModel::dismissError) {
                            Text("OK", color = OnPrimary)
                        }
                    }
                ) {
                    Text(state.errorMessage ?: "")
                }
            }
        }
    }

    // Text Input Dialog
    if (state.showTextInput) {
        TextInputDialog(
            onDismiss = { viewModel.showTextInput(false) },
            onConfirm = { text, fontSize, color, isBold ->
                viewModel.onTextInputConfirm(text, fontSize, color, isBold)
            }
        )
    }

    // Export Dialog
    if (state.showExportDialog) {
        ExportDialog(
            originalFileName = state.fileName,
            isExporting = state.isExporting,
            exportSuccess = state.exportSuccess,
            errorMessage = state.errorMessage,
            onDismiss = { viewModel.showExportDialog(false) },
            onExport = { fileName -> viewModel.exportPdf(fileName) },
            onSaveAs = {
                val defaultName = state.fileName.removeSuffix(".pdf") + "_edited.pdf"
                saveAsLauncher.launch(defaultName)
            },
            onShare = {
                viewModel.getExportedFile()?.let { file ->
                    ShareUtils.sharePdf(context, file)
                }
            }
        )
    }
}

@Composable
private fun PdfCanvasWithOverlays(
    bitmap: Bitmap,
    state: com.editpdf.online.domain.model.EditorState,
    canvasSize: IntSize,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onCanvasTap: (Offset) -> Unit,
    onCoverDraw: (Offset, Offset) -> Unit,
    onObjectTap: (String) -> Unit,
    onObjectDrag: (String, Float, Float) -> Unit,
    viewModel: EditorViewModel
) {
    var coverStart by remember { mutableStateOf<Offset?>(null) }
    var coverCurrent by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .shadow(4.dp, RoundedCornerShape(8.dp))
    ) {
        // PDF page bitmap
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.editor_pdf_page_cd, state.currentPage + 1),
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { onCanvasSizeChanged(it) }
                .pointerInput(state.activeTool) {
                    if (state.activeTool == EditorTool.COVER || state.activeTool == EditorTool.REPLACE) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                coverStart = offset
                                coverCurrent = offset
                            },
                            onDragEnd = {
                                val start = coverStart
                                val end = coverCurrent
                                if (start != null && end != null) {
                                    onCoverDraw(start, end)
                                }
                                coverStart = null
                                coverCurrent = null
                            },
                            onDragCancel = {
                                coverStart = null
                                coverCurrent = null
                            }
                        ) { change, _ ->
                            change.consume()
                            coverCurrent = change.position
                        }
                    }
                }
                .pointerInput(state.activeTool) {
                    if (state.activeTool != EditorTool.COVER) {
                        detectTapGestures { offset ->
                            onCanvasTap(offset)
                        }
                    }
                },
            contentScale = ContentScale.Fit
        )

        // Cover drawing preview
        if (coverStart != null && coverCurrent != null) {
            val start = coverStart!!
            val current = coverCurrent!!
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val left = minOf(start.x, current.x)
                val top = minOf(start.y, current.y)
                val width = kotlin.math.abs(current.x - start.x)
                val height = kotlin.math.abs(current.y - start.y)

                drawRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
                drawRect(
                    color = SelectionBorder,
                    topLeft = Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )
            }
        }

        // Render overlay objects
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val pdfDims = remember(state.currentPage) { mutableStateOf<Pair<Float, Float>?>(null) }

            LaunchedEffect(state.currentPage) {
                viewModel.pdfRendererManager.getPageDimensions(state.currentPage)?.let { (w, h) ->
                    pdfDims.value = Pair(w.toFloat(), h.toFloat())
                }
            }

            pdfDims.value?.let { (pdfW, pdfH) ->
                state.currentPageObjects.forEach { obj ->
                    EditorOverlayObject(
                        obj = obj,
                        isSelected = obj.id == state.selectedObjectId,
                        viewWidth = canvasSize.width.toFloat(),
                        viewHeight = canvasSize.height.toFloat(),
                        pdfPageWidth = pdfW,
                        pdfPageHeight = pdfH,
                        onTap = { onObjectTap(obj.id) },
                        onDrag = { x, y -> onObjectDrag(obj.id, x, y) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    fileName: String,
    currentPage: Int,
    totalPages: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    hasChanges: Boolean,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onSave: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = fileName.ifBlank { "Editor" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnPrimary
                )
                if (totalPages > 0) {
                    Text(
                        text = stringResource(R.string.editor_page_indicator, currentPage + 1, totalPages),
                        style = MaterialTheme.typography.labelSmall,
                        color = OnPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = OnPrimary
                )
            }
        },
        actions = {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = stringResource(R.string.action_undo),
                    tint = if (canUndo) OnPrimary else OnPrimary.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = stringResource(R.string.action_redo),
                    tint = if (canRedo) OnPrimary else OnPrimary.copy(alpha = 0.3f)
                )
            }
            IconButton(onClick = onSave, enabled = hasChanges) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = stringResource(R.string.editor_toolbar_save),
                    tint = if (hasChanges) OnPrimary else OnPrimary.copy(alpha = 0.3f)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = PrimaryNavy
        )
    )
}

@Composable
private fun EditorBottomToolbar(
    activeTool: EditorTool,
    onSelectTool: (EditorTool) -> Unit,
    onDeleteSelected: () -> Unit,
    hasSelectedObject: Boolean
) {
    Surface(
        shadowElevation = 8.dp,
        color = Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolButton(
                icon = Icons.Default.TextFields,
                label = stringResource(R.string.editor_toolbar_text),
                isSelected = activeTool == EditorTool.TEXT,
                onClick = { onSelectTool(EditorTool.TEXT) }
            )
            ToolButton(
                icon = Icons.Default.Rectangle,
                label = stringResource(R.string.editor_toolbar_cover),
                isSelected = activeTool == EditorTool.COVER,
                onClick = { onSelectTool(EditorTool.COVER) }
            )
            ToolButton(
                icon = Icons.Default.FindReplace,
                label = stringResource(R.string.editor_toolbar_replace),
                isSelected = activeTool == EditorTool.REPLACE,
                onClick = { onSelectTool(EditorTool.REPLACE) }
            )
            ToolButton(
                icon = Icons.Default.Draw,
                label = stringResource(R.string.editor_toolbar_sign),
                isSelected = activeTool == EditorTool.SIGN,
                onClick = { onSelectTool(EditorTool.SIGN) }
            )
            ToolButton(
                icon = Icons.Default.CheckBox,
                label = stringResource(R.string.editor_toolbar_check),
                isSelected = activeTool == EditorTool.CHECKMARK,
                onClick = { onSelectTool(EditorTool.CHECKMARK) }
            )

            if (hasSelectedObject) {
                ToolButton(
                    icon = Icons.Default.Delete,
                    label = stringResource(R.string.action_delete),
                    isSelected = false,
                    onClick = onDeleteSelected,
                    tint = ErrorRed
                )
            }
        }
    }
}

@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tint: Color = if (isSelected) SecondaryBlue else TextSecondary
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) CardBlue else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun InstructionBar(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CardBlue
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = PrimaryNavy,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PageNavigationBar(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Surface(
        color = Surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousPage,
                enabled = currentPage > 0,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (currentPage > 0) CardBlue else SurfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = stringResource(R.string.editor_previous_page),
                    tint = if (currentPage > 0) PrimaryNavy else TextHint
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = "${currentPage + 1} / $totalPages",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground
            )

            Spacer(modifier = Modifier.width(24.dp))

            IconButton(
                onClick = onNextPage,
                enabled = currentPage < totalPages - 1,
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (currentPage < totalPages - 1) CardBlue else SurfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = stringResource(R.string.editor_next_page),
                    tint = if (currentPage < totalPages - 1) PrimaryNavy else TextHint
                )
            }
        }
    }
}
