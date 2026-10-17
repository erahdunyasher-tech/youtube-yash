package com.ytlibre.app.ui.watch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ytlibre.app.R
import com.ytlibre.app.YouTubeLibreApp
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.databinding.FragmentWatchBinding
import com.ytlibre.app.ui.channel.ChannelFragment
import com.ytlibre.app.ui.common.UiState
import com.ytlibre.app.ui.common.VideoAdapter
import com.ytlibre.app.ui.main.MainActivity
import com.ytlibre.app.util.Formatters
import kotlinx.coroutines.launch

/**
 * Écran de lecture.
 *
 * La vidéo est lue par le lecteur officiel YouTube embarqué dans une WebView.
 * L'application n'extrait aucun flux, ne contourne aucune publicité et ne
 * télécharge aucun contenu : elle respecte les règles de diffusion de YouTube.
 */
class WatchFragment : Fragment() {

    private var _binding: FragmentWatchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WatchViewModel by viewModels {
        (requireActivity().application as YouTubeLibreApp).container.viewModelFactory
    }

    private lateinit var relatedAdapter: VideoAdapter
    private var videoId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        videoId = args.getString(ARG_VIDEO_ID).orEmpty()

        binding.tvTitle.text = args.getString(ARG_TITLE).orEmpty()
        binding.tvChannel.text = args.getString(ARG_CHANNEL_TITLE).orEmpty()

        relatedAdapter = VideoAdapter(R.layout.item_video, onClick = ::openRelatedVideo)
        binding.recyclerRelated.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerRelated.adapter = relatedAdapter
        binding.recyclerRelated.isNestedScrollingEnabled = false

        binding.playerView.listener = object : PlayerWebView.Listener {
            override fun onPlayerReady() = Unit
            override fun onPlayingChanged(isPlaying: Boolean) = Unit
            override fun onPlayerError(message: String) {
                binding.tvPlayerError.visibility = View.VISIBLE
                binding.tvPlayerError.text = message
            }
        }
        binding.playerView.loadVideo(videoId, autoplay = true)

        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite() }
        binding.btnPip.setOnClickListener {
            (requireActivity() as? MainActivity)?.enterPlayerPictureInPicture()
        }
        binding.btnShare.setOnClickListener { shareVideo() }
        binding.btnOpenYouTube.setOnClickListener { openInYouTube() }
        binding.tvChannel.setOnClickListener { openChannel() }
        binding.btnDescription.setOnClickListener {
            binding.tvDescription.visibility =
                if (binding.tvDescription.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.video.collect { renderVideo(it) } }
                launch { viewModel.related.collect { renderRelated(it) } }
                launch {
                    viewModel.isFavorite.collect { favorite ->
                        binding.btnFavorite.setIconResource(
                            if (favorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
                        )
                        binding.btnFavorite.contentDescription = getString(
                            if (favorite) R.string.action_remove_favorite else R.string.action_add_favorite
                        )
                    }
                }
            }
        }

        viewModel.load(videoId)
    }

    private fun renderVideo(state: UiState<Video>) {
        when (state) {
            is UiState.Success -> {
                val video = state.data
                binding.tvTitle.text = video.title
                binding.tvChannel.text = video.channelTitle
                binding.tvMeta.text = listOfNotNull(
                    Formatters.formatViews(video.viewCount).takeIf { video.viewCount > 0 },
                    Formatters.formatRelativeDate(video.publishedAt).takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                binding.tvDescription.text = video.description
                binding.tvDescription.visibility =
                    if (video.description.isBlank()) View.GONE else View.VISIBLE
            }

            is UiState.Error -> {
                binding.tvPlayerError.visibility = View.VISIBLE
                binding.tvPlayerError.text = state.error.userMessage
            }

            else -> Unit
        }
    }

    private fun renderRelated(state: UiState<List<Video>>) {
        when (state) {
            is UiState.Success -> {
                binding.groupRelated.visibility =
                    if (state.data.isEmpty()) View.GONE else View.VISIBLE
                relatedAdapter.submitList(state.data)
            }

            is UiState.Error -> {
                binding.groupRelated.visibility = View.GONE
            }

            else -> Unit
        }
    }

    private fun shareVideo() {
        val text = getString(R.string.share_text, "https://www.youtube.com/watch?v=$videoId")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, binding.tvTitle.text.toString())
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.action_share)))
    }

    private fun openInYouTube() {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")))
        }
    }

    private fun openChannel() {
        val channelId = (viewModel.video.value as? UiState.Success)?.data?.channelId
        if (channelId.isNullOrBlank()) return
        findNavController().navigate(
            R.id.action_watch_to_channel,
            bundleOf(
                ChannelFragment.ARG_CHANNEL_ID to channelId,
                ChannelFragment.ARG_CHANNEL_TITLE to binding.tvChannel.text.toString()
            )
        )
    }

    private fun openRelatedVideo(video: Video) {
        findNavController().navigate(
            R.id.action_watch_to_watch,
            bundleOf(
                ARG_VIDEO_ID to video.id,
                ARG_TITLE to video.title,
                ARG_CHANNEL_TITLE to video.channelTitle,
                ARG_CHANNEL_ID to video.channelId
            )
        )
    }

    override fun onPause() {
        super.onPause()
        // Le lecteur officiel continue ou non selon ses propres règles : on ne
        // force aucune lecture en arrière-plan (non pris en charge officiellement).
        if (!requireActivity().isInPictureInPictureMode) {
            binding.playerView.pause()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.playerView.release()
        _binding = null
    }

    companion object {
        const val ARG_VIDEO_ID = "videoId"
        const val ARG_TITLE = "title"
        const val ARG_CHANNEL_TITLE = "channelTitle"
        const val ARG_CHANNEL_ID = "channelId"
    }
}
