package com.ytlibre.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------------------------
// Objets bruts renvoyés par la YouTube Data API v3.
// ---------------------------------------------------------------------------

data class SearchResponse(
    @SerializedName("nextPageToken") val nextPageToken: String? = null,
    @SerializedName("pageInfo") val pageInfo: PageInfoDto? = null,
    @SerializedName("items") val items: List<SearchItemDto> = emptyList()
)

data class PageInfoDto(
    @SerializedName("totalResults") val totalResults: Int = 0,
    @SerializedName("resultsPerPage") val resultsPerPage: Int = 0
)

data class SearchItemDto(
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("id") val id: IdDto? = null,
    @SerializedName("snippet") val snippet: SnippetDto? = null
)

data class IdDto(
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("videoId") val videoId: String? = null,
    @SerializedName("channelId") val channelId: String? = null
)

data class SnippetDto(
    @SerializedName("title") val title: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("channelTitle") val channelTitle: String? = null,
    @SerializedName("channelId") val channelId: String? = null,
    @SerializedName("publishedAt") val publishedAt: String? = null,
    @SerializedName("thumbnails") val thumbnails: ThumbnailsDto? = null
)

data class ThumbnailsDto(
    @SerializedName("default") val default: ThumbnailDto? = null,
    @SerializedName("medium") val medium: ThumbnailDto? = null,
    @SerializedName("high") val high: ThumbnailDto? = null,
    @SerializedName("standard") val standard: ThumbnailDto? = null,
    @SerializedName("maxres") val maxres: ThumbnailDto? = null
) {
    fun bestUrl(): String? =
        maxres?.url ?: standard?.url ?: high?.url ?: medium?.url ?: default?.url
}

data class VideosListResponse(
    @SerializedName("items") val items: List<VideoDto> = emptyList()
)

data class VideoDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("snippet") val snippet: SnippetDto? = null,
    @SerializedName("contentDetails") val contentDetails: ContentDetailsDto? = null,
    @SerializedName("statistics") val statistics: StatisticsDto? = null,
    @SerializedName("liveStreamingDetails") val liveStreamingDetails: LiveStreamingDetailsDto? = null
)

data class ContentDetailsDto(
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("definition") val definition: String? = null
)

data class StatisticsDto(
    @SerializedName("viewCount") val viewCount: String? = null,
    @SerializedName("likeCount") val likeCount: String? = null,
    @SerializedName("favoriteCount") val favoriteCount: String? = null
)

data class LiveStreamingDetailsDto(
    @SerializedName("actualEndTime") val actualEndTime: String? = null,
    @SerializedName("actualStartTime") val actualStartTime: String? = null,
    @SerializedName("concurrentViewers") val concurrentViewers: String? = null
)

data class ChannelsListResponse(
    @SerializedName("items") val items: List<ChannelDto> = emptyList()
)

data class ChannelDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("snippet") val snippet: SnippetDto? = null,
    @SerializedName("contentDetails") val contentDetails: ChannelContentDetailsDto? = null,
    @SerializedName("statistics") val statistics: ChannelStatisticsDto? = null
)

data class ChannelContentDetailsDto(
    @SerializedName("relatedPlaylists") val relatedPlaylists: RelatedPlaylistsDto? = null
)

data class RelatedPlaylistsDto(
    @SerializedName("uploads") val uploads: String? = null
)

data class ChannelStatisticsDto(
    @SerializedName("subscriberCount") val subscriberCount: String? = null,
    @SerializedName("videoCount") val videoCount: String? = null,
    @SerializedName("viewCount") val viewCount: String? = null
)

/** Format d'erreur renvoyé par les API Google. */
data class ApiErrorEnvelope(
    @SerializedName("error") val error: ApiErrorBody? = null
)

data class ApiErrorBody(
    @SerializedName("code") val code: Int? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("errors") val errors: List<ApiErrorReason>? = null
)

data class ApiErrorReason(
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("domain") val domain: String? = null
)
