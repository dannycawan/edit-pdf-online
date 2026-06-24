/**
 * Purpose: Tools screen showing all available PDF tools
 * Caller: AppNavigation (Routes.TOOLS)
 * Dependencies: Material3
 * Main Functions: ToolsScreen - grid of PDF manipulation tools
 * Side Effects: None (pure UI, navigates to tool sub-screens)
 */
package com.editpdf.online.ui.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editpdf.online.R
import com.editpdf.online.ads.BannerAdView
import com.editpdf.online.ui.theme.*
import com.editpdf.online.utils.ShareUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateBack: () -> Unit,
    onToolClick: (String) -> Unit,
    viewModel: ToolsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingTool by remember { mutableStateOf<String?>(null) }

    val mergePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { persistReadPermission(context, it) }
        viewModel.mergePdfs(uris)
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        uris.forEach { persistReadPermission(context, it) }
        viewModel.imageToPdf(uris)
    }

    val singlePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        val tool = pendingTool
        pendingTool = null
        if (uri != null && tool != null) {
            persistReadPermission(context, uri)
            if (tool == "pdf_to_image") {
                viewModel.pdfToImages(uri)
            } else {
                viewModel.prepareSinglePdfTool(tool, uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.nav_tools),
                        fontWeight = FontWeight.SemiBold,
                        color = OnPrimary
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryNavy
                )
            )
        },
        bottomBar = { BannerAdView() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Background)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Edit Tools Section
            ToolSectionHeader(title = stringResource(R.string.main_tools))
            Spacer(modifier = Modifier.height(12.dp))
            EditToolsRow(onToolClick = onToolClick)

            Spacer(modifier = Modifier.height(24.dp))

            // PDF Tools Section
            ToolSectionHeader(title = stringResource(R.string.pdf_tools))
            Spacer(modifier = Modifier.height(12.dp))
            PdfToolsGrid(
                onToolClick = { toolId ->
                    when (toolId) {
                        "merge" -> mergePicker.launch(arrayOf("application/pdf"))
                        "split", "rotate", "delete_pages" -> {
                            pendingTool = toolId
                            singlePdfPicker.launch(arrayOf("application/pdf"))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Convert Tools Section
            ToolSectionHeader(title = stringResource(R.string.tools_convert))
            Spacer(modifier = Modifier.height(12.dp))
            ConvertToolsRow(
                onToolClick = { toolId ->
                    when (toolId) {
                        "image_to_pdf" -> imagePicker.launch(arrayOf("image/*"))
                        "pdf_to_image" -> {
                            pendingTool = toolId
                            singlePdfPicker.launch(arrayOf("application/pdf"))
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (state.isWorking) {
        WorkingDialog()
    }

    if (state.selectedPdfUri != null && state.activeTool == "split") {
        SplitToolDialog(
            onDismiss = viewModel::clearActiveDialog,
            onConfirm = viewModel::splitPdf
        )
    }

    if (state.selectedPdfUri != null && state.activeTool == "rotate") {
        RotateToolDialog(
            onDismiss = viewModel::clearActiveDialog,
            onConfirm = viewModel::rotatePdf
        )
    }

    if (state.selectedPdfUri != null && state.activeTool == "delete_pages") {
        DeletePagesDialog(
            onDismiss = viewModel::clearActiveDialog,
            onConfirm = viewModel::deletePages
        )
    }

    state.resultMessage?.let { message ->
        ResultDialog(
            message = message,
            onDismiss = viewModel::dismissMessages,
            onShare = {
                val mimeType = if (state.resultFiles.any { it.extension.equals("png", true) }) {
                    "image/png"
                } else {
                    "application/pdf"
                }
                ShareUtils.shareFiles(context, state.resultFiles, mimeType)
            }
        )
    }

    state.errorMessage?.let { message ->
        ErrorDialog(
            message = message,
            onDismiss = viewModel::dismissMessages
        )
    }
}

@Composable
private fun ToolSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = OnBackground,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun EditToolsRow(onToolClick: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LargeToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_edit_pdf_text),
                subtitle = stringResource(R.string.tools_subtitle_edit),
                icon = Icons.Default.Edit,
                gradientColors = listOf(PrimaryNavy, SecondaryBlue),
                onClick = { onToolClick("edit_text") }
            )
            LargeToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_sign_pdf),
                subtitle = stringResource(R.string.tools_subtitle_sign),
                icon = Icons.Default.Draw,
                gradientColors = listOf(SuccessGreen, TertiaryTeal),
                onClick = { onToolClick("sign") }
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LargeToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_fill_form),
                subtitle = stringResource(R.string.tools_subtitle_fill),
                icon = Icons.Default.Checklist,
                gradientColors = listOf(WarningYellow, Color(0xFFFF8F00)),
                onClick = { onToolClick("fill_form") }
            )
            LargeToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_add_text),
                subtitle = stringResource(R.string.tools_subtitle_add_text),
                icon = Icons.Default.TextFields,
                gradientColors = listOf(Color(0xFF6A1B9A), Color(0xFF9C27B0)),
                onClick = { onToolClick("add_text") }
            )
        }
    }
}

