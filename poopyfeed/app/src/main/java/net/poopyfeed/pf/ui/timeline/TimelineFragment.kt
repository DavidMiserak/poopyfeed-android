package net.poopyfeed.pf.ui.timeline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import net.poopyfeed.pf.databinding.FragmentTimelineBinding
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.util.logScreenView

/**
 * Displays a 7-day timeline of feedings, diapers, and naps for a child with day navigation. All
 * data is fetched once on load; day switching is client-side with no additional API calls.
 */
@AndroidEntryPoint
class TimelineFragment : Fragment() {

  private var _binding: FragmentTimelineBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: TimelineViewModel by viewModels()
  @Inject lateinit var tokenManager: TokenManager
  private lateinit var adapter: TimelineAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentTimelineBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    logScreenView(viewModel.analyticsTracker, "Timeline")

    // Determine profile timezone (falls back to device when not set)
    val profileTimezoneId = tokenManager.getProfileTimezone()

    // Setup adapter with gap nap callback
    adapter =
        TimelineAdapter(profileTimezoneId) { gap ->
          viewModel.createNapFromGap(gap.newerEventAt, gap.olderEventAt)
        }
    binding.recyclerTimeline.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerTimeline.adapter = adapter

    // Setup navigation buttons
    binding.btnPrevious.setOnClickListener { viewModel.previousDay() }
    binding.btnNext.setOnClickListener { viewModel.nextDay() }
    binding.btnRetry.setOnClickListener { viewModel.refresh() }

    // Collect UI state and nap creation results
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { state -> updateUI(state) } }
        launch {
          viewModel.napCreationResult.collect { message ->
            if (message != null) {
              _binding?.root?.let { root ->
                Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
              }
              viewModel.clearNapCreationResult()
            }
          }
        }
      }
    }
  }

  private fun updateUI(state: TimelineUiState) {
    val binding = _binding ?: return
    when (state) {
      is TimelineUiState.Loading -> {
        binding.recyclerTimeline.visibility = View.GONE
        binding.layoutLoadingState.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
        binding.layoutErrorState.visibility = View.GONE
      }
      is TimelineUiState.Ready -> {
        binding.layoutLoadingState.visibility = View.GONE
        binding.layoutErrorState.visibility = View.GONE

        val hasEvents = state.items.any { it is TimelineItem.Event }
        binding.recyclerTimeline.visibility = if (hasEvents) View.VISIBLE else View.GONE
        binding.layoutEmptyState.visibility = if (hasEvents) View.GONE else View.VISIBLE

        // Update day header
        binding.textDayHeader.text = state.dayHeader

        // Update buttons
        binding.btnPrevious.isEnabled = state.canGoPrevious
        binding.btnNext.isEnabled = state.canGoNext

        // Submit items (events + gap markers) to adapter
        adapter.submitList(state.items)
      }
      is TimelineUiState.Error -> {
        binding.recyclerTimeline.visibility = View.GONE
        binding.layoutLoadingState.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.layoutErrorState.visibility = View.VISIBLE
        binding.textErrorMessage.text = state.message
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
