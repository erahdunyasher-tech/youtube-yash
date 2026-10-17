package com.ytlibre.app.data.remote

import com.ytlibre.app.data.model.Channel
import com.ytlibre.app.data.model.SearchResultItem
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.data.remote.dto.ChannelDto
import com.ytlibre.app.data.remote.dto.SearchItemDto
import com.ytlibre.app.data.remote.dto.VideoDto
import com.ytlibre.app.util.Formatters

/**
 * Conversion des DTO bruts de l'API en modèles de domaine.
 */
object Mappers {

    fun searchItemToDomain(item: SearchItemDto): SearchResultItem? {
        val snippet = item.snippet ?: return null
        val title = snippet.title.orEmpty()
        return when {
            item.id?.videoId != null -> SearchResultItem.VideoItem(
                Video(
                    id = item.id.videoId,
                    title = title,
                    description = snippet.description.orEmpty(),
                    channelId = snippet.channelId.orEmpty(),
                    channelTitle = snippet.channelTitle.orEmpty(),
                    thumbnailUrl = snippet.thumbnails?.bestUrl().orEmpty(),
                    publishedAt = snippet.publishedAt.orEmpty(),
                    isShort = looksLikeShort(title)
                )
            )

            item.id?.channelId != null -> SearchResultItem.ChannelItem(
                Channel(
                    id = item.id.channelId,
                    title = title,
                    description = snippet.description.orEmpty(),
                    thumbnailUrl = snippet.thumbnails?.bestUrl().orEmpty(),
                    publishedAt = snippet.publishedAt.orEmpty()
                )
            )

            else -> null
        }
    }

    fun videoToDomain(dto: VideoDto): Video? {
        val id = dto.id ?: return null
        val snippet = dto.snippet
        val live = dto.liveStreamingDetails
        val isLive = live?.actualStartTime != null && live.actualEndTime == null
        return Video(
            id = id,
            title = snippet?.title.orEmpty(),
            description = snippet?.description.orEmpty(),
            channelId = snippet?.channelId.orEmpty(),
            channelTitle = snippet?.channelTitle.orEmpty(),
            thumbnailUrl = snippet?.thumbnails?.bestUrl().orEmpty(),
            durationSeconds = Formatters.parseDurationSeconds(dto.contentDetails?.duration),
            viewCount = dto.statistics?.viewCount?.toLongOrNull() ?: 0L,
            likeCount = dto.statistics?.likeCount?.toLongOrNull() ?: 0L,
            publishedAt = snippet?.publishedAt.orEmpty(),
            isLive = isLive,
            isShort = looksLikeShort(snippet?.title.orEmpty())
        )
    }

    fun channelToDomain(dto: ChannelDto): Channel? {
        val id = dto.id ?: return null
        val snippet = dto.snippet
        return Channel(
            id = id,
            title = snippet?.title.orEmpty(),
            description = snippet?.description.orEmpty(),
            thumbnailUrl = snippet?.thumbnails?.bestUrl().orEmpty(),
            bannerUrl = snippet?.thumbnails?.maxres?.url.orEmpty(),
            subscriberCount = dto.statistics?.subscriberCount?.toLongOrNull() ?: 0L,
            videoCount = dto.statistics?.videoCount?.toLongOrNull() ?: 0L,
            viewCount = dto.statistics?.viewCount?.toLongOrNull() ?: 0L,
            publishedAt = snippet?.publishedAt.orEmpty()
        )
    }

    /** Détection d'un Short (format vertical) par son titre. */
    fun looksLikeShort(title: String): Boolean {
        val lowered = title.lowercase()
        return lowered.contains("#shorts") || lowered.contains("#short") ||
            lowered.contains("shorts") && lowered.contains("vertical")
    }
}
