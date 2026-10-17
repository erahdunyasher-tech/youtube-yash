package com.ytlibre.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ytlibre.app.data.local.SecurePreferences
import com.ytlibre.app.data.repository.LibraryRepository
import com.ytlibre.app.data.repository.YouTubeRepository
import com.ytlibre.app.ui.common.AppResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Réglages : clé API (chiffrée), proxy serveur, région, thème, maintenance locale.
 */
class SettingsViewModel(
    private val preferences: SecurePreferences,
    private val repository: YouTubeRepository,
    private val libraryRepository: LibraryRepository
) : ViewModel() {

    sealed interface TestResult {
        data object Idle : TestResult
        data object Running : TestResult
        data class Success(val message: String) : TestResult
        data class Failure(val message: String) : TestResult
    }

    private val _testResult = MutableStateFlow<TestResult>(TestResult.Idle)
    val testResult: StateFlow<TestResult> = _testResult.asStateFlow()

    private val _isProxyMode = MutableStateFlow(!preferences.proxyBaseUrl.isNullOrBlank())
    val isProxyMode: StateFlow<Boolean> = _isProxyMode.asStateFlow()

    val apiKey: String get() = preferences.apiKey.orEmpty()
    val proxyUrl: String get() = preferences.proxyBaseUrl.orEmpty()
    val regionCode: String get() = preferences.regionCode
    val themeMode: SecurePreferences.ThemeMode get() = preferences.themeMode
    val historyLimit: Int get() = preferences.historyLimit

    fun saveApiKey(value: String) {
        preferences.apiKey = value
    }

    fun saveProxyUrl(value: String) {
        preferences.proxyBaseUrl = value
        _isProxyMode.value = !value.isNullOrBlank()
        repository.clearCache()
    }

    fun saveRegion(value: String) {
        preferences.regionCode = value
        repository.clearCache()
    }

    fun saveTheme(mode: SecurePreferences.ThemeMode) {
        preferences.themeMode = mode
    }

    fun saveHistoryLimit(limit: Int) {
        preferences.historyLimit = limit
    }

    /** Vérifie que la configuration courante permet d'appeler l'API. */
    fun testConnection() {
        viewModelScope.launch {
            _testResult.value = TestResult.Running
            repository.clearCache()
            _testResult.value = when (val result = repository.trending()) {
                is AppResult.Success ->
                    TestResult.Success("Connexion réussie : ${result.data.size} vidéos reçues.")

                is AppResult.Failure ->
                    TestResult.Failure(result.error.userMessage)
            }
        }
    }

    fun resetTestResult() {
        _testResult.value = TestResult.Idle
    }

    fun clearFavorites() {
        viewModelScope.launch { libraryRepository.clearFavorites() }
    }

    fun clearHistory() {
        viewModelScope.launch { libraryRepository.clearHistory() }
    }

    fun clearRecentQueries() {
        preferences.clearRecentQueries()
    }
}
