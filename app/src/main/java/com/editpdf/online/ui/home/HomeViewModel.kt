/**
 * Purpose: ViewModel for the Home screen
 * Caller: HomeScreen
 * Dependencies: RecentFileRepository
 * Main Functions: Provides recent files data flow
 * Side Effects: Database reads
 */
package com.editpdf.online.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.editpdf.online.data.model.RecentFile
import com.editpdf.online.data.repository.RecentFileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecentFileRepository(application.applicationContext)

    /**
     * Recent files flow (latest 5 for home screen).
     */
    val recentFiles: StateFlow<List<RecentFile>> = repository.getRecentFiles(5)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Delete a recent file by ID.
     */
    fun deleteRecentFile(id: String) {
        viewModelScope.launch {
            repository.deleteRecentFile(id)
        }
    }

    /**
     * Clear all recent files.
     */
    fun clearAllRecentFiles() {
        viewModelScope.launch {
            repository.deleteAllRecentFiles()
        }
    }
}
