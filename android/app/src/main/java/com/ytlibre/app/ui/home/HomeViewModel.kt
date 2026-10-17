package com.ytlibre.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytlibre.app.data.model.ContentCategory
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.data.repository.YouTubeRepository
import com.ytlibre.app.ui.common.AppResult
import com.ytlibre.app.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Écran d'accueil : tendances YouTube par catégorie.
 */
class HomeViewModel(private val repository: YouTubeRepository) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Video>>>(UiState.Idle)
    val state: StateFlow<UiState<List<Video>>> = _state.asStateFlow()

    private val _category = MutableStateFlow(ContentCategory.ALL)
    val category: StateFlow<ContentCategory> = _category.asStateFlow()

    private var loadJob: Job? = null

    fun selectCategory(category: ContentCategory) {
        if (_category.value == category) return
        _category.value = category
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.value = UiState.Loading
            _state.value = when (val result = repository.trending(_category.value)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}
