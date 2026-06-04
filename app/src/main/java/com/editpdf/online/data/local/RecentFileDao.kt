/**
 * Purpose: Room DAO for recent files CRUD operations
 * Caller: RecentFileRepository
 * Dependencies: Room, RecentFile entity
 * Main Functions: Insert, query, delete recent files
 * Side Effects: Reads/writes to SQLite database
 */
package com.editpdf.online.data.local

import androidx.room.*
import com.editpdf.online.data.model.RecentFile
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {

    @Query("SELECT * FROM recent_files ORDER BY lastOpenedTime DESC")
    fun getAllRecentFiles(): Flow<List<RecentFile>>

    @Query("SELECT * FROM recent_files ORDER BY lastOpenedTime DESC LIMIT :limit")
    fun getRecentFiles(limit: Int): Flow<List<RecentFile>>

    @Query("SELECT * FROM recent_files WHERE id = :id")
    suspend fun getRecentFileById(id: String): RecentFile?

    @Query("SELECT * FROM recent_files WHERE fileUri = :uri LIMIT 1")
    suspend fun getRecentFileByUri(uri: String): RecentFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(recentFile: RecentFile)

    @Update
    suspend fun updateRecentFile(recentFile: RecentFile)

    @Delete
    suspend fun deleteRecentFile(recentFile: RecentFile)

    @Query("DELETE FROM recent_files WHERE id = :id")
    suspend fun deleteRecentFileById(id: String)

    @Query("DELETE FROM recent_files")
    suspend fun deleteAllRecentFiles()

    @Query("SELECT COUNT(*) FROM recent_files")
    suspend fun getRecentFileCount(): Int
}
