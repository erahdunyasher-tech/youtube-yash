package com.ytlibre.app.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.ytlibre.app.R
import com.ytlibre.app.YouTubeLibreApp
import com.ytlibre.app.data.model.SearchFilter
import com.ytlibre.app.data.model.SearchResultItem
import com.ytlibre.app.databinding.FragmentSearchBinding
import com.ytlibre.app.ui.channel.ChannelFragment
import com.ytlibre.app.ui.common.SearchResultAdapter
import com.ytlibre.app.ui.common.UiState
import com.ytlibre.app.ui.watch.WatchFragment
import kotlinx.coroutines.launch

/**
 * Recherche de vidéos et de chaînes (YouTube Data API v3).
 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels {
        (requireActivity().application as YouTubeLibreApp).container.viewModelFactory
    }

    private lateinit var adapter: SearchResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SearchResultAdapter(
            onVideoClick = { video ->
                findNavController().navigate(
                    R.id.action_search_to_watch,
                    bundleOf(
                        WatchFragment.ARG_VIDEO_ID to video.id,
                        WatchFragment.ARG_TITLE to video.title,
                        WatchFragment.ARG_CHANNEL_TITLE to video.channelTitle,
                        WatchFragment.ARG_CHANNEL_ID to video.channelId
                    )
                )
            },
            onChannelClick = { channel ->
                findNavController().navigate(
                    R.id.action_search_to_channel,
                    bundleOf(
                        ChannelFragment.ARG_CHANNEL_ID to channel.id,
                        ChannelFragment.ARG_CHANNEL_TITLE to channel.title
                    )
                )
            }
        )
        binding.recyclerResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerResults.adapter = adapter
        binding.recyclerResults.addOnScrollListener(PaginationScrollListener { viewModel.loadMore() })

        binding.editSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.submitQuery(binding.editSearch.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }
        binding.btnSearch.setOnClickListener {
            viewModel.submitQuery(binding.editSearch.text?.toString().orEmpty())
        }

        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipVideos -> SearchFilter.VIDEOS
                R.id.chipChannels -> SearchFilter.CHANNELS
                else -> SearchFilter.ALL
            }
            viewModel.onFilterChanged(filter)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch { viewModel.recentQueries.collect { renderRecent(it) } }
                launch { viewModel.hasMore.collect { binding.btnLoadMore.visibility = if (it) View.VISIBLE else View.GONE } }
            }
        }
    }

    private fun render(state: UiState<List<SearchResultItem>>) {
        binding.progress.visibility =
            if (state is UiState.Loading) View.VISIBLE else View.GONE
        binding.btnLoadMore.visibility =
            if (state is UiState.Success && viewModel.hasMore.value) View.VISIBLE else View.GONE

        when (state) {
            is UiState.Idle -> {
                binding.stateGroup.visibility = View.GONE
                binding.recyclerResults.visibility = View.GONE
                binding.recentGroup.visibility = View.VISIBLE
            }

            is UiState.Loading -> {
                binding.stateGroup.visibility = View.GONE
                binding.recentGroup.visibility = View.GONE
                binding.recyclerResults.visibility = View.VISIBLE
            }

            is UiState.Success -> {
                binding.stateGroup.visibility = View.GONE
                binding.recentGroup.visibility = View.GONE
                binding.recyclerResults.visibility = View.VISIBLE
                adapter.submitList(state.data)
                if (state.data.isEmpty()) {
                    binding.stateGroup.visibility = View.VISIBLE
                    binding.stateTitle.text = getString(R.string.state_empty_title)
                    binding.stateMessage.text = getString(R.string.search_empty)
                    binding.stateRetry.visibility = View.GONE
                }
            }

            is UiState.Error -> {
                binding.stateGroup.visibility = View.VISIBLE
                binding.recentGroup.visibility = View.GONE
                binding.stateTitle.text = getString(R.string.state_error_title)
                binding.stateMessage.text = state.error.userMessage
                binding.stateRetry.visibility = View.VISIBLE
                binding.stateRetry.setOnClickListener {
                    viewModel.submitQuery(binding.editSearch.text?.toString().orEmpty())
                }
            }
        }
    }

    private fun renderRecent(queries: List<String>) {
        binding.chipGroupRecent.removeAllViews()
        binding.chipGroupRecent.visibility = if (queries.isEmpty()) View.GONE else View.VISIBLE
        queries.forEach { query ->
            val chip = Chip(requireContext())
            chip.text = query
            chip.setOnClickListener {
                binding.editSearch.setText(query)
                viewModel.submitQuery(query)
            }
            binding.chipGroupRecent.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Déclenche le chargement de la page suivante lorsque l'utilisateur approche
 * de la fin de la liste.
 */
class PaginationScrollListener(
    private val onLoadMore: () -> Unit
) : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {

    override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
        if (dy <= 0) return
        val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager ?: return
        val total = layoutManager.itemCount
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        if (lastVisible >= total - 3) onLoadMore()
    }
}
