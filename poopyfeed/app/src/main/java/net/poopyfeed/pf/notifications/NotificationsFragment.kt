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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.MainActivityViewModel
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentNotificationsBinding

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
            },
            onLoadMoreClick = { viewModel.loadNextPage() })
    binding.recyclerNotifications.adapter = adapter
    binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())

    binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    binding.layoutErrorState.findViewById<View>(R.id.button_retry).setOnClickListener {
      viewModel.refresh()
    }

    requestNotificationPermissionIfNeeded()

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
          when (state) {
            is NotificationsListUiState.Loading -> {
              binding.progressLoading.visibility = View.VISIBLE
              binding.recyclerNotifications.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              binding.swipeRefresh.isRefreshing = true
            }
            is NotificationsListUiState.Ready -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerNotifications.visibility = View.VISIBLE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.GONE
              val list =
                  state.notifications.map { NotificationsListItem.NotificationItem(it) } +
                      if (state.hasNextPage) {
                        listOf(NotificationsListItem.LoadMoreFooter(state.isLoadingMore))
                      } else {
                        emptyList()
                      }
              adapter.submitList(list)
            }
            is NotificationsListUiState.Empty -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerNotifications.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.VISIBLE
              binding.layoutErrorState.visibility = View.GONE
            }
            is NotificationsListUiState.Error -> {
              binding.progressLoading.visibility = View.GONE
              binding.recyclerNotifications.visibility = View.GONE
              binding.layoutEmptyState.visibility = View.GONE
              binding.layoutErrorState.visibility = View.VISIBLE
              binding.layoutErrorState
                  .findViewById<android.widget.TextView>(R.id.text_error_message)
                  .text = state.message
            }
          }
          requireActivity().invalidateOptionsMenu()
        }
      }
    }

    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.isRefreshing.collect { refreshing ->
          binding.swipeRefresh.isRefreshing =
              refreshing || viewModel.uiState.value is NotificationsListUiState.Loading
        }
      }
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
