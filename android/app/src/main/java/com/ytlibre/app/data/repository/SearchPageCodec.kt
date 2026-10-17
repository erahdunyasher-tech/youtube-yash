package com.ytlibre.app.data.repository

import com.google.gson.Gson
import com.ytlibre.app.data.model.Channel
import com.ytlibre.app.data.model.SearchPage
import com.ytlibre.app.data.model.SearchResultItem
import com.ytlibre.app.data.model.Video

/**
 * (Dé)serialisation de [SearchPage] pour le cache persistant Room.
 *
 * `SearchPage.items` est une liste d'une **interface scellée** ([SearchResultItem]).
 * À l'écriture Gson va bien, mais à la lecture il faudrait *instancier* cette
 * interface : il lève « Interfaces can't be instantiated! ». L'erreur étant
 * avalée par `runCatching`, le cache persistant semblait écrit mais n'était
 * jamais relu — chaque « cache hit » devenait en réalité un appel API en plus.
 *
 * D'où un gabarit explicite `{kind, video|channel}` : round-trip sans perte de type.
 */
internal object SearchPageCodec {

    private const val KIND_VIDEO = "video"
    private const val KIND_CHANNEL = "channel"

    private val gson = Gson()

    private data class StoredItem(
        val kind: String = "",
        val video: Video? = null,
        val channel: Channel? = null
    )

    private data class StoredPage(
        val items: List<StoredItem> = emptyList(),
        val nextPageToken: String? = null,
        val totalResults: Int = 0
    )

    fun encode(page: SearchPage): String = gson.toJson(
        StoredPage(
            items = page.items.map { item ->
                when (item) {
                    is SearchResultItem.VideoItem ->
                        StoredItem(kind = KIND_VIDEO, video = item.video)
                    is SearchResultItem.ChannelItem ->
                        StoredItem(kind = KIND_CHANNEL, channel = item.channel)
                }
            },
            nextPageToken = page.nextPageToken,
            totalResults = page.totalResults
        )
    )

    fun decode(payload: String?): SearchPage? {
        if (payload.isNullOrBlank()) return null
        return runCatching {
            val stored = gson.fromJson(payload, StoredPage::class.java) ?: return null
            SearchPage(
                items = stored.items.mapNotNull { item ->
                    when (item.kind) {
                        KIND_VIDEO -> item.video?.let { SearchResultItem.VideoItem(it) }
                        KIND_CHANNEL -> item.channel?.let { SearchResultItem.ChannelItem(it) }
                        else -> null
                    }
                },
                nextPageToken = stored.nextPageToken,
                totalResults = stored.totalResults
            )
        }.getOrNull()
    }
}
