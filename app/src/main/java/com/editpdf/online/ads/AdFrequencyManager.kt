/**
 * Purpose: Frequency cap logic for interstitial ads
 * Caller: InterstitialAdManager
 * Dependencies: RemoteConfigManager
 * Main Functions: canShowInterstitial, recordInterstitialShown
 * Side Effects: Tracks ad show timestamps and counts
 */
package com.editpdf.online.ads

import com.editpdf.online.config.RemoteConfigManager

/**
 * Enforces frequency caps on interstitial ads.
 *
 * Rules:
 * - Show 1 interstitial every N successful exports/actions (default: 3)
 * - Minimum M seconds between interstitials (default: 90)
 */
class AdFrequencyManager(
    private val remoteConfig: RemoteConfigManager
) {
    private var actionCount = 0
    private var lastInterstitialTimestamp = 0L

    /**
     * Records a successful action (export, merge, convert, etc.)
     * Call this every time a monetizable action completes successfully.
     */
    fun recordSuccessfulAction() {
        actionCount++
    }

    /**
     * Records that an interstitial was shown.
     * Resets the action count and updates the last shown timestamp.
     */
    fun recordInterstitialShown() {
        actionCount = 0
        lastInterstitialTimestamp = System.currentTimeMillis()
    }

    /**
     * Checks whether an interstitial can be shown based on frequency caps.
     *
     * @return true if frequency cap allows showing an interstitial
     */
    fun canShowInterstitial(): Boolean {
        if (!remoteConfig.isInterstitialAfterExportEnabled) return false

        val frequency = remoteConfig.interstitialFrequency
        val minSeconds = remoteConfig.interstitialMinSeconds

        // Check action count
        if (actionCount < frequency) return false

        // Check minimum time between interstitials
        val elapsed = (System.currentTimeMillis() - lastInterstitialTimestamp) / 1000
        if (elapsed < minSeconds) return false

        return true
    }

    /**
     * Resets all counters. Useful for testing.
     */
    fun reset() {
        actionCount = 0
        lastInterstitialTimestamp = 0L
    }
}
