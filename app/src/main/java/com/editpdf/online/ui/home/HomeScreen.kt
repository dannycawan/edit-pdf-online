/**
 * Purpose: Home screen for Edit PDF Online - Text Editor
 * Caller: AppNavigation (Routes.HOME)
 * Dependencies: Material3, Compose, string resources, HomeViewModel
 * Main Functions: HomeScreen composable - main app entry point
 * Side Effects: Observes recent files from Room database
 */
package com.editpdf.online.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.editpdf.online.R
import com.editpdf.online.data.model.RecentFile
import com.editpdf.online.ui.theme.*
import com.editpdf.online.utils.FileUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenPdf: () -> Unit,
    onOpenPdfWithTool: (String) -> Unit = {},
    onNavigateToEditor: (String) -> Unit,
    onNavigateToTools: () -> Unit,
    onNavigateToRecent: () -> Unit,
    onNavigateToSettings: () -> Unit,
    homeViewModel: HomeViewModel = viewModel()
) {
    val recentFiles by homeViewModel.recentFiles.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            BottomNavBar(
                onHomeClick = { /* Already on home */ },
                onFilesClick = onNavigateToRecent,
                onToolsClick = onNavigateToTools,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Hero Section
            HeroSection(onOpenPdf = onOpenPdf)

            Spacer(modifier = Modifier.height(24.dp))

            // Main Tools
            SectionHeader(title = stringResource(R.string.main_tools))
            Spacer(modifier = Modifier.height(12.dp))
            MainToolsGrid(
                onEditTextClick = { onOpenPdfWithTool("COVER") },
                onSignClick = { onOpenPdfWithTool("SIGN") },
                onFillFormClick = { onOpenPdfWithTool("CHECKMARK") },
                onAddTextClick = { onOpenPdfWithTool("TEXT") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PDF Tools
            SectionHeader(title = stringResource(R.string.pdf_tools))
            Spacer(modifier = Modifier.height(12.dp))
            PdfToolsGrid(onToolClick = { onNavigateToTools() })

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Files
            SectionHeader(
                title = stringResource(R.string.recent_files),
                actionText = if (recentFiles.isNotEmpty()) stringResource(R.string.view_all) else null,
                onActionClick = onNavigateToRecent
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (recentFiles.isEmpty()) {
                RecentFilesEmptyState()
            } else {
                RecentFilesList(
                    files = recentFiles,
                    onFileClick = { file -> onNavigateToEditor(file.fileUri) },
                    onDeleteFile = { file -> homeViewModel.deleteRecentFile(file.id) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HeroSection(onOpenPdf: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(PrimaryNavy, SecondaryBlue)
                )
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineLarge,
                color = OnPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = OnPrimary.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onOpenPdf,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnPrimary,
                    contentColor = PrimaryNavy
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    Icons.Default.FileOpen,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.open_pdf),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = OnBackground
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = SecondaryBlue
                )
            }
        }
    }
}

@Composable
private fun MainToolsGrid(
    onEditTextClick: () -> Unit,
    onSignClick: () -> Unit,
    onFillFormClick: () -> Unit,
    onAddTextClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_edit_pdf_text),
                icon = Icons.Default.Edit,
                backgroundColor = CardBlue,
                iconTint = PrimaryNavy,
                onClick = onEditTextClick
            )
            ToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_sign_pdf),
                icon = Icons.Default.Draw,
                backgroundColor = CardGreen,
                iconTint = SuccessGreen,
                onClick = onSignClick
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_fill_form),
                icon = Icons.Default.Checklist,
                backgroundColor = CardOrange,
                iconTint = WarningYellow,
                onClick = onFillFormClick
            )
            ToolCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.tool_add_text),
                icon = Icons.Default.TextFields,
                backgroundColor = CardPurple,
                iconTint = PrimaryNavy,
                onClick = onAddTextClick
            )
        }
    }
}

@Composable
private fun ToolCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    iconTint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground
            )
        }
    }
}

@Composable
private fun PdfToolsGrid(onToolClick: (String) -> Unit) {
    val tools = listOf(
        Triple(stringResource(R.string.tool_merge_pdf), Icons.AutoMirrored.Filled.MergeType, "merge"),
        Triple(stringResource(R.string.tool_split_pdf), Icons.AutoMirrored.Filled.CallSplit, "split"),
        Triple(stringResource(R.string.tool_rotate_pdf), Icons.AutoMirrored.Filled.RotateRight, "rotate"),
        Triple(stringResource(R.string.tool_delete_pages), Icons.Default.DeleteForever, "delete_pages"),
        Triple(stringResource(R.string.tool_image_to_pdf), Icons.Default.Image, "image_to_pdf"),
        Triple(stringResource(R.string.tool_pdf_to_image), Icons.Default.PhotoLibrary, "pdf_to_image")
    )

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        tools.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { (title, icon, id) ->
                    SmallToolCard(
                        modifier = Modifier.weight(1f),
                        title = title,
                        icon = icon,
                        onClick = { onToolClick(id) }
                    )
                }
                // Fill remaining space if row is incomplete
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SmallToolCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SecondaryBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                color = OnSurface
            )
        }
    }
}

@Composable
private fun RecentFilesEmptyState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.no_recent_files),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentFilesList(
    files: List<RecentFile>,
    onFileClick: (RecentFile) -> Unit,
    onDeleteFile: (RecentFile) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        files.forEach { file ->
            RecentFileItem(
                file = file,
                onClick = { onFileClick(file) },
                onDelete = { onDeleteFile(file) }
            )
        }
    }
}

@Composable
private fun RecentFileItem(
    file: RecentFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = FileUtils.formatFileSize(file.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (file.pageCount > 0) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = stringResource(R.string.editor_pages_count, file.pageCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = dateFormat.format(Date(file.lastOpenedTime)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextHint
                )
            }

            // Delete button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    onHomeClick: () -> Unit,
    onFilesClick: () -> Unit,
    onToolsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    NavigationBar(
        containerColor = Surface,
        tonalElevation = 2.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = onHomeClick,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_home)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryNavy,
                selectedTextColor = PrimaryNavy,
                indicatorColor = CardBlue
            )
        )
        NavigationBarItem(
            selected = false,
            onClick = onFilesClick,
            icon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_files)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onToolsClick,
            icon = { Icon(Icons.Outlined.Build, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_tools)) }
        )
        NavigationBarItem(
            selected = false,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_settings)) }
        )
    }
}
