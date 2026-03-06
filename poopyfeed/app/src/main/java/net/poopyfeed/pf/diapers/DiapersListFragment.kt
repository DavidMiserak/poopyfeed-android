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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentDiapersListBinding

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

    adapter = DiaperAdapter { diaper -> showDeleteConfirmationDialog(diaper.id) }
    binding.recyclerDiapers.adapter = adapter
    binding.recyclerDiapers.layoutManager = LinearLayoutManager(requireContext())

    binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    binding.layoutErrorState.findViewById<View>(R.id.button_retry).setOnClickListener {
      viewModel.refresh()
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is DiapersListUiState.Loading -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.recyclerDiapers.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.swipeRefresh.isRefreshing = true
            }
            is DiapersListUiState.Ready -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerDiapers.visibility = View.VISIBLE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              adapter.submitList(state.diapers)
            }
            is DiapersListUiState.Empty -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerDiapers.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.VISIBLE
              binding.layoutErrorState.visibility = View.GONE
            }
            is DiapersListUiState.Error -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerDiapers.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.VISIBLE
              binding.layoutErrorState
                  .findViewById<android.widget.TextView>(R.id.text_error_message)
                  .text = state.message
            }
          }
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.isRefreshing.collect { refreshing ->
          binding.swipeRefresh.isRefreshing =
              refreshing || viewModel.uiState.value is DiapersListUiState.Loading
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.deleteError.collect { message ->
          Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow("diaper_created", false)
            ?.collect { created ->
              if (created) {
                viewModel.refresh()
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("diaper_created", false)
              }
            }
      }
    }
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
