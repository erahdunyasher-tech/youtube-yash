package com.ytlibre.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Bibliothèque locale : favoris et historique.
 */
class LibraryViewModel(private val libraryRepository: LibraryRepository) : ViewModel() {

    enum class Tab { FAVORITES, HISTORY }

    private val _tab = MutableStateFlow(Tab.FAVORITES)
    val tab: StateFlow<Tab> = _tab.asStateFlow()

    val favorites: StateFlow<List<Video>> = libraryRepository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val history: StateFlow<List<Video>> = libraryRepository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectTab(tab: Tab) {
        _tab.value = tab
    }

    fun removeFavorite(videoId: String) {
        viewModelScope.launch { libraryRepository.removeFavorite(videoId) }
    }

    fun removeHistory(videoId: String) {
        viewModelScope.launch { libraryRepository.removeFromHistory(videoId) }
    }

    fun clearFavorites() {
        viewModelScope.launch { libraryRepository.clearFavorites() }
    }

    fun clearHistory() {
        viewModelScope.launch { libraryRepository.clearHistory() }
    }
}
