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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentFeedingsListBinding

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

    adapter = FeedingAdapter { feeding -> showDeleteConfirmationDialog(feeding.id) }
    binding.recyclerFeedings.adapter = adapter
    binding.recyclerFeedings.layoutManager = LinearLayoutManager(requireContext())

    binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    binding.layoutErrorState.findViewById<View>(R.id.button_retry).setOnClickListener {
      viewModel.refresh()
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is FeedingsListUiState.Loading -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.recyclerFeedings.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.swipeRefresh.isRefreshing = true
            }
            is FeedingsListUiState.Ready -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerFeedings.visibility = View.VISIBLE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              adapter.submitList(state.feedings)
            }
            is FeedingsListUiState.Empty -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerFeedings.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.VISIBLE
              binding.layoutErrorState.visibility = View.GONE
            }
            is FeedingsListUiState.Error -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerFeedings.visibility = View.GONE
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
              refreshing || viewModel.uiState.value is FeedingsListUiState.Loading
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
            ?.getStateFlow("feeding_created", false)
            ?.collect { created ->
              if (created) {
                viewModel.refresh()
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("feeding_created", false)
              }
            }
      }
    }
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
