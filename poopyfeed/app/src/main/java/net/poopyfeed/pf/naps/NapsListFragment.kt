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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentNapsListBinding

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

    adapter =
        NapAdapter(
            onDeleteClick = { nap -> showDeleteConfirmationDialog(nap.id) },
            onEndNapClick = { nap -> viewModel.endNap(nap.id) },
        )
    binding.recyclerNaps.adapter = adapter
    binding.recyclerNaps.layoutManager = LinearLayoutManager(requireContext())

    binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    binding.layoutErrorState.findViewById<View>(R.id.button_retry).setOnClickListener {
      viewModel.refresh()
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is NapsListUiState.Loading -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.recyclerNaps.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.swipeRefresh.isRefreshing = true
            }
            is NapsListUiState.Ready -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerNaps.visibility = View.VISIBLE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              adapter.submitList(state.naps)
            }
            is NapsListUiState.Empty -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerNaps.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.VISIBLE
              binding.layoutErrorState.visibility = View.GONE
            }
            is NapsListUiState.Error -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerNaps.visibility = View.GONE
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
              refreshing || viewModel.uiState.value is NapsListUiState.Loading
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
            ?.getStateFlow("nap_created", false)
            ?.collect { created ->
              if (created) {
                viewModel.refresh()
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("nap_created", false)
              }
            }
      }
    }
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
