package net.poopyfeed.pf.feedings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentFeedingsListBinding
import net.poopyfeed.pf.ui.common.PagingLoadStateAdapter

/**
 * Displays a list of feedings for a child with pull-to-refresh. FAB opens create feeding bottom
 * sheet. Long-press on an item shows delete confirmation.
 */
@AndroidEntryPoint
class FeedingsListFragment : Fragment() {

  private var _binding: FragmentFeedingsListBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: FeedingsListViewModel by viewModels()
  private lateinit var adapter: FeedingAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentFeedingsListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val childId = requireArguments().getInt("childId")
    adapter =
        FeedingAdapter(
            onItemClick = { feeding -> navigateToEditFeeding(childId, feeding.id) },
            onDeleteClick = { feeding -> showDeleteConfirmationDialog(feeding.id) },
        )
    binding.recyclerFeedings.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerFeedings.adapter = adapter.withLoadStateFooter(
        footer = PagingLoadStateAdapter { adapter.retry() }
    )

    binding.swipeRefresh.setOnRefreshListener { adapter.refresh() }

    // Collect paging data
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.pagingData.collect { pagingData ->
          adapter.submitData(pagingData)
        }
      }
    }

    // Handle load states (loading spinner)
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        adapter.loadStateFlow.collect { loadStates ->
          // Show/hide refresh spinner on initial load
          binding.swipeRefresh.isRefreshing =
              loadStates.refresh is LoadState.Loading && adapter.itemCount == 0
        }
      }
    }

    // Handle delete errors
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.deleteError.collect { message ->
          if (message != null) {
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
          }
        }
      }
    }

    // Handle post-creation refresh
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow("feeding_created", false)
            ?.collect { created ->
              if (created) {
                adapter.refresh()
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("feeding_created", false)
              }
            }
      }
    }

    // Handle post-update refresh
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow("feeding_updated", false)
            ?.collect { updated ->
              if (updated) {
                adapter.refresh()
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("feeding_updated", false)
              }
            }
      }
    }
  }

  private fun navigateToEditFeeding(childId: Int, feedingId: Int) {
    findNavController()
        .navigate(
            R.id.action_feedingsList_to_editFeeding,
            Bundle().apply {
              putInt("childId", childId)
              putInt("feedingId", feedingId)
            },
        )
  }

  private fun showDeleteConfirmationDialog(feedingId: Int) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.feedings_delete_confirm_title)
        .setMessage(R.string.feedings_delete_confirm_message)
        .setPositiveButton(R.string.feedings_delete_confirm) { _, _ ->
          viewModel.deleteFeeding(feedingId)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
