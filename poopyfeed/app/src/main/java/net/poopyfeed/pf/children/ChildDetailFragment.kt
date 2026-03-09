package net.poopyfeed.pf.children

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentChildDetailBinding

/**
 * Displays details of a single child including name, age, gender, and last activities. Shows edit
 * button for owner/co-parent. Delete is available from the edit screen (owner only).
 */
@AndroidEntryPoint
class ChildDetailFragment : Fragment() {

  private var _binding: FragmentChildDetailBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: ChildDetailViewModel by viewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentChildDetailBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.buttonEdit.setOnClickListener { openEditChild() }

    // Tappable "Last X" status cards — shortcut to activity lists
    binding.cardFeeding.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_feedingsList, bundle)
    }
    binding.cardDiaper.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_diapersList, bundle)
    }
    binding.cardNap.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_napsList, bundle)
    }

    binding.buttonFeedings.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_feedingsList, bundle)
    }
    binding.buttonDiapers.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_diapersList, bundle)
    }
    binding.buttonNaps.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_napsList, bundle)
    }
    binding.buttonTimeline.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_timeline, bundle)
    }
    binding.buttonShare.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_childDetail_to_sharingFragment, bundle)
    }

    collectFlows()
  }

  private fun collectFlows() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.uiState.collect { updateUiState(it) } }
        launch {
          try {
            val handle =
                findNavController().currentBackStackEntry?.savedStateHandle ?: return@launch
            handle.getStateFlow("child_deleted", false).collect { deleted ->
              if (deleted) {
                handle.set("child_deleted", false)
                findNavController().popBackStack()
              }
            }
          } catch (_: IllegalStateException) {
            // NavController not fully set up (e.g. in tests); skip child_deleted listener
          }
        }
        launch {
          try {
            val handle =
                findNavController().currentBackStackEntry?.savedStateHandle ?: return@launch
            handle.getStateFlow("child_updated", false).collect { updated ->
              if (updated) {
                handle.set("child_updated", false)
                viewModel.refresh()
              }
            }
          } catch (_: IllegalStateException) {}
        }
        launch {
          try {
            val handle =
                findNavController().currentBackStackEntry?.savedStateHandle ?: return@launch
            handle.getStateFlow("diaper_created", false).collect { created ->
              if (created) {
                handle.set("diaper_created", false)
                viewModel.refresh()
              }
            }
          } catch (_: IllegalStateException) {}
        }
        launch {
          try {
            val handle =
                findNavController().currentBackStackEntry?.savedStateHandle ?: return@launch
            handle.getStateFlow("feeding_created", false).collect { created ->
              if (created) {
                handle.set("feeding_created", false)
                viewModel.refresh()
                viewModel.refreshPatternAlerts()
              }
            }
          } catch (_: IllegalStateException) {}
        }
        launch {
          try {
            val handle =
                findNavController().currentBackStackEntry?.savedStateHandle ?: return@launch
            handle.getStateFlow("nap_created", false).collect { created ->
              if (created) {
                handle.set("nap_created", false)
                viewModel.refresh()
                viewModel.refreshPatternAlerts()
              }
            }
          } catch (_: IllegalStateException) {}
        }
      }
    }
  }

  private fun updateUiState(state: ChildDetailUiState) {
    when (state) {
      is ChildDetailUiState.Loading -> {
        binding.layoutSkeleton.visibility = View.VISIBLE
        binding.cardHero.visibility = View.GONE
        binding.labelRecentActivity.visibility = View.GONE
        binding.cardFeeding.visibility = View.GONE
        binding.cardDiaper.visibility = View.GONE
        binding.cardNap.visibility = View.GONE
        binding.cardToday.visibility = View.GONE
        binding.labelTrack.visibility = View.GONE
        binding.cardTracking.visibility = View.GONE
        binding.labelInsights.visibility = View.GONE
        binding.buttonTimeline.visibility = View.GONE
      }
      is ChildDetailUiState.Ready -> {
        binding.layoutSkeleton.visibility = View.GONE
        binding.cardHero.visibility = View.VISIBLE
        binding.labelRecentActivity.visibility = View.VISIBLE
        binding.cardFeeding.visibility = View.VISIBLE
        binding.cardDiaper.visibility = View.VISIBLE
        binding.cardNap.visibility = View.VISIBLE
        binding.cardToday.visibility = View.VISIBLE
        binding.labelTrack.visibility = View.VISIBLE
        binding.cardTracking.visibility = View.VISIBLE
        binding.labelInsights.visibility = View.VISIBLE
        binding.buttonTimeline.visibility = View.VISIBLE
        bindReadyState(state)
      }
      is ChildDetailUiState.Error ->
          Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
    }
  }

  private fun bindReadyState(state: ChildDetailUiState.Ready) {
    (activity as? AppCompatActivity)?.supportActionBar?.title = state.child.name
    binding.textChildName.text = state.child.name
    binding.textAgeGender.text = state.ageFormatted
    binding.labelFeeding.text =
        getString(R.string.child_detail_last_feeding, state.lastFeedingFormatted)
    binding.labelDiaper.text =
        getString(R.string.child_detail_last_diaper, state.lastDiaperFormatted)
    binding.labelNap.text = getString(R.string.child_detail_last_nap, state.lastNapFormatted)
    binding.chipRole.visibility = if (state.isOwner) View.GONE else View.VISIBLE
    if (!state.isOwner) {
      binding.chipRole.text = state.child.user_role.replaceFirstChar { it.uppercaseChar() }
    }
    binding.buttonEdit.visibility = if (state.canEdit) View.VISIBLE else View.GONE
    binding.buttonShare.visibility = if (state.isOwner) View.VISIBLE else View.GONE

    // Pattern alerts (feeding and nap warnings)
    val alerts = state.patternAlerts
    if (alerts != null && alerts.hasAnyAlert) {
      binding.cardPatternAlerts.visibility = View.VISIBLE
      binding.rowFeedingAlert.visibility = if (alerts.feeding.alert) View.VISIBLE else View.GONE
      binding.textFeedingAlert.text = alerts.feeding.message
      binding.rowNapAlert.visibility = if (alerts.nap.alert) View.VISIBLE else View.GONE
      binding.textNapAlert.text = alerts.nap.message
    } else {
      binding.cardPatternAlerts.visibility = View.GONE
    }

    val summary = state.dashboardSummary
    if (summary != null) {
      binding.todaySkeleton.visibility = View.GONE
      binding.todayContent.visibility = View.VISIBLE
      binding.textTodaySummary.text =
          getString(
              R.string.child_detail_today_summary,
              summary.today.feedings.count,
              summary.today.diapers.count,
              summary.today.sleep.naps,
          )
    } else {
      binding.todaySkeleton.visibility = View.VISIBLE
      binding.todayContent.visibility = View.GONE
    }
  }

  private fun openEditChild() {
    findNavController()
        .navigate(
            R.id.action_childDetail_to_editChild,
            Bundle().apply { putInt("childId", viewModel.childId) })
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
