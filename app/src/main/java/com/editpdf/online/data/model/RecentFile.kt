/**
 * Purpose: PDF file metadata model for recent files and file operations
 * Caller: RecentFileDao, RecentFileRepository, HomeViewModel
 * Dependencies: Room annotations
 * Main Functions: Data class for PDF file information
 * Side Effects: None (pure data model)
 */
package com.editpdf.online.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a recently opened/edited PDF file.
 * Stored in Room database for the Recent Files feature.
 */
@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey
    val id: String,
    val fileName: String,
    val fileUri: String,
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 0,
    val lastOpenedTime: Long = System.currentTimeMillis(),
    val lastExportedTime: Long? = null,
    val thumbnailPath: String? = null,
    val isExported: Boolean = false
)
