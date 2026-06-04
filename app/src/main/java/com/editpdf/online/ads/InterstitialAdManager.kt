/**
 * Purpose: AdMob interstitial ad manager with loading and showing logic
 * Caller: EditorViewModel (after export), ToolsViewModel (after tool success)
 * Dependencies: Google Mobile Ads SDK, AdFrequencyManager
 * Main Functions: loadInterstitial, showInterstitial
 * Side Effects: Loads/shows full-screen interstitial ads
 */
package com.editpdf.online.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Manages interstitial ad lifecycle (load, show, reload).
 * Works with AdFrequencyManager to enforce frequency caps.
 */
class InterstitialAdManager(
    private val context: Context,
    private val frequencyManager: AdFrequencyManager
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    companion object {
        private const val TAG = "InterstitialAdManager"
    }

    /**
     * Preloads an interstitial ad.
     */
    fun loadInterstitial(adUnitId: String = TEST_INTERSTITIAL_AD_UNIT_ID) {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    Log.d(TAG, "Interstitial ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading = false
                    Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows the interstitial ad if one is loaded and frequency cap allows it.
     * Only call this after a successful export/action.
     *
     * @param activity The activity to show the ad on
     * @param onAdDismissed Callback when ad is dismissed or not shown
     * @return true if ad was shown, false if skipped
     */
    fun showInterstitialIfReady(
        activity: Activity,
        onAdDismissed: () -> Unit = {}
    ): Boolean {
        if (!frequencyManager.canShowInterstitial()) {
            Log.d(TAG, "Interstitial skipped: frequency cap")
            onAdDismissed()
            return false
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Interstitial not loaded, reloading")
            loadInterstitial()
            onAdDismissed()
            return false
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial() // Preload next ad
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial()
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                frequencyManager.recordInterstitialShown()
            }
        }

        ad.show(activity)
        return true
    }

    /**
     * Checks if an interstitial ad is ready to show.
     */
    fun isAdReady(): Boolean = interstitialAd != null
}
