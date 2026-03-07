package net.poopyfeed.pf.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.data.repository.NotificationsRepository

/** UI state for the notifications list screen. */
sealed interface NotificationsListUiState {
  data object Loading : NotificationsListUiState

  data class Ready(val notifications: List<Notification>, val hasNextPage: Boolean) :
      NotificationsListUiState

  data object Empty : NotificationsListUiState

  data class Error(val message: String) : NotificationsListUiState
}

/**
 * ViewModel for [NotificationsFragment]. Loads notifications list, supports mark all read and tap
 * to navigate to child dashboard (marking that notification read).
 */
@HiltViewModel
class NotificationsViewModel
@Inject
constructor(
    private val repo: NotificationsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<NotificationsListUiState> =
      MutableStateFlow(NotificationsListUiState.Loading)
  val uiState: StateFlow<NotificationsListUiState> = _uiState.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  /** One-shot: navigate to child dashboard (childId). Fragment should navigate and mark read. */
  private val _navigateToChild = MutableSharedFlow<Int>(replay = 0)
  val navigateToChild: SharedFlow<Int> = _navigateToChild.asSharedFlow()

  /** One-shot errors (e.g. mark all read failed). */
  private val _errorMessage = MutableSharedFlow<String>(replay = 0)
  val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

  init {
    refresh()
  }

  /** Whether there is at least one unread notification in the current list. */
  val hasUnread: Boolean
    get() {
      val state = _uiState.value
      return state is NotificationsListUiState.Ready && state.notifications.any { !it.isRead }
    }

  fun refresh() {
    viewModelScope.launch {
      _isRefreshing.value = true
      when (val result = repo.listNotifications(page = 1)) {
        is ApiResult.Success -> {
          val list = result.data.results
          _uiState.value =
              when {
                list.isEmpty() -> NotificationsListUiState.Empty
                else ->
                    NotificationsListUiState.Ready(
                        notifications = list, hasNextPage = result.data.hasNextPage)
              }
        }
        is ApiResult.Error ->
            _uiState.value = NotificationsListUiState.Error(result.error.getUserMessage(context))
        else -> Unit
      }
      _isRefreshing.value = false
    }
  }

  fun markAllRead() {
    viewModelScope.launch {
      when (val result = repo.markAllRead()) {
        is ApiResult.Success -> refresh()
        is ApiResult.Error -> _errorMessage.emit(result.error.getUserMessage(context))
        else -> Unit
      }
    }
  }

  /**
   * Mark the notification as read and emit [childId] so the fragment can navigate to child
   * dashboard.
   */
  fun markAsReadAndNavigate(notificationId: Int, childId: Int) {
    viewModelScope.launch {
      when (val result = repo.markAsRead(notificationId)) {
        is ApiResult.Success -> {
          _navigateToChild.emit(childId)
          refresh()
        }
        is ApiResult.Error -> _errorMessage.emit(result.error.getUserMessage(context))
        else -> Unit
      }
    }
  }
}
