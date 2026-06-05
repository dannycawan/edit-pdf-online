package com.editpdf.online.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleUtils {

    /**
     * Applies the correct app locale:
     * - Indonesian devices ("in" / "id") → keep Indonesian so values-in resources are used
     * - All other locales → force English so the app never shows a half-translated UI
     */
    fun applyAppLocale(context: Context): Context {
        val deviceLanguage = Locale.getDefault().language
        if (deviceLanguage == "in" || deviceLanguage == "id") {
            // Indonesian device — let Android pick values-in/strings.xml naturally
            return context
        }
        // Non-Indonesian device — force English
        val locale = Locale.ENGLISH
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }
}
