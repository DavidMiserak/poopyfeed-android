package net.poopyfeed.pf.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentPediatricianSummaryBinding
import net.poopyfeed.pf.util.logScreenView

@AndroidEntryPoint
class PediatricianSummaryFragment : Fragment() {

  private var _binding: FragmentPediatricianSummaryBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: PediatricianSummaryViewModel by viewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentPediatricianSummaryBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    logScreenView(viewModel.analyticsTracker, "PediatricianSummary")

    binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    collectFlows()
  }

  private fun collectFlows() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { updateUi(it) } }
        launch {
          viewModel.isRefreshing.collect { refreshing ->
            binding.swipeRefresh.isRefreshing =
                refreshing || viewModel.uiState.value is PediatricianSummaryUiState.Loading
          }
        }
      }
    }
  }

  private fun updateUi(state: PediatricianSummaryUiState) {
    binding.progressLoading.visibility = View.GONE
    binding.textError.visibility = View.GONE
    binding.textEmpty.visibility = View.GONE
    binding.layoutContent.visibility = View.GONE

    when (state) {
      is PediatricianSummaryUiState.Loading -> {
        binding.progressLoading.visibility = View.VISIBLE
      }
      is PediatricianSummaryUiState.Ready -> {
        binding.layoutContent.visibility = View.VISIBLE
        binding.textFeedingsPerDay.text =
            getString(R.string.pediatrician_feedings_per_day, state.feedingsPerDay.toString())
        binding.textOzPerDay.text =
            String.format(getString(R.string.pediatrician_oz_per_day), state.ozPerDay)
        binding.textDiapersPerDay.text =
            getString(R.string.pediatrician_diapers_per_day, state.diapersPerDay.toString())
        binding.textNapsPerDay.text =
            getString(R.string.pediatrician_naps_per_day, state.napsPerDay.toString())
        val hours = state.sleepMinutesPerDay / 60
        val mins = state.sleepMinutesPerDay % 60
        val sleepStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        binding.textSleepPerDay.text = getString(R.string.pediatrician_sleep_per_day, sleepStr)
      }
      is PediatricianSummaryUiState.Empty -> {
        binding.textEmpty.visibility = View.VISIBLE
      }
      is PediatricianSummaryUiState.Error -> {
        binding.textError.visibility = View.VISIBLE
        binding.textError.text = state.message
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
