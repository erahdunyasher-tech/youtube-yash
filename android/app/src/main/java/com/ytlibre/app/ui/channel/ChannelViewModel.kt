package com.ytlibre.app.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytlibre.app.data.model.Channel
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.data.repository.YouTubeRepository
import com.ytlibre.app.ui.common.AppResult
import com.ytlibre.app.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Page d'une chaîne : informations publiques et dernières vidéos.
 */
class ChannelViewModel(private val repository: YouTubeRepository) : ViewModel() {

    private val _channel = MutableStateFlow<UiState<Channel>>(UiState.Idle)
    val channel: StateFlow<UiState<Channel>> = _channel.asStateFlow()

    private val _videos = MutableStateFlow<UiState<List<Video>>>(UiState.Idle)
    val videos: StateFlow<UiState<List<Video>>> = _videos.asStateFlow()

    private var loadedChannelId: String? = null

    fun load(channelId: String) {
        if (channelId.isBlank() || loadedChannelId == channelId) return
        loadedChannelId = channelId
        fetch(channelId)
    }

    /** Force un nouveau chargement (bouton « Réessayer »). */
    fun reload(channelId: String) {
        loadedChannelId = null
        load(channelId)
    }

    private fun fetch(channelId: String) {
        viewModelScope.launch {
            _channel.value = UiState.Loading
            _channel.value = when (val result = repository.channelDetails(channelId)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Failure -> UiState.Error(result.error)
            }
            _videos.value = UiState.Loading
            _videos.value = when (val result = repository.channelVideos(channelId)) {
                is AppResult.Success -> UiState.Success(result.data)
                is AppResult.Failure -> UiState.Error(result.error)
            }
        }
    }
}
