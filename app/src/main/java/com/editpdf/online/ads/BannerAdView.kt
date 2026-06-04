/**
 * Purpose: AdMob banner ad composable wrapper for Jetpack Compose
 * Caller: HomeScreen, ToolsScreen, RecentFilesScreen
 * Dependencies: Google Mobile Ads SDK, AndroidView
 * Main Functions: BannerAdView composable
 * Side Effects: Loads and displays AdMob banner ads
 */
package com.editpdf.online.ads

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Composable that displays an AdMob banner ad.
 * Uses AndroidView to host the native AdView in Compose.
 *
 * @param adUnitId The AdMob ad unit ID (use test ID during development)
 * @param modifier Modifier for the composable
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_AD_UNIT_ID
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

// Test ad unit IDs - replace with real IDs for production
const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
