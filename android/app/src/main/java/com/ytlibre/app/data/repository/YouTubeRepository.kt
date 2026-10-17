package com.ytlibre.app.data.repository

import com.ytlibre.app.data.model.Channel
import com.ytlibre.app.data.model.ContentCategory
import com.ytlibre.app.data.model.SearchFilter
import com.ytlibre.app.data.model.SearchPage
import com.ytlibre.app.data.model.SearchResultItem
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.data.remote.ApiErrorMapper
import com.ytlibre.app.data.remote.Mappers
import com.ytlibre.app.data.remote.NetworkModule
import com.ytlibre.app.ui.common.AppError
import com.ytlibre.app.ui.common.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Accès aux données YouTube (métadonnées uniquement via la Data API v3).
 *
 * Objectifs :
 *  - limiter la consommation de quota (cache mémoire TTL + cache HTTP) ;
 *  - ne jamais faire planter l'UI en cas d'erreur réseau/quota ;
 *  - n'extraire aucun flux vidéo (l'application ne fait que *afficher* la vidéo
 *    via le lecteur officiel intégré par YouTube).
 */
class YouTubeRepository(
    private val network: NetworkModule,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val regionProvider: () -> String? = { null }
) {

    private data class CacheEntry<T>(val value: T, val expiresAt: Long)

    private val memoryCache = ConcurrentHashMap<String, CacheEntry<Any>>()

    // -----------------------------------------------------------------------
    // API publique
    // -----------------------------------------------------------------------

    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL,
        pageToken: String? = null
    ): AppResult<SearchPage> = cached("search:$query:${filter.name}:$pageToken") {
        apiCall {
            val key = network.apiKey()
            val response = network.api().search(
                part = "snippet",
                type = filter.apiType,
                query = query,
                channelId = null,
                relatedToVideoId = null,
                order = "relevance",
                maxResults = PAGE_SIZE,
                pageToken = pageToken,
                regionCode = null,
                videoCategoryId = null,
                safeSearch = "moderate",
                apiKey = key
            )
            val items = response.items.mapNotNull { Mappers.searchItemToDomain(it) }
            val enriched = enrichVideos(items)
            AppResult.Success(
                SearchPage(
                    items = enriched,
                    nextPageToken = response.nextPageToken,
                    totalResults = response.pageInfo?.totalResults ?: enriched.size
                )
            )
        }
    }

    suspend fun trending(category: ContentCategory = ContentCategory.ALL): AppResult<List<Video>> =
        cached("trending:${category.name}:${network.baseUrl()}") {
            apiCall {
                val response = network.api().videosChart(
                    part = "snippet,contentDetails,statistics",
                    chart = "mostPopular",
                    videoCategoryId = category.apiId.takeIf { it.isNotBlank() },
                    regionCode = regionCode(),
                    maxResults = PAGE_SIZE,
                    apiKey = network.apiKey()
                )
                AppResult.Success(response.items.mapNotNull { Mappers.videoToDomain(it) })
            }
        }

    suspend fun videoDetails(videoId: String): AppResult<Video> = cached("video:$videoId") {
        apiCall {
            val response = network.api().videos(
                part = "snippet,contentDetails,statistics,liveStreamingDetails",
                ids = videoId,
                apiKey = network.apiKey()
            )
            val video = response.items.firstOrNull()?.let { Mappers.videoToDomain(it) }
                ?: return@apiCall AppResult.Failure(AppError.ApiFailure(404, "Vidéo introuvable."))
            AppResult.Success(video)
        }
    }

    suspend fun relatedVideos(videoId: String): AppResult<List<Video>> =
        cached("related:$videoId") {
            apiCall {
                val response = network.api().search(
                    part = "snippet",
                    type = "video",
                    query = null,
                    channelId = null,
                    relatedToVideoId = videoId,
                    order = "relevance",
                    maxResults = 15,
                    pageToken = null,
                    regionCode = null,
                    videoCategoryId = null,
                    safeSearch = "moderate",
                    apiKey = network.apiKey()
                )
                val items = response.items.mapNotNull { Mappers.searchItemToDomain(it) }
                AppResult.Success(enrichVideos(items).filterIsInstance<SearchResultItem.VideoItem>().map { it.video })
            }
        }

    suspend fun channelDetails(channelId: String): AppResult<Channel> = cached("channel:$channelId") {
        apiCall {
            val response = network.api().channels(
                part = "snippet,statistics",
                ids = channelId,
                apiKey = network.apiKey()
            )
            val channel = response.items.firstOrNull()?.let { Mappers.channelToDomain(it) }
                ?: return@apiCall AppResult.Failure(AppError.ApiFailure(404, "Chaîne introuvable."))
            AppResult.Success(channel)
        }
    }

    suspend fun channelVideos(channelId: String): AppResult<List<Video>> =
        cached("channelVideos:$channelId") {
            apiCall {
                val response = network.api().search(
                    part = "snippet",
                    type = "video",
                    query = null,
                    channelId = channelId,
                    relatedToVideoId = null,
                    order = "date",
                    maxResults = PAGE_SIZE,
                    pageToken = null,
                    regionCode = null,
                    videoCategoryId = null,
                    safeSearch = "moderate",
                    apiKey = network.apiKey()
                )
                val items = response.items.mapNotNull { Mappers.searchItemToDomain(it) }
                AppResult.Success(
                    enrichVideos(items).filterIsInstance<SearchResultItem.VideoItem>().map { it.video }
                )
            }
        }

    // -----------------------------------------------------------------------
    // Interne
    // -----------------------------------------------------------------------

    /** Région utilisée pour les tendances (réglage utilisateur). */
    private fun regionCode(): String? = regionProvider()?.takeIf { it.isNotBlank() }

    /**
     * Ajoute les métadonnées manquantes (durée, vues) aux résultats de recherche.
     * Un seul appel `videos.list` couvre jusqu'à 50 identifiants (1 unité de quota).
     */
    private suspend fun enrichVideos(items: List<SearchResultItem>): List<SearchResultItem> {
        val videoIds = items.filterIsInstance<SearchResultItem.VideoItem>().map { it.video.id }
        if (videoIds.isEmpty()) return items
        val details = runCatching {
            network.api().videos(
                part = "contentDetails,statistics,liveStreamingDetails",
                ids = videoIds.take(50).joinToString(","),
                apiKey = network.apiKey()
            ).items.mapNotNull { Mappers.videoToDomain(it) }.associateBy { it.id }
        }.getOrDefault(emptyMap())
        if (details.isEmpty()) return items
        return items.map { item ->
            if (item is SearchResultItem.VideoItem) {
                val detail = details[item.video.id]
                if (detail != null) {
                    item.copy(
                        video = item.video.copy(
                            durationSeconds = detail.durationSeconds,
                            viewCount = detail.viewCount,
                            likeCount = detail.likeCount,
                            isLive = detail.isLive,
                            publishedAt = item.video.publishedAt.ifBlank { detail.publishedAt }
                        )
                    )
                } else {
                    item
                }
            } else {
                item
            }
        }
    }

    private suspend fun <T> apiCall(block: suspend () -> AppResult<T>): AppResult<T> = withContext(ioDispatcher) {
        if (!network.isProxyMode() && network.apiKey().isNullOrBlank()) {
            return@withContext AppResult.Failure(AppError.MissingApiKey)
        }
        try {
            block()
        } catch (throwable: Exception) {
            AppResult.Failure(ApiErrorMapper.map(throwable))
        }
    }

    /** Cache mémoire TTL : les recherches identiques ne consomment pas de quota. */
    private suspend fun <T> cached(key: String, block: suspend () -> AppResult<T>): AppResult<T> {
        val hit = memoryCache[key]
        if (hit != null && hit.expiresAt > System.currentTimeMillis()) {
            @Suppress("UNCHECKED_CAST")
            return hit.value as AppResult<T>
        }
        val result = block()
        if (result is AppResult.Success) {
            memoryCache[key] = CacheEntry(result, System.currentTimeMillis() + CACHE_TTL_MS)
        }
        return result
    }

    fun clearCache() = memoryCache.clear()

    companion object {
        private const val PAGE_SIZE = 25
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
