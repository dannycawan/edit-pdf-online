/**
 * Purpose: Application class for Edit PDF Online - Text Editor
 * Caller: Android OS (specified in AndroidManifest.xml)
 * Dependencies: PdfBox-Android (PDFBoxResourceLoader)
 * Main Functions: onCreate - initializes PdfBox resource loader
 * Side Effects: Initializes PdfBox-Android globally for the app lifecycle
 */
package com.editpdf.online

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class EditPdfApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize PdfBox-Android resource loader
        // This must be called before any PdfBox operations
        PDFBoxResourceLoader.init(applicationContext)
    }
}