@Composable
private fun PdfToolsGrid(onToolClick: (String) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CompactToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_merge_pdf),
                icon = Icons.AutoMirrored.Filled.MergeType,
                iconTint = SecondaryBlue,
                bgColor = CardBlue,
                onClick = { onToolClick("merge") }
            )
            CompactToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_split_pdf),
                icon = Icons.AutoMirrored.Filled.CallSplit,
                iconTint = TertiaryTeal,
                bgColor = CardTeal,
                onClick = { onToolClick("split") }
            )
            CompactToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_rotate_pdf),
                icon = Icons.AutoMirrored.Filled.RotateRight,
                iconTint = WarningYellow,
                bgColor = CardOrange,
                onClick = { onToolClick("rotate") }
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CompactToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_delete_pages),
                icon = Icons.Default.DeleteForever,
                iconTint = ErrorRed,
                bgColor = CardRed,
                onClick = { onToolClick("delete_pages") }
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ConvertToolsRow(onToolClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CompactToolCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.tool_image_to_pdf),
            icon = Icons.Default.Image,
            iconTint = SecondaryBlue,
            bgColor = CardBlue,
            onClick = { onToolClick("image_to_pdf") }
        )
        CompactToolCard(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.tool_pdf_to_image),
            icon = Icons.Default.PhotoLibrary,
            iconTint = TertiaryTeal,
            bgColor = CardTeal,
            onClick = { onToolClick("pdf_to_image") }
        )
        // Spacer for alignment
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LargeToolCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(gradientColors)
                )
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = OnPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimary
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = OnPrimary.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactToolCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    iconTint: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
        }
    }
}

@Composable
private fun WorkingDialog() {
    AlertDialog(
        onDismissRequest = {},
        confirmButton = {},
        title = { Text(stringResource(R.string.tools_processing_title)) },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = SecondaryBlue)
                Text(stringResource(R.string.tools_processing_message))
            }
        }
    )
}

@Composable
private fun SplitToolDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var rangeText by remember { mutableStateOf("1") }
    ToolInputDialog(
        title = stringResource(R.string.tool_split_pdf),
        label = stringResource(R.string.tools_page_ranges),
        value = rangeText,
        onValueChange = { rangeText = it },
        supportingText = stringResource(R.string.tools_page_ranges_hint),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(rangeText) }
    )
}

@Composable
private fun RotateToolDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var pageText by remember { mutableStateOf("1") }
    var degrees by remember { mutableStateOf(90) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tool_rotate_pdf)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = pageText,
                    onValueChange = { pageText = it },
                    label = { Text(stringResource(R.string.tools_pages)) },
                    supportingText = { Text(stringResource(R.string.tools_pages_hint)) },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(90, 180, 270).forEach { option ->
                        FilterChip(
                            selected = degrees == option,
                            onClick = { degrees = option },
                            label = { Text("$option deg") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(degrees, pageText) }) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun DeletePagesDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pageText by remember { mutableStateOf("1") }
    ToolInputDialog(
        title = stringResource(R.string.tool_delete_pages),
        label = stringResource(R.string.tools_pages),
        value = pageText,
        onValueChange = { pageText = it },
        supportingText = stringResource(R.string.tools_pages_hint),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(pageText) }
    )
}

@Composable
private fun ToolInputDialog(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                supportingText = { Text(supportingText) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ResultDialog(
    message: String,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tools_result_title)) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onShare) {
                Text(stringResource(R.string.action_share_pdf))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        }
    )
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.error_generic)) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        }
    )
}

private fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Some providers grant only temporary access, which is enough for immediate processing.
    }
}
