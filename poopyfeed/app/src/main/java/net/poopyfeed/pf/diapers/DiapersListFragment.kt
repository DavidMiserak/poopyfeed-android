package net.poopyfeed.pf.diapers

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
import net.poopyfeed.pf.databinding.FragmentDiapersListBinding
import net.poopyfeed.pf.ui.common.PagingLoadStateAdapter

/**
 * Displays a list of diaper changes for a child with pull-to-refresh. FAB opens create diaper
 * bottom sheet. Long-press on an item shows delete confirmation.
 */
@AndroidEntryPoint
class DiapersListFragment : Fragment() {

  private var _binding: FragmentDiapersListBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: DiapersListViewModel by viewModels()
  private lateinit var adapter: DiaperAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentDiapersListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val childId = requireArguments().getInt("childId")
    adapter =
        DiaperAdapter(
            onItemClick = { diaper -> navigateToEditDiaper(childId, diaper.id) },
            onDeleteClick = { diaper -> showDeleteConfirmationDialog(diaper.id) },
        )
    binding.recyclerDiapers.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerDiapers.adapter = adapter.withLoadStateFooter(
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
            ?.getStateFlow("diaper_created", false)
            ?.collect { created ->
              if (created) {
                adapter.refresh()
                binding.recyclerDiapers.scrollToPosition(0)
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("diaper_created", false)
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
            ?.getStateFlow("diaper_updated", false)
            ?.collect { updated ->
              if (updated) {
                adapter.refresh()
                binding.recyclerDiapers.scrollToPosition(0)
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("diaper_updated", false)
              }
            }
      }
    }
  }

  private fun navigateToEditDiaper(childId: Int, diaperId: Int) {
    findNavController()
        .navigate(
            R.id.action_diapersList_to_editDiaper,
            Bundle().apply {
              putInt("childId", childId)
              putInt("diaperId", diaperId)
            },
        )
  }

  private fun showDeleteConfirmationDialog(diaperId: Int) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.diapers_delete_confirm_title)
        .setMessage(R.string.diapers_delete_confirm_message)
        .setPositiveButton(R.string.diapers_delete_confirm) { _, _ ->
          viewModel.deleteDiaper(diaperId)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
