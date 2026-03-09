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
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentNapsListBinding
import net.poopyfeed.pf.ui.common.PagingLoadStateAdapter

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

    val childId = requireArguments().getInt("childId")
    adapter =
        NapAdapter(
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
            ?.getStateFlow("nap_created", false)
            ?.collect { created ->
              if (created) {
                adapter.refresh()
                binding.recyclerNaps.scrollToPosition(0)
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("nap_created", false)
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
            ?.getStateFlow("nap_updated", false)
            ?.collect { updated ->
              if (updated) {
                adapter.refresh()
                binding.recyclerNaps.scrollToPosition(0)
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("nap_updated", false)
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
