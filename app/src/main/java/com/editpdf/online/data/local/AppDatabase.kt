/**
 * Purpose: Room database definition for Edit PDF Online
 * Caller: Application (singleton), Repository classes
 * Dependencies: Room, RecentFile entity
 * Main Functions: Provides database instance and DAOs
 * Side Effects: Creates SQLite database on first access
 */
package com.editpdf.online.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.editpdf.online.data.model.RecentFile

@Database(
    entities = [RecentFile::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recentFileDao(): RecentFileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "edit_pdf_online.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
