package com.ytlibre.app.data.repository

import com.ytlibre.app.data.local.SearchCacheDao
import com.ytlibre.app.data.local.SearchCacheEntity
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    private val regionProvider: () -> String? = { null },
    private val searchCacheDao: SearchCacheDao? = null
) {

    private data class CacheEntry<T>(val value: T, val expiresAt: Long)

    private val memoryCache = ConcurrentHashMap<String, CacheEntry<Any>>()

    // -----------------------------------------------------------------------
    // API publique
    // -----------------------------------------------------------------------

    /**
     * Recherche (vidéos / chaînes).
     *
     * Optimisations quota, par ordre de priorité :
     *  1. cache persistant (Room) : une page 1 déjà obtenue est resservie
     *     indéfiniment sans requête réseau — y compris hors ligne ;
     *  2. cache mémoire TTL 5 min ;
     *  3. appel API réel (100 unités) + enrichissement via `videos.list` (1 unité).
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter.ALL,
        pageToken: String? = null
    ): AppResult<SearchPage> {
        val cacheKey = "search:$query:${filter.name}"
        if (pageToken == null) {
            readPersistedPage(cacheKey)?.let { (page, createdAt) ->
                return AppResult.Success(page.copy(cachedAt = createdAt))
            }
        }
        return cached("$cacheKey:${pageToken?.hashCode() ?: "first"}") {
            val result = apiSearch(query, filter, pageToken)
            if (pageToken == null && result is AppResult.Success) {
                persistPage(cacheKey, result.data)
            }
            result
        }
    }

    private suspend fun apiSearch(
        query: String,
        filter: SearchFilter,
        pageToken: String?
    ): AppResult<SearchPage> = apiCall {
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

    /** Page 1 en cache persistant, même hors ligne (repli quota / réseau). */
    suspend fun cachedSearchPage(
        query: String,
        filter: SearchFilter = SearchFilter.ALL
    ): Pair<SearchPage, Long>? = readPersistedPage("search:$query:${filter.name}")
        ?.takeIf { it.first.items.isNotEmpty() }

    private suspend fun readPersistedPage(cacheKey: String): Pair<SearchPage, Long>? {
        val dao = searchCacheDao ?: return null
        val entity = runCatching { dao.get(cacheKey) }.getOrNull() ?: return null
        // SearchPageCodec (et non Gson brut) : `items` est une interface scellée
        // que Gson sait écrire mais pas relire (« Interfaces can't be instantiated! »).
        val page = SearchPageCodec.decode(entity.payload) ?: return null
        return page to entity.createdAt
    }

    private suspend fun persistPage(cacheKey: String, page: SearchPage) {
        val dao = searchCacheDao ?: return
        runCatching {
            dao.put(SearchCacheEntity(cacheKey, SearchPageCodec.encode(page), System.currentTimeMillis()))
            dao.prune(MAX_SEARCH_CACHE_ENTRIES)
        }
    }

    suspend fun trending(category: ContentCategory = ContentCategory.ALL): AppResult<List<Video>> =
        cached("trending:${category.name}:${network.baseUrl()}", ttlMs = TRENDING_TTL_MS) {
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

    /** Cache mémoire TTL : les requêtes identiques ne consomment pas de quota. */
    private suspend fun <T> cached(
        key: String,
        ttlMs: Long = CACHE_TTL_MS,
        block: suspend () -> AppResult<T>
    ): AppResult<T> {
        val hit = memoryCache[key]
        if (hit != null && hit.expiresAt > System.currentTimeMillis()) {
            @Suppress("UNCHECKED_CAST")
            return hit.value as AppResult<T>
        }
        val result = block()
        if (result is AppResult.Success) {
            memoryCache[key] = CacheEntry(result, System.currentTimeMillis() + ttlMs)
        }
        return result
    }

    fun clearCache() {
        memoryCache.clear()
        val dao = searchCacheDao ?: return
        // Appelé depuis des contextes non-suspend (Réglages) : on nettoie le
        // cache persistant de façon asynchrone sur Dispatchers.IO.
        CoroutineScope(ioDispatcher).launch { runCatching { dao.clear() } }
    }

    companion object {
        private const val PAGE_SIZE = 25
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
        /** Les tendances évoluent lentement : cache 60 min (1 unité / heure max par catégorie). */
        private const val TRENDING_TTL_MS = 60 * 60 * 1000L
        private const val MAX_SEARCH_CACHE_ENTRIES = 100
    }
}
