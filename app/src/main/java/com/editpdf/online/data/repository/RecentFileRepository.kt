/**
 * Purpose: Repository for recent files data operations
 * Caller: ViewModels (HomeViewModel, RecentFilesViewModel, EditorViewModel)
 * Dependencies: RecentFileDao, AppDatabase
 * Main Functions: CRUD wrapper for recent files
 * Side Effects: Database operations
 */
package com.editpdf.online.data.repository

import android.content.Context
import com.editpdf.online.data.local.AppDatabase
import com.editpdf.online.data.model.RecentFile
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class RecentFileRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).recentFileDao()

    fun getAllRecentFiles(): Flow<List<RecentFile>> = dao.getAllRecentFiles()

    fun getRecentFiles(limit: Int = 5): Flow<List<RecentFile>> = dao.getRecentFiles(limit)

    suspend fun addOrUpdateRecentFile(
        fileName: String,
        fileUri: String,
        fileSizeBytes: Long = 0L,
        pageCount: Int = 0
    ) {
        val existing = dao.getRecentFileByUri(fileUri)
        if (existing != null) {
            dao.updateRecentFile(
                existing.copy(
                    fileName = fileName,
                    lastOpenedTime = System.currentTimeMillis(),
                    fileSizeBytes = fileSizeBytes,
                    pageCount = pageCount
                )
            )
        } else {
            dao.insertRecentFile(
                RecentFile(
                    id = UUID.randomUUID().toString(),
                    fileName = fileName,
                    fileUri = fileUri,
                    fileSizeBytes = fileSizeBytes,
                    pageCount = pageCount,
                    lastOpenedTime = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun markAsExported(fileUri: String) {
        val existing = dao.getRecentFileByUri(fileUri)
        if (existing != null) {
            dao.updateRecentFile(
                existing.copy(
                    lastExportedTime = System.currentTimeMillis(),
                    isExported = true
                )
            )
        }
    }

    suspend fun deleteRecentFile(id: String) = dao.deleteRecentFileById(id)

    suspend fun deleteAllRecentFiles() = dao.deleteAllRecentFiles()
}
