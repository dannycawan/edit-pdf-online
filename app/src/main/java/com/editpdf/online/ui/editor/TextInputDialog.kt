/**
 * Purpose: Dialog composable for text input when adding text to PDF
 * Caller: EditorScreen
 * Dependencies: Material3
 * Main Functions: TextInputDialog - text entry with font/color options
 * Side Effects: None (pure UI)
 */
package com.editpdf.online.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editpdf.online.R
import com.editpdf.online.ui.theme.*

/**
 * Dialog for entering text to add to the PDF page.
 * Includes text input, font size slider, bold toggle, and color picker.
 */
@Composable
fun TextInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (text: String, fontSize: Float, color: Int, isBold: Boolean) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(16f) }
    var isBold by remember { mutableStateOf(false) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val colorOptions = listOf(
        0xFF000000.toInt() to "Black",
        0xFF1A237E.toInt() to "Navy",
        0xFF1976D2.toInt() to "Blue",
        0xFFD32F2F.toInt() to "Red",
        0xFF2E7D32.toInt() to "Green",
        0xFF6A1B9A.toInt() to "Purple",
        0xFFF57F17.toInt() to "Orange",
        0xFF455A64.toInt() to "Gray"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Surface,
        title = {
            Text(
                text = stringResource(R.string.tool_add_text),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = OnBackground
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Text Input
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.editor_text_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryBlue,
                        unfocusedBorderColor = Outline
                    )
                )

                // Font Size
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.editor_font_size),
                            style = MaterialTheme.typography.labelLarge,
                            color = OnBackground
                        )
                        Text(
                            text = "${fontSize.toInt()}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryBlue
                        )
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 8f..48f,
                        steps = 39,
                        colors = SliderDefaults.colors(
                            thumbColor = SecondaryBlue,
                            activeTrackColor = SecondaryBlue,
                            inactiveTrackColor = Outline
                        )
                    )
                }

                // Bold Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.editor_bold),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = OnBackground
                    )
                    Switch(
                        checked = isBold,
                        onCheckedChange = { isBold = it },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = SecondaryBlue,
                            checkedThumbColor = OnPrimary
                        )
                    )
                }

                // Color Picker
                Column {
                    Text(
                        text = stringResource(R.string.editor_text_color),
                        style = MaterialTheme.typography.labelLarge,
                        color = OnBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colorOptions.forEachIndexed { index, (color, _) ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(color))
                                    .then(
                                        if (index == selectedColorIndex) {
                                            Modifier.border(3.dp, SecondaryBlue, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Outline, CircleShape)
                                        }
                                    )
                                    .clickable { selectedColorIndex = index }
                            )
                        }
                    }
                }

                // Preview
                if (text.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariant)
                    ) {
                        Text(
                            text = text,
                            modifier = Modifier.padding(12.dp),
                            fontSize = fontSize.sp,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            color = Color(colorOptions[selectedColorIndex].first),
                            maxLines = 3
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onConfirm(text, fontSize, colorOptions[selectedColorIndex].first, isBold)
                    }
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.action_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.action_cancel),
                    color = TextSecondary
                )
            }
        }
    )
}
