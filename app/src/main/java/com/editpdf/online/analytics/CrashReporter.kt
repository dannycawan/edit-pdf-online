/**
 * Purpose: Crashlytics wrapper for non-sensitive error logging
 * Caller: All error handlers across the app
 * Dependencies: Firebase Crashlytics (stubbed until Firebase is configured)
 * Main Functions: logError, logMessage, setCustomKey
 * Side Effects: Sends crash reports to Firebase Crashlytics
 */
package com.editpdf.online.analytics

/**
 * Crashlytics wrapper that logs non-sensitive error metadata.
 * Uses stub implementation until Firebase is configured.
 *
 * IMPORTANT: Never log PDF content, user document text, signature images,
 * or private document names.
 */
object CrashReporter {

    // Stub: Replace with FirebaseCrashlytics.getInstance() when Firebase is configured
    // private val crashlytics = FirebaseCrashlytics.getInstance()

    /**
     * Logs a non-fatal exception to Crashlytics.
     */
    fun logError(exception: Throwable, context: String = "") {
        // Stub: Uncomment when Firebase is configured
        // crashlytics.recordException(exception)
        // if (context.isNotBlank()) {
        //     crashlytics.log("Context: $context")
        // }

        android.util.Log.e("CrashReporter", "Error in $context", exception)
    }

    /**
     * Logs a message to Crashlytics for additional context.
     */
    fun logMessage(message: String) {
        // crashlytics.log(message)
        android.util.Log.d("CrashReporter", message)
    }

    /**
     * Sets a custom key-value pair for crash report context.
     * Do not set sensitive values.
     */
    fun setCustomKey(key: String, value: String) {
        // crashlytics.setCustomKey(key, value)
        android.util.Log.d("CrashReporter", "Custom key: $key = $value")
    }

    fun setCustomKey(key: String, value: Int) {
        // crashlytics.setCustomKey(key, value)
        android.util.Log.d("CrashReporter", "Custom key: $key = $value")
    }

    fun setCustomKey(key: String, value: Boolean) {
        // crashlytics.setCustomKey(key, value)
        android.util.Log.d("CrashReporter", "Custom key: $key = $value")
    }
}
