package net.poopyfeed.pf.children

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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import net.poopyfeed.pf.MainActivity
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentChildrenListBinding
import net.poopyfeed.pf.tour.TourManager
import net.poopyfeed.pf.tour.TourStep
import net.poopyfeed.pf.util.logScreenView

/**
 * Displays a list of children with pull-to-refresh support. Allows tapping a child to view details.
 * Shows loading, empty, and error states.
 */
@AndroidEntryPoint
class ChildrenListFragment : Fragment() {

  @Inject lateinit var tourManager: TourManager

  private var _binding: FragmentChildrenListBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: ChildrenListViewModel by viewModels()
  private lateinit var adapter: ChildAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentChildrenListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    logScreenView(viewModel.analyticsTracker, "ChildrenList")

    // Setup adapter
    adapter = ChildAdapter { child -> navigateToChildDetail(child.id) }

    // Setup RecyclerView
    binding.recyclerChildren.adapter = adapter
    binding.recyclerChildren.layoutManager = LinearLayoutManager(requireContext())

    // Setup SwipeRefreshLayout
    binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }

    // Setup retry button
    binding.layoutErrorState.findViewById<View>(R.id.button_retry).setOnClickListener {
      viewModel.refresh()
    }

    // Collect UI state
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is ChildrenListUiState.Loading -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.recyclerChildren.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
            }
            is ChildrenListUiState.Ready -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerChildren.visibility = View.VISIBLE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              adapter.submitList(state.children)
            }
            is ChildrenListUiState.Empty -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerChildren.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.VISIBLE
              binding.layoutErrorState.visibility = View.GONE
            }
            is ChildrenListUiState.Error -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerChildren.visibility = View.GONE
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

    // Drive SwipeRefreshLayout spinner from isRefreshing so pull-to-refresh stops when refresh
    // completes even when uiState does not change (e.g. same list data).
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.isRefreshing.collect { refreshing ->
          binding.swipeRefresh.isRefreshing = refreshing
        }
      }
    }

    // Collect delete error events
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.deleteError.collect { message ->
          Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
      }
    }

    // Listen for child_created event from CreateChildBottomSheetFragment
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow("child_created", false)
            ?.collect { created ->
              if (created) {
                // Refresh to show the newly created child
                viewModel.refresh()
                // Clear the flag
                findNavController()
                    .currentBackStackEntry
                    ?.savedStateHandle
                    ?.set("child_created", false)
              }
            }
      }
    }

    // Listen for accept-invite result from ChildrenListFabBottomSheetFragment
    parentFragmentManager.setFragmentResultListener(
        ChildrenListFabBottomSheetFragment.ACCEPT_INVITE_RESULT_KEY,
        viewLifecycleOwner,
    ) { _, bundle ->
      val childId = bundle.getInt(ChildrenListFabBottomSheetFragment.KEY_CHILD_ID, -1)
      if (childId != -1) {
        viewModel.refresh()
        navigateToChildDetail(childId)
      }
    }

    if (tourManager.shouldShowPart(1)) {
      binding.root.postDelayed(
          {
            if (isAdded) showTourPart1()
          },
          TourManager.START_DELAY_MS,
      )
    }
  }

  private fun showTourPart1() {
    val activity = requireActivity() as? MainActivity ?: return
    val ctx = requireContext()
    val bottomNav = activity.getBottomNavView()
    val fab = activity.getFabView()

    val childrenTab = bottomNav.findViewById<View>(R.id.ChildrenListFragment) ?: return
    val notificationsTab = bottomNav.findViewById<View>(R.id.NotificationsFragment) ?: return

    val total = 4
    val steps =
        listOf(
            TourStep(
                childrenTab,
                TourManager.buildTarget(
                    ctx,
                    childrenTab,
                    getString(R.string.tour_p1_children_title),
                    getString(R.string.tour_p1_children_desc),
                    1,
                    total)),
            TourStep(
                fab,
                TourManager.buildTarget(
                    ctx,
                    fab,
                    getString(R.string.tour_p1_fab_title),
                    getString(R.string.tour_p1_fab_desc),
                    2,
                    total)),
            TourStep(
                notificationsTab,
                TourManager.buildTarget(
                    ctx,
                    notificationsTab,
                    getString(R.string.tour_p1_notifications_title),
                    getString(R.string.tour_p1_notifications_desc),
                    3,
                    total)),
            TourStep(
                null,
                TourManager.buildToolbarOverflowTarget(
                    ctx,
                    activity.getToolbarForTour(),
                    getString(R.string.tour_p1_settings_title),
                    getString(R.string.tour_p1_settings_desc),
                    4,
                    total)),
        )

    tourManager.showSequence(requireActivity(), 1, steps)
  }

  private fun navigateToChildDetail(childId: Int) {
    val bundle = Bundle().apply { putInt("childId", childId) }
    findNavController().navigate(R.id.action_childrenList_to_childDetail, bundle)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
