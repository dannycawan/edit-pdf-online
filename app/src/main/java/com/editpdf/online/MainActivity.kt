/**
 * Purpose: Single Activity host for Edit PDF Online - Text Editor
 * Caller: Android OS (launcher intent)
 * Dependencies: EditPdfOnlineTheme, AppNavigation
 * Main Functions: onCreate - sets up Compose content with theme and navigation
 * Side Effects: Sets status bar appearance, hosts all Compose screens
 */
package com.editpdf.online

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.editpdf.online.ui.navigation.AppNavigation
import com.editpdf.online.ui.theme.EditPdfOnlineTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EditPdfOnlineTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
