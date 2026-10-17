package com.ytlibre.app.ui.library

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
import com.ytlibre.app.databinding.FragmentLibraryBinding
import com.ytlibre.app.ui.common.VideoAdapter
import com.ytlibre.app.ui.watch.WatchFragment
import kotlinx.coroutines.launch

/**
 * Bibliothèque locale : favoris et historique d'écoute.
 */
class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LibraryViewModel by viewModels {
        (requireActivity().application as YouTubeLibreApp).container.viewModelFactory
    }

    private lateinit var adapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = VideoAdapter(R.layout.item_video, onClick = ::openVideo)
        binding.recyclerLibrary.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerLibrary.adapter = adapter

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.selectTab(
                if (checkedId == R.id.btnTabHistory) {
                    LibraryViewModel.Tab.HISTORY
                } else {
                    LibraryViewModel.Tab.FAVORITES
                }
            )
        }

        binding.btnClear.setOnClickListener {
            when (viewModel.tab.value) {
                LibraryViewModel.Tab.FAVORITES -> viewModel.clearFavorites()
                LibraryViewModel.Tab.HISTORY -> viewModel.clearHistory()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.favorites.collect {
                        if (viewModel.tab.value == LibraryViewModel.Tab.FAVORITES) render(it)
                    }
                }
                launch {
                    viewModel.history.collect {
                        if (viewModel.tab.value == LibraryViewModel.Tab.HISTORY) render(it)
                    }
                }
                launch {
                    viewModel.tab.collect { tab ->
                        val list = when (tab) {
                            LibraryViewModel.Tab.FAVORITES -> viewModel.favorites.value
                            LibraryViewModel.Tab.HISTORY -> viewModel.history.value
                        }
                        render(list)
                    }
                }
            }
        }
    }

    private fun render(videos: List<Video>) {
        adapter.submitList(videos)
        val isEmpty = videos.isEmpty()
        binding.stateGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerLibrary.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.btnClear.visibility = if (isEmpty) View.GONE else View.VISIBLE
        if (isEmpty) {
            val favorites = viewModel.tab.value == LibraryViewModel.Tab.FAVORITES
            binding.stateTitle.text = getString(
                if (favorites) R.string.library_empty_favorites else R.string.library_empty_history
            )
            binding.stateMessage.text = getString(
                if (favorites) R.string.library_empty_favorites_hint else R.string.library_empty_history_hint
            )
            binding.stateRetry.visibility = View.GONE
        }
    }

    private fun openVideo(video: Video) {
        findNavController().navigate(
            R.id.action_library_to_watch,
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
