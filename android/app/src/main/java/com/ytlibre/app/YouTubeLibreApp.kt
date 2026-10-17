package com.ytlibre.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.ytlibre.app.data.local.SecurePreferences
import com.ytlibre.app.di.AppContainer

/**
 * Point d'entrée de l'application.
 * Conteneur d'injection manual : aucun framework supplémentaire, rapide à démarrer.
 */
class YouTubeLibreApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        applyTheme(container.preferences.themeMode)
    }

    companion object {
        fun applyTheme(mode: SecurePreferences.ThemeMode) {
            AppCompatDelegate.setDefaultNightMode(
                when (mode) {
                    SecurePreferences.ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    SecurePreferences.ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                    SecurePreferences.ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                }
            )
        }
    }
}
