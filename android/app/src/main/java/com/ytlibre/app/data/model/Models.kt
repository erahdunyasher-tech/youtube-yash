package com.ytlibre.app.data.model

/**
 * Modèle de domaine : une vidéo YouTube telle que consommée par l'interface.
 * Aucune donnée de flux vidéo n'est stockée : uniquement les métadonnées
 * publiques exposées par la YouTube Data API v3.
 */
data class Video(
    val id: String,
    val title: String,
    val description: String = "",
    val channelId: String = "",
    val channelTitle: String = "",
    val thumbnailUrl: String = "",
    val durationSeconds: Int = 0,
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val publishedAt: String = "",
    val isLive: Boolean = false,
    val isShort: Boolean = false
) {
    val isShorts: Boolean get() = isShort
}

data class Channel(
    val id: String,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String = "",
    val bannerUrl: String = "",
    val subscriberCount: Long = 0L,
    val videoCount: Long = 0L,
    val viewCount: Long = 0L,
    val publishedAt: String = ""
)

/** Résultat unifié d'une recherche (vidéos et/ou chaînes). */
sealed interface SearchResultItem {
    val id: String

    data class VideoItem(val video: Video) : SearchResultItem {
        override val id: String get() = video.id
    }

    data class ChannelItem(val channel: Channel) : SearchResultItem {
        override val id: String get() = channel.id
    }
}

data class SearchPage(
    val items: List<SearchResultItem> = emptyList(),
    val nextPageToken: String? = null,
    val totalResults: Int = 0
)

enum class SearchFilter(val apiType: String) {
    ALL("video,channel"),
    VIDEOS("video"),
    CHANNELS("channel")
}

enum class ContentCategory(val apiId: String, val label: String) {
    ALL("", "Tout"),
    MUSIC("10", "Musique"),
    GAMES("20", "Jeux"),
    FILMS("1", "Films"),
    NEWS("25", "Actualités"),
    SPORTS("17", "Sports"),
    LEARNING("27", "Apprentissage")
}
