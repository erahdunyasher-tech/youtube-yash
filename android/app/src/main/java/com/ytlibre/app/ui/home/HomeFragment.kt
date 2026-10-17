package com.ytlibre.app.ui.home

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
import androidx.recyclerview.widget.GridLayoutManager
import com.ytlibre.app.R
import com.ytlibre.app.YouTubeLibreApp
import com.ytlibre.app.data.model.ContentCategory
import com.ytlibre.app.data.model.Video
import com.ytlibre.app.databinding.FragmentHomeBinding
import com.ytlibre.app.ui.common.UiState
import com.ytlibre.app.ui.watch.WatchFragment
import kotlinx.coroutines.launch

/**
 * Accueil : tendances YouTube filtrables par catégorie.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        (requireActivity().application as YouTubeLibreApp).container.viewModelFactory
    }

    private lateinit var adapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = VideoAdapter(R.layout.item_video_grid, onClick = ::openVideo)
        binding.recyclerVideos.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerVideos.adapter = adapter
        binding.recyclerVideos.setHasFixedSize(true)

        binding.swipeRefresh.setOnRefreshListener { viewModel.load() }
        binding.stateRetry.setOnClickListener { viewModel.load() }
        binding.chipGroupCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chipMusic -> ContentCategory.MUSIC
                R.id.chipGames -> ContentCategory.GAMES
                R.id.chipMovies -> ContentCategory.FILMS
                R.id.chipNews -> ContentCategory.NEWS
                R.id.chipSports -> ContentCategory.SPORTS
                R.id.chipLearning -> ContentCategory.LEARNING
                else -> ContentCategory.ALL
            }
            viewModel.selectCategory(category)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }

        if (savedInstanceState == null) viewModel.load()
    }

    private fun render(state: UiState<List<Video>>) {
        binding.swipeRefresh.isRefreshing = false
        when (state) {
            is UiState.Idle -> Unit
            is UiState.Loading -> {
                binding.progress.visibility = View.VISIBLE
                binding.stateGroup.visibility = View.GONE
            }

            is UiState.Success -> {
                binding.progress.visibility = View.GONE
                binding.stateGroup.visibility = View.GONE
                binding.recyclerVideos.visibility = View.VISIBLE
                adapter.submitList(state.data)
                if (state.data.isEmpty()) showState(
                    title = getString(R.string.state_empty_title),
                    message = getString(R.string.state_empty_message)
                )
            }

            is UiState.Error -> {
                binding.progress.visibility = View.GONE
                showState(
                    title = getString(R.string.state_error_title),
                    message = state.error.userMessage
                )
            }
        }
    }

    private fun showState(title: String, message: String) {
        binding.stateGroup.visibility = View.VISIBLE
        binding.recyclerVideos.visibility = View.GONE
        binding.stateTitle.text = title
        binding.stateMessage.text = message
        binding.stateRetry.visibility = View.VISIBLE
    }

    private fun openVideo(video: Video) {
        findNavController().navigate(
            R.id.action_home_to_watch,
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
}
