package com.ytlibre.app.data.repository

import com.ytlibre.app.data.local.AppDatabase
import com.ytlibre.app.data.local.FavoriteEntity
import com.ytlibre.app.data.local.HistoryEntity
import com.ytlibre.app.data.local.SecurePreferences
import com.ytlibre.app.data.model.Video
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Favoris et historique : 100 % local (base Room), jamais synchronisés.
 */
class LibraryRepository(
    private val database: AppDatabase,
    private val preferences: SecurePreferences,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val favorites: Flow<List<Video>> =
        database.favoriteDao().observeAll().map { list -> list.map { it.toVideo() } }

    val history: Flow<List<Video>> =
        database.historyDao().observeRecent(preferences.historyLimit)
            .map { list -> list.map { it.toVideo() } }

    fun isFavorite(videoId: String): Flow<Boolean> =
        database.favoriteDao().observeIsFavorite(videoId)

    suspend fun addToHistory(video: Video) = withContext(ioDispatcher) {
        database.historyDao().upsert(
            HistoryEntity(
                videoId = video.id,
                title = video.title,
                channelId = video.channelId,
                channelTitle = video.channelTitle,
                thumbnailUrl = video.thumbnailUrl,
                durationSeconds = video.durationSeconds,
                publishedAt = video.publishedAt,
                watchedAt = System.currentTimeMillis()
            )
        )
        database.historyDao().trimTo(preferences.historyLimit)
    }

    suspend fun toggleFavorite(video: Video) = withContext(ioDispatcher) {
        if (database.favoriteDao().findById(video.id) != null) {
            database.favoriteDao().deleteById(video.id)
        } else {
            database.favoriteDao().upsert(
                FavoriteEntity(
                    videoId = video.id,
                    title = video.title,
                    channelId = video.channelId,
                    channelTitle = video.channelTitle,
                    thumbnailUrl = video.thumbnailUrl,
                    durationSeconds = video.durationSeconds,
                    publishedAt = video.publishedAt,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeFavorite(videoId: String) = withContext(ioDispatcher) {
        database.favoriteDao().deleteById(videoId)
    }

    suspend fun clearFavorites() = withContext(ioDispatcher) { database.favoriteDao().clear() }

    suspend fun clearHistory() = withContext(ioDispatcher) { database.historyDao().clear() }

    suspend fun removeFromHistory(videoId: String) = withContext(ioDispatcher) {
        database.historyDao().deleteById(videoId)
    }
}

private fun FavoriteEntity.toVideo() = Video(
    id = videoId,
    title = title,
    channelId = channelId,
    channelTitle = channelTitle,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    publishedAt = publishedAt
)

private fun HistoryEntity.toVideo() = Video(
    id = videoId,
    title = title,
    channelId = channelId,
    channelTitle = channelTitle,
    thumbnailUrl = thumbnailUrl,
    durationSeconds = durationSeconds,
    publishedAt = publishedAt
)
