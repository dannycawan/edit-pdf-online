/**
 * Purpose: Remote Config manager for feature flags and ad controls
 * Caller: AdFrequencyManager, ViewModels
 * Dependencies: Firebase Remote Config (stubbed until Firebase is configured)
 * Main Functions: fetchConfig, getConfigValue for all parameters
 * Side Effects: Fetches remote configuration from Firebase
 */
package com.editpdf.online.config

/**
 * Manages Remote Config values for ad behavior and feature flags.
 * Uses local defaults until Firebase is configured.
 */
class RemoteConfigManager {

    // Default values matching specification
    private val defaults = mapOf(
        // Ads
        ADS_BANNER_HOME_ENABLED to true,
        ADS_BANNER_TOOLS_ENABLED to true,
        ADS_BANNER_RECENT_ENABLED to true,
        ADS_INTERSTITIAL_AFTER_EXPORT_ENABLED to true,
        ADS_INTERSTITIAL_FREQUENCY to 3L,
        ADS_INTERSTITIAL_MIN_SECONDS to 90L,
        ADS_REWARDED_ENABLED to false,
        ADS_APP_OPEN_ENABLED to false,

        // Features
        FEATURE_MERGE_PDF_ENABLED to true,
        FEATURE_SPLIT_PDF_ENABLED to true,
        FEATURE_ROTATE_PDF_ENABLED to true,
        FEATURE_IMAGE_TO_PDF_ENABLED to true,
        FEATURE_PDF_TO_IMAGE_ENABLED to true,
        FEATURE_OCR_ENABLED to false,
        FEATURE_PDF_TO_WORD_ENABLED to false,
        FEATURE_WORD_TO_PDF_ENABLED to false,
        FEATURE_LOGIN_ENABLED to false,
        FEATURE_CLOUD_ENABLED to false,
        FEATURE_SUBSCRIPTION_ENABLED to false,

        // Editor
        EDITOR_MAX_PDF_SIZE_MB to 50L,
        EDITOR_AUTO_SAVE_ENABLED to true
    )

    // Stub: Replace with FirebaseRemoteConfig when Firebase is configured
    // private val remoteConfig = FirebaseRemoteConfig.getInstance()

    /**
     * Fetches remote configuration.
     * Currently uses local defaults only.
     */
    suspend fun fetchConfig() {
        // Stub: Uncomment when Firebase is configured
        // remoteConfig.setDefaultsAsync(defaults)
        // remoteConfig.fetchAndActivate()
    }

    fun getBoolean(key: String): Boolean =
        defaults[key] as? Boolean ?: false

    fun getLong(key: String): Long =
        defaults[key] as? Long ?: 0L

    fun getInt(key: String): Int =
        getLong(key).toInt()

    // ==================== Convenience Getters ====================

    val isBannerHomeEnabled: Boolean get() = getBoolean(ADS_BANNER_HOME_ENABLED)
    val isBannerToolsEnabled: Boolean get() = getBoolean(ADS_BANNER_TOOLS_ENABLED)
    val isBannerRecentEnabled: Boolean get() = getBoolean(ADS_BANNER_RECENT_ENABLED)
    val isInterstitialAfterExportEnabled: Boolean get() = getBoolean(ADS_INTERSTITIAL_AFTER_EXPORT_ENABLED)
    val interstitialFrequency: Int get() = getInt(ADS_INTERSTITIAL_FREQUENCY)
    val interstitialMinSeconds: Int get() = getInt(ADS_INTERSTITIAL_MIN_SECONDS)
    val isRewardedEnabled: Boolean get() = getBoolean(ADS_REWARDED_ENABLED)
    val isAppOpenEnabled: Boolean get() = getBoolean(ADS_APP_OPEN_ENABLED)

    val isMergePdfEnabled: Boolean get() = getBoolean(FEATURE_MERGE_PDF_ENABLED)
    val isSplitPdfEnabled: Boolean get() = getBoolean(FEATURE_SPLIT_PDF_ENABLED)
    val isRotatePdfEnabled: Boolean get() = getBoolean(FEATURE_ROTATE_PDF_ENABLED)
    val isImageToPdfEnabled: Boolean get() = getBoolean(FEATURE_IMAGE_TO_PDF_ENABLED)
    val isPdfToImageEnabled: Boolean get() = getBoolean(FEATURE_PDF_TO_IMAGE_ENABLED)
    val maxPdfSizeMb: Int get() = getInt(EDITOR_MAX_PDF_SIZE_MB)

    companion object {
        const val ADS_BANNER_HOME_ENABLED = "ads_banner_home_enabled"
        const val ADS_BANNER_TOOLS_ENABLED = "ads_banner_tools_enabled"
        const val ADS_BANNER_RECENT_ENABLED = "ads_banner_recent_enabled"
        const val ADS_INTERSTITIAL_AFTER_EXPORT_ENABLED = "ads_interstitial_after_export_enabled"
        const val ADS_INTERSTITIAL_FREQUENCY = "ads_interstitial_frequency"
        const val ADS_INTERSTITIAL_MIN_SECONDS = "ads_interstitial_min_seconds"
        const val ADS_REWARDED_ENABLED = "ads_rewarded_enabled"
        const val ADS_APP_OPEN_ENABLED = "ads_app_open_enabled"

        const val FEATURE_MERGE_PDF_ENABLED = "feature_merge_pdf_enabled"
        const val FEATURE_SPLIT_PDF_ENABLED = "feature_split_pdf_enabled"
        const val FEATURE_ROTATE_PDF_ENABLED = "feature_rotate_pdf_enabled"
        const val FEATURE_IMAGE_TO_PDF_ENABLED = "feature_image_to_pdf_enabled"
        const val FEATURE_PDF_TO_IMAGE_ENABLED = "feature_pdf_to_image_enabled"
        const val FEATURE_OCR_ENABLED = "feature_ocr_enabled"
        const val FEATURE_PDF_TO_WORD_ENABLED = "feature_pdf_to_word_enabled"
        const val FEATURE_WORD_TO_PDF_ENABLED = "feature_word_to_pdf_enabled"
        const val FEATURE_LOGIN_ENABLED = "feature_login_enabled"
        const val FEATURE_CLOUD_ENABLED = "feature_cloud_enabled"
        const val FEATURE_SUBSCRIPTION_ENABLED = "feature_subscription_enabled"

        const val EDITOR_MAX_PDF_SIZE_MB = "editor_max_pdf_size_mb"
        const val EDITOR_AUTO_SAVE_ENABLED = "editor_auto_save_enabled"
    }
}
