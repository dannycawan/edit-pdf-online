/**
 * Purpose: Settings screen for app configuration and information
 * Caller: AppNavigation (Routes.SETTINGS)
 * Dependencies: Material3
 * Main Functions: SettingsScreen - app info, rate, share, privacy
 * Side Effects: Launches external intents for rate/share
 */
package com.editpdf.online.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.editpdf.online.R
import com.editpdf.online.ads.BannerAdType
import com.editpdf.online.ads.BannerAdView
import com.editpdf.online.config.RemoteConfigManager
import com.editpdf.online.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val remoteConfig = remember { RemoteConfigManager() }
    var activeDialog by remember { mutableStateOf<SettingsDialogType?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
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
        bottomBar = {
            BannerAdView(
                adType = BannerAdType.SETTINGS,
                enabled = remoteConfig.isBannerRecentEnabled
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(Background)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App Info Card
            AppInfoCard()

            Spacer(modifier = Modifier.height(24.dp))

            // Banner above "Umum" section (non-intrusive)
            BannerAdView(
                adType = BannerAdType.SETTINGS_GENERAL,
                enabled = remoteConfig.isBannerSettingsGeneralEnabled,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // General Section
            SettingsSectionHeader(stringResource(R.string.settings_general))
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.Language,
                    title = stringResource(R.string.settings_language),
                    subtitle = stringResource(R.string.settings_language_current),
                    onClick = { activeDialog = SettingsDialogType.LANGUAGE }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = Outline.copy(alpha = 0.5f)
                )
                SettingsItem(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(R.string.settings_theme),
                    subtitle = stringResource(R.string.settings_theme_current),
                    onClick = { activeDialog = SettingsDialogType.THEME }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Banner above "Dukungan" section (non-intrusive)
            BannerAdView(
                adType = BannerAdType.SETTINGS_SUPPORT,
                enabled = remoteConfig.isBannerSettingsSupportEnabled,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Support Section
            SettingsSectionHeader(stringResource(R.string.settings_support))
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.Star,
                    title = stringResource(R.string.settings_rate_app),
                    subtitle = stringResource(R.string.settings_rate_subtitle),
                    onClick = {
                        try {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=${context.packageName}")
                            )
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = Outline.copy(alpha = 0.5f)
                )
                SettingsItem(
                    icon = Icons.Outlined.Share,
                    title = stringResource(R.string.settings_share_app),
                    subtitle = stringResource(R.string.settings_share_subtitle),
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_share_subject))
                            putExtra(
                                Intent.EXTRA_TEXT,
                                context.getString(R.string.settings_share_text, context.packageName)
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, null))
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = Outline.copy(alpha = 0.5f)
                )
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.HelpCenter,
                    title = stringResource(R.string.settings_help),
                    subtitle = stringResource(R.string.settings_help_subtitle),
                    onClick = { activeDialog = SettingsDialogType.HELP }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Banner above "Legal" section (non-intrusive)
            BannerAdView(
                adType = BannerAdType.SETTINGS_LEGAL,
                enabled = remoteConfig.isBannerSettingsLegalEnabled,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Legal Section
            SettingsSectionHeader(stringResource(R.string.settings_legal))
            Spacer(modifier = Modifier.height(8.dp))

            SettingsCard {
                SettingsItem(
                    icon = Icons.Outlined.PrivacyTip,
                    title = stringResource(R.string.settings_privacy_policy),
                    subtitle = stringResource(R.string.settings_privacy_subtitle),
                    onClick = { activeDialog = SettingsDialogType.PRIVACY }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 56.dp),
                    color = Outline.copy(alpha = 0.5f)
                )
                SettingsItem(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.settings_terms),
                    subtitle = stringResource(R.string.settings_terms_subtitle),
                    onClick = { activeDialog = SettingsDialogType.TERMS }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Version footer
            Text(
                text = stringResource(R.string.settings_footer_version),
                style = MaterialTheme.typography.labelSmall,
                color = TextHint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    activeDialog?.let { dialogType ->
        SettingsInfoDialog(
            dialogType = dialogType,
            onDismiss = { activeDialog = null }
        )
    }
}

private enum class SettingsDialogType {
    LANGUAGE,
    THEME,
    HELP,
    PRIVACY,
    TERMS
}

@Composable
private fun SettingsInfoDialog(
    dialogType: SettingsDialogType,
    onDismiss: () -> Unit
) {
    val title = when (dialogType) {
        SettingsDialogType.LANGUAGE -> stringResource(R.string.settings_language_dialog_title)
        SettingsDialogType.THEME -> stringResource(R.string.settings_theme_dialog_title)
        SettingsDialogType.HELP -> stringResource(R.string.settings_help_dialog_title)
        SettingsDialogType.PRIVACY -> stringResource(R.string.settings_privacy_dialog_title)
        SettingsDialogType.TERMS -> stringResource(R.string.settings_terms_dialog_title)
    }
    val message = when (dialogType) {
        SettingsDialogType.LANGUAGE -> stringResource(R.string.settings_language_dialog_message)
        SettingsDialogType.THEME -> stringResource(R.string.settings_theme_dialog_message)
        SettingsDialogType.HELP -> stringResource(R.string.settings_help_dialog_message)
        SettingsDialogType.PRIVACY -> stringResource(R.string.settings_privacy_dialog_message)
        SettingsDialogType.TERMS -> stringResource(R.string.settings_terms_dialog_message)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_ok))
            }
        }
    )
}

@Composable
private fun AppInfoCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(PrimaryNavy, SecondaryBlue)
                    )
                )
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OnPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_version_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnPrimary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = SecondaryBlue,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SecondaryBlue,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = OnBackground
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
