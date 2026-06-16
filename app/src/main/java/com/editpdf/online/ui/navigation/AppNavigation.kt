/**
 * Purpose: Navigation graph for Edit PDF Online - Text Editor
 * Caller: MainActivity
 * Dependencies: NavHost, Compose Navigation, all screen composables
 * Main Functions: AppNavigation - defines routes and navigation flow
 * Side Effects: Controls screen transitions and back stack
 */
package com.editpdf.online.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.editpdf.online.ui.editor.EditorScreen
import com.editpdf.online.ui.files.RecentFilesScreen
import com.editpdf.online.ui.home.HomeScreen
import com.editpdf.online.ui.home.HomeViewModel
import com.editpdf.online.ui.settings.SettingsScreen
import com.editpdf.online.ui.signature.SignatureScreen
import com.editpdf.online.ui.tools.ToolsScreen

/**
 * Navigation route constants.
 * Each route string maps to exactly one screen composable.
 */
object Routes {
    const val HOME = "home"
    const val EDITOR = "editor?uri={uri}"
    const val SIGNATURE = "signature"
    const val RECENT_FILES = "recent_files"
    const val TOOLS = "tools"
    const val SETTINGS = "settings"
    const val ONBOARDING = "onboarding"

    fun editorRoute(uri: String): String = "editor?uri=${Uri.encode(uri)}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Shared HomeViewModel for Home and RecentFiles screens
    val homeViewModel: HomeViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            // SAF file picker launcher
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.let {
                    persistReadPermission(context, it)
                    navController.navigate(Routes.editorRoute(it.toString()))
                }
            }

            HomeScreen(
                onOpenPdf = {
                    filePickerLauncher.launch(arrayOf("application/pdf"))
                },
                onNavigateToEditor = {
                    filePickerLauncher.launch(arrayOf("application/pdf"))
                },
                onNavigateToTools = {
                    navController.navigate(Routes.TOOLS)
                },
                onNavigateToRecent = {
                    navController.navigate(Routes.RECENT_FILES)
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                homeViewModel = homeViewModel
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(
                navArgument("uri") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            val signaturePath by backStackEntry.savedStateHandle
                .getStateFlow("signature_path", "")
                .collectAsState()
            EditorScreen(
                // Navigation already decodes query arguments once. Decoding again can corrupt
                // SAF document IDs that intentionally contain escaped slashes, e.g. %2F.
                uriString = uri,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSignature = { navController.navigate(Routes.SIGNATURE) },
                signatureImagePath = signaturePath,
                onSignatureImagePathConsumed = {
                    backStackEntry.savedStateHandle["signature_path"] = ""
                }
            )
        }

        composable(Routes.SIGNATURE) {
            SignatureScreen(
                onNavigateBack = { navController.popBackStack() },
                onSignatureSaved = { path ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("signature_path", path)
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.RECENT_FILES) {
            val recentFilePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.let {
                    persistReadPermission(context, it)
                    navController.navigate(Routes.editorRoute(it.toString()))
                }
            }

            RecentFilesScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenFile = {
                    recentFilePickerLauncher.launch(arrayOf("application/pdf"))
                },
                viewModel = homeViewModel
            )
        }

        composable(Routes.TOOLS) {
            // SAF file picker for tools that need a PDF file
            val toolFilePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                uri?.let {
                    persistReadPermission(context, it)
                    navController.navigate(Routes.editorRoute(it.toString()))
                }
            }

            ToolsScreen(
                onNavigateBack = { navController.popBackStack() },
                onToolClick = { toolId ->
                    when (toolId) {
                        "edit_text", "add_text", "sign", "fill_form" -> {
                            // These tools open the editor with a file picker
                            toolFilePickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    }
                }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ONBOARDING) {
            // OnboardingScreen - optional, low priority
        }
    }
}

private fun persistReadPermission(context: Context, uri: Uri) {
    // Try to take persistable read permission. If the provider does not support
    // persistable permissions (e.g. Google Drive, some OEM providers), this will
    // throw but the URI still has temporary access that is valid for the current session.
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: SecurityException) {
        // Provider only granted temporary access — still fine for this session.
    } catch (_: IllegalArgumentException) {
        // Provider does not support persistable permissions.
    } catch (_: Exception) {
        // Keep editor flow working even if persist permission fails.
    }
}
