package com.ytlibre.app.ui.channel

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
import coil.load
import com.ytlibre.app.R
import com.ytlibre.app.YouTubeLibreApp
import com.ytlibre.app.data.model.Channel
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.databinding.FragmentChannelBinding
import com.ytlibre.app.ui.common.UiState
import com.ytlibre.app.ui.common.VideoAdapter
import com.ytlibre.app.ui.watch.WatchFragment
import com.ytlibre.app.util.Formatters
import kotlinx.coroutines.launch

/**
 * Page d'une chaîne : informations publiques et dernières vidéos publiées.
 */
class ChannelFragment : Fragment() {

    private var _binding: FragmentChannelBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChannelViewModel by viewModels {
        (requireActivity().application as YouTubeLibreApp).container.viewModelFactory
    }

    private lateinit var adapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChannelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val args = requireArguments()
        val channelId = args.getString(ARG_CHANNEL_ID).orEmpty()
        binding.tvChannelName.text = args.getString(ARG_CHANNEL_TITLE).orEmpty()

        adapter = VideoAdapter(R.layout.item_video, onClick = ::openVideo)
        binding.recyclerChannelVideos.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerChannelVideos.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.channel.collect { renderChannel(it) } }
                launch { viewModel.videos.collect { renderVideos(it) } }
            }
        }

        viewModel.load(channelId)
    }

    private fun renderChannel(state: UiState<Channel>) {
        when (state) {
            is UiState.Success -> {
                val channel = state.data
                binding.tvChannelName.text = channel.title
                binding.tvChannelDescription.text = channel.description
                binding.tvSubscribers.text = Formatters.formatSubscribers(channel.subscriberCount)
                binding.ivAvatar.load(channel.thumbnailUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_thumbnail_placeholder)
                    error(R.drawable.ic_broken_image)
                }
                binding.ivBanner.load(channel.bannerUrl) {
                    crossfade(true)
                    placeholder(R.drawable.bg_thumbnail_placeholder)
                    error(R.drawable.bg_thumbnail_placeholder)
                }
            }

            is UiState.Error -> {
                binding.stateGroup.visibility = View.VISIBLE
                binding.stateTitle.text = getString(R.string.state_error_title)
                binding.stateMessage.text = state.error.userMessage
                binding.stateRetry.visibility = View.VISIBLE
                binding.stateRetry.setOnClickListener {
                    viewModel.reload(requireArguments().getString(ARG_CHANNEL_ID).orEmpty())
                }
            }

            else -> Unit
        }
    }

    private fun renderVideos(state: UiState<List<Video>>) {
        when (state) {
            is UiState.Success -> {
                adapter.submitList(state.data)
                binding.progress.visibility = View.GONE
            }

            is UiState.Error -> {
                binding.progress.visibility = View.GONE
            }

            is UiState.Loading -> binding.progress.visibility = View.VISIBLE

            else -> Unit
        }
    }

    private fun openVideo(video: Video) {
        findNavController().navigate(
            R.id.action_channel_to_watch,
            bundleOf(
                WatchFragment.ARG_VIDEO_ID to video.id,
                WatchFragment.ARG_TITLE to video.title,
                WatchFragment.ARG_CHANNEL_TITLE to video.channelTitle,
                WatchFragment.ARG_CHANNEL_ID to video.channelId
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_CHANNEL_ID = "channelId"
        const val ARG_CHANNEL_TITLE = "channelTitle"
    }
}
