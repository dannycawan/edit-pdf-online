/**
 * Purpose: Full-screen list of all recent PDF files
 * Caller: AppNavigation (Routes.RECENT_FILES)
 * Dependencies: Material3, RecentFileRepository, HomeViewModel
 * Main Functions: RecentFilesScreen - shows all recent files with search and clear
 * Side Effects: Database reads/writes
 */
package com.editpdf.online.ui.files

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.editpdf.online.R
import com.editpdf.online.ads.BannerAdView
import com.editpdf.online.data.model.RecentFile
import com.editpdf.online.ui.home.HomeViewModel
import com.editpdf.online.ui.theme.*
import com.editpdf.online.utils.FileUtils
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFilesScreen(
    onNavigateBack: () -> Unit,
    onOpenFile: (String) -> Unit,
    viewModel: HomeViewModel
) {
    val recentFiles by viewModel.recentFiles.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.recent_files),
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
                actions = {
                    if (recentFiles.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = stringResource(R.string.files_clear_all_cd),
                                tint = OnPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryNavy
                )
            )
        },
        bottomBar = { BannerAdView() }
    ) { paddingValues ->
        if (recentFiles.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        tint = TextHint,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.no_recent_files),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentFiles, key = { it.id }) { file ->
                    RecentFileCard(
                        file = file,
                        onClick = { onOpenFile(file.fileUri) },
                        onDelete = { viewModel.deleteRecentFile(file.id) }
                    )
                }
            }
        }
    }

    // Clear all confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.files_clear_all_title)) },
            text = { Text(stringResource(R.string.files_clear_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllRecentFiles()
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.files_clear_all_confirm), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun RecentFileCard(
    file: RecentFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PDF icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = OnBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = FileUtils.formatFileSize(file.fileSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    if (file.pageCount > 0) {
                        Text("•", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            stringResource(R.string.editor_pages_count, file.pageCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    if (file.isExported) {
                        Text("•", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            stringResource(R.string.files_exported),
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Medium
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
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
