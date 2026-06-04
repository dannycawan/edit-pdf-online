/**
 * Purpose: Material3 Compose theme for Edit PDF Online - Text Editor
 * Caller: MainActivity (setContent), all Composable screens
 * Dependencies: Color.kt, Type.kt, Material3
 * Main Functions: EditPdfOnlineTheme composable
 * Side Effects: Applies color scheme and typography to the composition tree
 */
package com.editpdf.online.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavy,
    onPrimary = OnPrimary,
    primaryContainer = CardBlue,
    onPrimaryContainer = PrimaryNavyDark,
    secondary = SecondaryBlue,
    onSecondary = OnSecondary,
    secondaryContainer = CardBlue,
    onSecondaryContainer = SecondaryBlueDark,
    tertiary = TertiaryTeal,
    onTertiary = OnTertiary,
    tertiaryContainer = CardTeal,
    onTertiaryContainer = TertiaryTeal,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorRed,
    outline = Outline,
    outlineVariant = OutlineVariant
)

@Composable
fun EditPdfOnlineTheme(
    content: @Composable () -> Unit
) {
    // V1: Light theme only. Dark mode planned for V2.
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = AppTypography,
        content = content
    )
}
