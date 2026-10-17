package com.ytlibre.app.ui.common

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.ytlibre.app.R
import com.ytlibre.app.data.model.Channel
import com.ytlibre.app.data.model.SearchResultItem
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.util.Formatters

/**
 * Adaptateur de vidéos, utilisable en mode liste ou en mode grille.
 * Les deux gabarits partagent les mêmes identifiants de vues.
 */
class VideoAdapter(
    private val layoutRes: Int,
    private val onClick: (Video) -> Unit,
    private val onLongClick: ((Video) -> Unit)? = null
) : ListAdapter<Video, VideoAdapter.VideoHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoHolder {
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return VideoHolder(view)
    }

    override fun onBindViewHolder(holder: VideoHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val channel: TextView = itemView.findViewById(R.id.tvChannel)
        private val meta: TextView = itemView.findViewById(R.id.tvMeta)
        private val duration: TextView = itemView.findViewById(R.id.tvDuration)

        fun bind(video: Video) {
            title.text = video.title
            channel.text = video.channelTitle
            meta.text = buildMeta(video)
            duration.visibility = if (video.durationSeconds > 0 && !video.isLive) View.VISIBLE else View.GONE
            duration.text = Formatters.formatDuration(video.durationSeconds)
            thumbnail.load(video.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_thumbnail_placeholder)
                error(R.drawable.ic_broken_image)
            }
            itemView.setOnClickListener { onClick(video) }
            itemView.setOnLongClickListener {
                onLongClick?.invoke(video)
                onLongClick != null
            }
            itemView.contentDescription = video.title
        }

        private fun buildMeta(video: Video): String {
            val parts = mutableListOf<String>()
            if (video.viewCount > 0) parts += Formatters.formatViews(video.viewCount)
            val relative = Formatters.formatRelativeDate(video.publishedAt)
            if (relative.isNotBlank()) parts += relative
            if (video.isLive) parts += "En direct"
            return parts.joinToString(" • ")
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Video>() {
            override fun areItemsTheSame(oldItem: Video, newItem: Video) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Video, newItem: Video) = oldItem == newItem
        }
    }
}

/**
 * Adaptateur de chaînes.
 */
class ChannelAdapter(
    private val onClick: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.ChannelHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_channel, parent, false)
        return ChannelHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChannelHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val description: TextView = itemView.findViewById(R.id.tvDescription)

        fun bind(channel: Channel) {
            name.text = channel.title
            description.text = channel.description
            avatar.load(channel.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_thumbnail_placeholder)
                error(R.drawable.ic_broken_image)
            }
            itemView.setOnClickListener { onClick(channel) }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Channel>() {
            override fun areItemsTheSame(oldItem: Channel, newItem: Channel) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Channel, newItem: Channel) = oldItem == newItem
        }
    }
}

/**
 * Adaptateur de résultats de recherche : mélange vidéos et chaînes.
 */
class SearchResultAdapter(
    private val onVideoClick: (Video) -> Unit,
    private val onChannelClick: (Channel) -> Unit
) : ListAdapter<SearchResultItem, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is SearchResultItem.VideoItem -> TYPE_VIDEO
        is SearchResultItem.ChannelItem -> TYPE_CHANNEL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_CHANNEL) {
            ChannelHolder(inflater.inflate(R.layout.item_channel, parent, false))
        } else {
            VideoHolder(inflater.inflate(R.layout.item_video, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SearchResultItem.VideoItem -> (holder as VideoHolder).bind(item.video)
            is SearchResultItem.ChannelItem -> (holder as ChannelHolder).bind(item.channel)
        }
    }

    inner class VideoHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val channel: TextView = itemView.findViewById(R.id.tvChannel)
        private val meta: TextView = itemView.findViewById(R.id.tvMeta)
        private val duration: TextView = itemView.findViewById(R.id.tvDuration)

        fun bind(video: Video) {
            title.text = video.title
            channel.text = video.channelTitle
            val parts = mutableListOf<String>()
            if (video.viewCount > 0) parts += Formatters.formatViews(video.viewCount)
            val relative = Formatters.formatRelativeDate(video.publishedAt)
            if (relative.isNotBlank()) parts += relative
            meta.text = parts.joinToString(" • ")
            duration.visibility = if (video.durationSeconds > 0) View.VISIBLE else View.GONE
            duration.text = Formatters.formatDuration(video.durationSeconds)
            thumbnail.load(video.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_thumbnail_placeholder)
                error(R.drawable.ic_broken_image)
            }
            itemView.setOnClickListener { onVideoClick(video) }
        }
    }

    inner class ChannelHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val avatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val name: TextView = itemView.findViewById(R.id.tvName)
        private val description: TextView = itemView.findViewById(R.id.tvDescription)

        fun bind(channel: Channel) {
            name.text = channel.title
            description.text = channel.description
            avatar.load(channel.thumbnailUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_thumbnail_placeholder)
                error(R.drawable.ic_broken_image)
            }
            itemView.setOnClickListener { onChannelClick(channel) }
        }
    }

    companion object {
        private const val TYPE_VIDEO = 0
        private const val TYPE_CHANNEL = 1

        val DIFF = object : DiffUtil.ItemCallback<SearchResultItem>() {
            override fun areItemsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: SearchResultItem, newItem: SearchResultItem) =
                oldItem == newItem
        }
    }
}
