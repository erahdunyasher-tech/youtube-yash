package com.ytlibre.app.ui.watch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.data.repository.LibraryRepository
import com.ytlibre.app.data.repository.YouTubeRepository
import com.ytlibre.app.ui.common.AppResult
import com.ytlibre.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Écran de lecture : métadonnées de la vidéo, favoris et vidéos similaires.
 *
 * Le flux vidéo est assuré par le lecteur officiel intégré (WebView / embed
 * YouTube). L'application ne télécharge, n'extrait et ne contourne rien.
 */
class WatchViewModel(
    private val repository: YouTubeRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    private val _video = MutableStateFlow<UiState<Video>>(UiState.Idle)
    val video: StateFlow<UiState<Video>> = _video.asStateFlow()

    private val _related = MutableStateFlow<UiState<List<Video>>>(UiState.Idle)
    val related: StateFlow<UiState<List<Video>>> = _related.asStateFlow()

    private val _videoId = MutableStateFlow("")

    val isFavorite: StateFlow<Boolean> = _videoId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(false) else libraryRepository.isFavorite(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun load(videoId: String) {
        if (videoId.isBlank() || _videoId.value == videoId) return
        _videoId.value = videoId
        viewModelScope.launch {
            _video.value = UiState.Loading
            _video.value = when (val result = repository.videoDetails(videoId)) {
                is AppResult.Success -> {
                    libraryRepository.addToHistory(result.data)
                    UiState.Success(result.data)
                }

                is AppResult.Failure -> UiState.Error(result.error)
            }
            _related.value = UiState.Loading
            _related.value = when (val result = repository.relatedVideos(videoId)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Failure -> UiState.Error(result.error)
            }
        }
    }

    fun toggleFavorite() {
        val current = (_video.value as? UiState.Success)?.data ?: return
        viewModelScope.launch { libraryRepository.toggleFavorite(current) }
    }
}
