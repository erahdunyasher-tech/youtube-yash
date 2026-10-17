package com.ytlibre.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytlibre.app.data.local.SecurePreferences
import com.ytlibre.app.data.model.SearchFilter
import com.ytlibre.app.data.model.SearchResultItem
import com.ytlibre.app.data.repository.YouTubeRepository
import com.ytlibre.app.ui.common.AppResult
import com.ytlibre.app.ui.common.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Recherche de vidéos et de chaînes (debounce 400 ms, pagination).
 */
class SearchViewModel(
    private val repository: YouTubeRepository,
    private val preferences: SecurePreferences
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<SearchResultItem>>>(UiState.Idle)
    val state: StateFlow<UiState<List<SearchResultItem>>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter.ALL)
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _recentQueries = MutableStateFlow(preferences.recentQueries)
    val recentQueries: StateFlow<List<String>> = _recentQueries.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var searchJob: Job? = null
    private var nextPageToken: String? = null

    fun onQueryChanged(raw: String) {
        _query.value = raw
        searchJob?.cancel()
        if (raw.isBlank()) {
            _state.value = UiState.Idle
            nextPageToken = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            performSearch(reset = true)
        }
    }

    fun onFilterChanged(filter: SearchFilter) {
        if (_filter.value == filter) return
        _filter.value = filter
        if (_query.value.isNotBlank()) {
            searchJob?.cancel()
            searchJob = viewModelScope.launch { performSearch(reset = true) }
        }
    }

    fun submitQuery(raw: String = _query.value) {
        val cleaned = raw.trim()
        if (cleaned.isBlank()) return
        _query.value = cleaned
        preferences.addRecentQuery(cleaned)
        _recentQueries.value = preferences.recentQueries
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(reset = true) }
    }

    fun loadMore() {
        val token = nextPageToken ?: return
        if (searchJob?.isActive == true) return
        searchJob = viewModelScope.launch { performSearch(reset = false, pageToken = token) }
    }

    fun clearRecent() {
        preferences.clearRecentQueries()
        _recentQueries.value = emptyList()
    }

    private suspend fun performSearch(reset: Boolean, pageToken: String? = null) {
        val query = _query.value.trim()
        if (query.isBlank()) return
        if (reset) {
            _state.value = UiState.Loading
        }
        when (val result = repository.search(query, _filter.value, pageToken)) {
            is AppResult.Success -> {
                nextPageToken = result.data.nextPageToken
                _hasMore.value = !result.data.nextPageToken.isNullOrBlank()
                val previous = if (reset) emptyList() else (_state.value as? UiState.Success)?.data.orEmpty()
                _state.value = UiState.Success(previous + result.data.items)
            }

            is AppResult.Failure -> {
                if (reset) _state.value = UiState.Error(result.error)
            }
        }
    }

    companion object {
        private const val DEBOUNCE_MS = 400L
    }
}
