package net.poopyfeed.pf.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.MainActivityViewModel
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentNotificationsBinding
import net.poopyfeed.pf.ui.common.PagingLoadStateAdapter

/**
 * In-app notifications center. Lists notifications with pull-to-refresh, "Load more" for
 * pagination, "Mark all read" in toolbar, and tap to navigate to child dashboard (marking that
 * notification read).
 */
@AndroidEntryPoint
class NotificationsFragment : Fragment() {

  private var _binding: FragmentNotificationsBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: NotificationsViewModel by viewModels()
  private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
  private lateinit var adapter: NotificationAdapter

  private val requestPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
          Snackbar.make(
                  binding.root,
                  getString(R.string.notifications_permission_denied),
                  Snackbar.LENGTH_LONG)
              .show()
        }
      }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    requireActivity()
        .addMenuProvider(
            object : MenuProvider {
              override fun onCreateMenu(menu: Menu, menuInflater: android.view.MenuInflater) {
                menuInflater.inflate(R.menu.menu_notifications, menu)
              }

              override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.action_mark_all_read)?.isEnabled = viewModel.hasUnread
              }

              override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                return when (menuItem.itemId) {
                  R.id.action_mark_all_read -> {
                    viewModel.markAllRead()
                    true
                  }
                  else -> false
                }
              }
            },
            viewLifecycleOwner,
            Lifecycle.State.STARTED)

    adapter =
        NotificationAdapter(
            onNotificationClick = { notification ->
              viewModel.markAsReadAndNavigate(notification.id, notification.childId)
            })
    binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
    binding.recyclerNotifications.adapter =
        adapter.withLoadStateFooter(footer = PagingLoadStateAdapter { adapter.retry() })

    binding.swipeRefresh.setOnRefreshListener { adapter.refresh() }

    requestNotificationPermissionIfNeeded()

    // Collect paginated notifications and submit to adapter
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

          // Manage center loading spinner and state overlays
          when {
            // Initial load - show center spinner
            loadStates.refresh is LoadState.Loading && adapter.itemCount == 0 -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.recyclerNotifications.visibility = View.GONE
            }
            // Error during initial load - show error
            loadStates.refresh is LoadState.Error && adapter.itemCount == 0 -> {
              binding.progressLoading.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.VISIBLE
              binding.recyclerNotifications.visibility = View.GONE
              binding.layoutErrorState
                  .findViewById<android.widget.TextView>(R.id.text_error_message)
                  .text =
                  (loadStates.refresh as? LoadState.Error)?.error?.message ?: "Unknown error"
            }
            // Data loaded - show list
            loadStates.refresh is LoadState.NotLoading && adapter.itemCount > 0 -> {
              binding.progressLoading.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.recyclerNotifications.visibility = View.VISIBLE
            }
            // Empty result - show empty state
            loadStates.refresh is LoadState.NotLoading && adapter.itemCount == 0 -> {
              binding.progressLoading.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.VISIBLE
              binding.layoutErrorState.visibility = View.GONE
              binding.recyclerNotifications.visibility = View.GONE
            }
          }

          requireActivity().invalidateOptionsMenu()
        }
      }
    }

    binding.layoutErrorState.findViewById<View>(R.id.button_retry).setOnClickListener {
      adapter.refresh()
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.navigateToChild.collect { childId ->
          val bundle = Bundle().apply { putInt("childId", childId) }
          findNavController().navigate(R.id.action_notificationsFragment_to_childDetail, bundle)
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.errorMessage.collect { message ->
          Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.unreadCountInvalidated.collect { mainActivityViewModel.refreshUnreadCount() }
      }
    }
  }

  private fun requestNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
          PackageManager.PERMISSION_GRANTED) {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
