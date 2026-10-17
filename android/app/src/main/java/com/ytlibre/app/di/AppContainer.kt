package com.ytlibre.app.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Room
import com.ytlibre.app.data.local.AppDatabase
import com.ytlibre.app.data.local.SecurePreferences
import com.ytlibre.app.data.remote.NetworkModule
import com.ytlibre.app.data.repository.LibraryRepository
import com.ytlibre.app.data.repository.YouTubeRepository
import com.ytlibre.app.ui.channel.ChannelViewModel
import com.ytlibre.app.ui.home.HomeViewModel
import com.ytlibre.app.ui.library.LibraryViewModel
import com.ytlibre.app.ui.search.SearchViewModel
import com.ytlibre.app.ui.settings.SettingsViewModel
import com.ytlibre.app.ui.watch.WatchViewModel

/**
 * Conteneur de dépendances : une seule instance par application.
 * Facilement remplaçable par Hilt/Koin si le projet grossit.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val preferences: SecurePreferences by lazy { SecurePreferences(appContext) }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "ytlibre.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val network: NetworkModule by lazy {
        NetworkModule(
            cacheDirectory = appContext.cacheDir,
            apiKeyProvider = { preferences.apiKey },
            proxyUrlProvider = { preferences.proxyBaseUrl }
        )
    }

    val youtubeRepository: YouTubeRepository by lazy {
        YouTubeRepository(network, regionProvider = { preferences.regionCode })
    }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(database, preferences)
    }

    val viewModelFactory: ViewModelProvider.Factory = viewModelFactory {
        initializer { HomeViewModel(youtubeRepository) }
        initializer { SearchViewModel(youtubeRepository, preferences) }
        initializer { WatchViewModel(youtubeRepository, libraryRepository) }
        initializer { LibraryViewModel(libraryRepository) }
        initializer { ChannelViewModel(youtubeRepository) }
        initializer { SettingsViewModel(preferences, youtubeRepository, libraryRepository) }
    }
}
