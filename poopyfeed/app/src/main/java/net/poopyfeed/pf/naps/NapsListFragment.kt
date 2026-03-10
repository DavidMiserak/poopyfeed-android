package net.poopyfeed.pf.naps

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
import javax.inject.Inject
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentNapsListBinding
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.ui.common.PagingLoadStateAdapter
import net.poopyfeed.pf.util.logScreenView

/**
 * Displays a list of naps for a child with pull-to-refresh. FAB opens create nap bottom sheet.
 * Long-press shows delete confirmation. "End Nap" button ends an in-progress nap.
 */
@AndroidEntryPoint
class NapsListFragment : Fragment() {

  private var _binding: FragmentNapsListBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: NapsListViewModel by viewModels()
  @Inject lateinit var tokenManager: TokenManager
  private lateinit var adapter: NapAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentNapsListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    logScreenView(viewModel.analyticsTracker, "NapsList")

    val childId = requireArguments().getInt("childId")
    adapter =
        NapAdapter(
            profileTimezoneId = tokenManager.getProfileTimezone(),
            onItemClick = { nap -> navigateToEditNap(childId, nap.id) },
            onDeleteClick = { nap -> showDeleteConfirmationDialog(nap.id) },
            onEndNapClick = { nap -> viewModel.endNap(nap.id) },
        )
    binding.recyclerNaps.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerNaps.adapter =
        adapter.withLoadStateFooter(footer = PagingLoadStateAdapter { adapter.retry() })

    binding.swipeRefresh.setOnRefreshListener { adapter.refresh() }

    // Collect paging data
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.pagingData.collect { pagingData -> adapter.submitData(pagingData) }
      }
    }

    // Handle load states (loading spinner and state overlays)
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        adapter.loadStateFlow.collect { loadStates ->
          // Show/hide refresh spinner on initial load
          binding.swipeRefresh.isRefreshing =
              loadStates.refresh is LoadState.Loading && adapter.itemCount == 0

          // Manage center loading spinner and state overlays
          when {
            // Initial load - show center spinner
            loadStates.refresh is LoadState.Loading && adapter.itemCount == 0 -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
            }
            // Error during initial load - show error
            loadStates.refresh is LoadState.Error && adapter.itemCount == 0 -> {
              binding.progressLoading.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.VISIBLE
              (loadStates.refresh as? LoadState.Error)?.error?.localizedMessage?.let {
                binding.textErrorMessage.text = it
              }
            }
            // Data loaded - show list
            loadStates.refresh is LoadState.NotLoading && adapter.itemCount > 0 -> {
              binding.progressLoading.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
            }
            // Empty result - show empty state
            loadStates.refresh is LoadState.NotLoading && adapter.itemCount == 0 -> {
              binding.progressLoading.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.VISIBLE
              binding.layoutErrorState.visibility = View.GONE
            }
          }
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

    // Handle post-creation refresh (one-shot: clear flag after consuming)
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        val backStackEntry = findNavController().currentBackStackEntry ?: return@repeatOnLifecycle
        val savedStateHandle = backStackEntry.savedStateHandle
        savedStateHandle.getStateFlow("nap_created", false).collect { created ->
          if (created) {
            adapter.refresh()
            binding.recyclerNaps.scrollToPosition(0)
            // Clear flag immediately to prevent duplicate refreshes on config change
            savedStateHandle.set("nap_created", false)
          }
        }
      }
    }

    // Handle post-update refresh (one-shot: clear flag after consuming)
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        val backStackEntry = findNavController().currentBackStackEntry ?: return@repeatOnLifecycle
        val savedStateHandle = backStackEntry.savedStateHandle
        savedStateHandle.getStateFlow("nap_updated", false).collect { updated ->
          if (updated) {
            adapter.refresh()
            binding.recyclerNaps.scrollToPosition(0)
            // Clear flag immediately to prevent duplicate refreshes on config change
            savedStateHandle.set("nap_updated", false)
          }
        }
      }
    }
  }

  private fun navigateToEditNap(childId: Int, napId: Int) {
    findNavController()
        .navigate(
            R.id.action_napsList_to_editNap,
            Bundle().apply {
              putInt("childId", childId)
              putInt("napId", napId)
            },
        )
  }

  private fun showDeleteConfirmationDialog(napId: Int) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.naps_delete_confirm_title)
        .setMessage(R.string.naps_delete_confirm_message)
        .setPositiveButton(R.string.naps_delete_confirm) { _, _ -> viewModel.deleteNap(napId) }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
