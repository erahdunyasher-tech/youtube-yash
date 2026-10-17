package com.ytlibre.app.data.remote

import com.ytlibre.app.data.remote.dto.ChannelsListResponse
import com.ytlibre.app.data.remote.dto.SearchResponse
import com.ytlibre.app.data.remote.dto.VideosListResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * YouTube Data API v3 — surface réseau minimale.
 *
 * La clé API est passée en paramètre `key` uniquement en mode « direct ».
 * En mode « proxy », le paramètre est omis : la clé vit dans l'environnement
 * du serveur (variable d'environnement) et n'est jamais présente sur l'appareil.
 */
interface YouTubeDataApi {

    @GET("search")
    suspend fun search(
        @Query("part") part: String,
        @Query("type") type: String,
        @Query("q") query: String?,
        @Query("channelId") channelId: String?,
        @Query("relatedToVideoId") relatedToVideoId: String?,
        @Query("order") order: String?,
        @Query("maxResults") maxResults: Int,
        @Query("pageToken") pageToken: String?,
        @Query("regionCode") regionCode: String?,
        @Query("videoCategoryId") videoCategoryId: String?,
        @Query("safeSearch") safeSearch: String,
        @Query("key") apiKey: String?
    ): SearchResponse

    @GET("videos")
    suspend fun videos(
        @Query("part") part: String,
        @Query("id") ids: String,
        @Query("key") apiKey: String?
    ): VideosListResponse

    /** Tendances / catégorie (chart = mostPopular). */
    @GET("videos")
    suspend fun videosChart(
        @Query("part") part: String,
        @Query("chart") chart: String,
        @Query("videoCategoryId") videoCategoryId: String?,
        @Query("regionCode") regionCode: String?,
        @Query("maxResults") maxResults: Int,
        @Query("key") apiKey: String?
    ): VideosListResponse

    @GET("channels")
    suspend fun channels(
        @Query("part") part: String,
        @Query("id") ids: String,
        @Query("key") apiKey: String?
    ): ChannelsListResponse
}
