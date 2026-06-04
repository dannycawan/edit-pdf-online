/**
 * Purpose: Dialog composable for exporting the edited PDF
 * Caller: EditorScreen
 * Dependencies: Material3
 * Main Functions: ExportDialog - filename input + export options
 * Side Effects: None (pure UI)
 */
package com.editpdf.online.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.editpdf.online.R
import com.editpdf.online.ui.theme.*

/**
 * Dialog for exporting the edited PDF.
 * Shows filename input, export options, and progress/success states.
 */
@Composable
fun ExportDialog(
    originalFileName: String,
    isExporting: Boolean,
    exportSuccess: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onExport: (String) -> Unit,
    onSaveAs: () -> Unit,
    onShare: () -> Unit
) {
    var fileName by remember {
        mutableStateOf(
            originalFileName.removeSuffix(".pdf").removeSuffix(".PDF") + "_edited.pdf"
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isExporting) onDismiss() },
        shape = RoundedCornerShape(20.dp),
        containerColor = Surface,
        title = {
            Text(
                text = if (exportSuccess) stringResource(R.string.success_pdf_exported)
                else stringResource(R.string.export_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (exportSuccess) SuccessGreen else OnBackground
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (exportSuccess) {
                    // Success state
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "PDF has been exported successfully!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    // Share button
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_share_pdf))
                    }
                } else if (isExporting) {
                    // Exporting state
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = SecondaryBlue
                    )
                    Text(
                        text = stringResource(R.string.export_saving),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    // Input state
                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text(stringResource(R.string.export_filename_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryBlue,
                            unfocusedBorderColor = Outline
                        )
                    )

                    // Error message
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }

                    // Export to cache button
                    Button(
                        onClick = {
                            val finalName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
                            onExport(finalName)
                        },
                        enabled = fileName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_export_pdf))
                    }

                    // Save As button (SAF)
                    OutlinedButton(
                        onClick = onSaveAs,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save As...")
                    }
                }
            }
        },
        confirmButton = {
            if (exportSuccess || !isExporting) {
                TextButton(onClick = onDismiss) {
                    Text(
                        if (exportSuccess) stringResource(R.string.action_done)
                        else stringResource(R.string.action_cancel),
                        color = if (exportSuccess) SecondaryBlue else TextSecondary
                    )
                }
            }
        },
        dismissButton = {}
    )
}
