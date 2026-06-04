/**
 * Purpose: Share utilities for sharing PDF files via Android Intent
 * Caller: EditorViewModel, export success flow
 * Dependencies: Android Intent, FileProvider
 * Main Functions: sharePdf
 * Side Effects: Launches Android share sheet
 */
package com.editpdf.online.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {

    /**
     * Shares a PDF file using Android share intent.
     *
     * @param context The context to start the activity from
     * @param file The PDF file to share
     * @return true if sharing was initiated, false if no app available
     */
    fun sharePdf(context: Context, file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            sharePdfUri(context, uri)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Shares a PDF file by URI using Android share intent.
     */
    fun sharePdfUri(context: Context, uri: Uri): Boolean {
        return try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, null)
            if (chooser.resolveActivity(context.packageManager) != null) {
                context.startActivity(chooser)
                true
            } else {
                // Try starting directly even without resolveActivity
                context.startActivity(chooser)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    fun shareFiles(context: Context, files: List<File>, mimeType: String): Boolean {
        if (files.isEmpty()) return false

        return try {
            val uris = files.map { file ->
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            val intent = if (uris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uris.first())
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = mimeType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                }
            }.apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, null))
            true
        } catch (_: Exception) {
            false
        }
    }
}
