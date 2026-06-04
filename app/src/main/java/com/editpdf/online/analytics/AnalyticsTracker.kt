/**
 * Purpose: Centralized Firebase Analytics event tracking
 * Caller: All ViewModels and UI event handlers
 * Dependencies: Firebase Analytics (stubbed until Firebase is configured)
 * Main Functions: Track all analytics events defined in the specification
 * Side Effects: Sends analytics events to Firebase
 */
package com.editpdf.online.analytics

import android.content.Context
import android.os.Bundle

/**
 * Centralized analytics tracker for all app events.
 * Uses a stub implementation until Firebase is configured with google-services.json.
 * To enable Firebase Analytics, uncomment the Firebase dependencies and replace
 * the stub calls with real FirebaseAnalytics calls.
 */
class AnalyticsTracker(private val context: Context) {

    // Stub: Replace with FirebaseAnalytics.getInstance(context) when Firebase is configured
    // private val firebaseAnalytics = FirebaseAnalytics.getInstance(context)

    private fun logEvent(eventName: String, params: Bundle? = null) {
        // Stub: Uncomment when Firebase is configured
        // firebaseAnalytics.logEvent(eventName, params)

        // Debug logging during development
        android.util.Log.d("Analytics", "Event: $eventName, Params: $params")
    }

    // ==================== Core PDF Open ====================

    fun trackOpenPdfClicked() = logEvent("open_pdf_clicked")

    fun trackPdfOpenSuccess(pageCount: Int, fileSizeMb: Float) {
        logEvent("pdf_open_success", Bundle().apply {
            putInt("page_count", pageCount)
            putFloat("file_size_mb", fileSizeMb)
            putString("size_bucket", getFileSizeBucket(fileSizeMb))
        })
    }

    fun trackPdfOpenFailed(errorType: String) {
        logEvent("pdf_open_failed", Bundle().apply {
            putString("error_type", errorType)
        })
    }

    // ==================== Editor ====================

    fun trackEditTextClicked() = logEvent("edit_text_clicked")
    fun trackAddTextClicked() = logEvent("add_text_clicked")
    fun trackCoverOldTextClicked() = logEvent("cover_old_text_clicked")
    fun trackReplaceTextClicked() = logEvent("replace_text_clicked")
    fun trackSignatureClicked() = logEvent("signature_clicked")
    fun trackFillFormClicked() = logEvent("fill_form_clicked")
    fun trackCheckmarkClicked() = logEvent("checkmark_clicked")

    // ==================== Export/Share ====================

    fun trackExportClicked() = logEvent("export_clicked")

    fun trackExportSuccess(pageCount: Int) {
        logEvent("export_success", Bundle().apply {
            putInt("page_count", pageCount)
        })
    }

    fun trackExportFailed(errorType: String) {
        logEvent("export_failed", Bundle().apply {
            putString("error_type", errorType)
        })
    }

    fun trackShareClicked() = logEvent("share_clicked")

    // ==================== Recent ====================

    fun trackRecentFileOpened() = logEvent("recent_file_opened")

    // ==================== Ads ====================

    fun trackAdBannerLoaded(placement: String) {
        logEvent("ad_banner_loaded", Bundle().apply {
            putString("placement", placement)
        })
    }

    fun trackAdBannerClicked(placement: String) {
        logEvent("ad_banner_clicked", Bundle().apply {
            putString("placement", placement)
        })
    }

    fun trackAdInterstitialLoaded() = logEvent("ad_interstitial_loaded")
    fun trackAdInterstitialShown() = logEvent("ad_interstitial_shown")
    fun trackAdInterstitialClicked() = logEvent("ad_interstitial_clicked")
    fun trackAdInterstitialDismissed() = logEvent("ad_interstitial_dismissed")

    // ==================== Remote Config ====================

    fun trackRemoteConfigFetched() = logEvent("remote_config_fetched")

    // ==================== Tools ====================

    fun trackToolUsed(toolName: String) {
        logEvent("tool_used", Bundle().apply {
            putString("tool_name", toolName)
        })
    }

    fun trackToolSuccess(toolName: String) {
        logEvent("tool_success", Bundle().apply {
            putString("tool_name", toolName)
        })
    }

    fun trackToolFailed(toolName: String, errorType: String) {
        logEvent("tool_failed", Bundle().apply {
            putString("tool_name", toolName)
            putString("error_type", errorType)
        })
    }

    // ==================== Helpers ====================

    private fun getFileSizeBucket(sizeMb: Float): String = when {
        sizeMb < 1 -> "under_1mb"
        sizeMb < 5 -> "1_5mb"
        sizeMb < 10 -> "5_10mb"
        sizeMb < 25 -> "10_25mb"
        sizeMb < 50 -> "25_50mb"
        else -> "over_50mb"
    }
}
