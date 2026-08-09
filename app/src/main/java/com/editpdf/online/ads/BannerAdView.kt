/**
 * Purpose: AdMob banner ad composable wrapper for Jetpack Compose
 * Caller: HomeScreen, ToolsScreen, RecentFilesScreen, SettingsScreen
 * Dependencies: Google Mobile Ads SDK, AndroidView
 * Main Functions: BannerAdView composable (adaptive banner for optimal revenue)
 * Side Effects: Loads and displays AdMob banner ads
 */
package com.editpdf.online.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Identifies where a banner ad is placed, for analytics and remote-config control.
 */
enum class BannerAdType {
    HOME,
    HOME_MAIN_TOOLS,
    HOME_PDF_TOOLS,
    HOME_INLINE,
    TOOLS,
    TOOLS_MAIN_TOOLS_BOTTOM,
    TOOLS_PDF_TOOLS,
    TOOLS_INLINE,
    RECENT,
    SETTINGS,
    SETTINGS_GENERAL,
    SETTINGS_SUPPORT,
    SETTINGS_LEGAL
}

/**
 * Composable that displays an AdMob adaptive banner ad.
 * Uses adaptive banner size for higher revenue (2-3x vs fixed banner).
 * Automatically adjusts to screen width for optimal fill rate.
 *
 * @param adType Where the banner is placed (for analytics / remote-config control)
 * @param enabled Whether the banner should be shown (controlled by RemoteConfig)
 * @param adUnitId The AdMob ad unit ID (use test ID during development)
 * @param modifier Modifier for the composable
 */
@Composable
fun BannerAdView(
    adType: BannerAdType = BannerAdType.HOME,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_AD_UNIT_ID
) {
    // If remote config disables this banner slot, render nothing.
    if (!enabled) return

    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp

    // Adaptive banner height is typically 50-90dp depending on screen
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                // Use adaptive banner for optimal revenue
                val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                    ctx,
                    screenWidthDp
                )
                setAdSize(adSize)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// Test ad unit IDs - replace with real IDs for production
const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"     // Adaptive banner test ID
const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
