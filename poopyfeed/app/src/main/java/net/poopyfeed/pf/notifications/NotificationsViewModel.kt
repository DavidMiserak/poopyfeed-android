package net.poopyfeed.pf.notifications

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.data.repository.NotificationsRepository

/** UI state for the notifications list screen. */
sealed interface NotificationsListUiState {
  data object Loading : NotificationsListUiState

  data class Ready(
      val notifications: List<Notification>,
      val hasNextPage: Boolean,
      val isLoadingMore: Boolean = false,
  ) : NotificationsListUiState

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
    val analyticsTracker: AnalyticsTracker,
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

  /**
   * One-shot: emitted when unread count may have changed (e.g. after mark all read or mark single
   * read). MainActivity should call refreshUnreadCount() so the bottom nav badge updates.
   */
  private val _unreadCountInvalidated = MutableSharedFlow<Unit>(replay = 0)
  val unreadCountInvalidated: SharedFlow<Unit> = _unreadCountInvalidated.asSharedFlow()

  /** Paging 3 flow for paginated notifications. */
  val pagingData: Flow<PagingData<Notification>> = repo.pagedNotifications()

  /** Next page to request when loading more (2 after first page, then 3, 4, …). */
  private var nextPageToLoad = 2

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
      nextPageToLoad = 2
      when (val result = repo.listNotifications(page = 1)) {
        is ApiResult.Success -> {
          val list = result.data.results
          _uiState.value =
              when {
                list.isEmpty() -> NotificationsListUiState.Empty
                else ->
                    NotificationsListUiState.Ready(
                        notifications = list,
                        hasNextPage = result.data.hasNextPage,
                        isLoadingMore = false)
              }
        }
        is ApiResult.Error ->
            _uiState.value = NotificationsListUiState.Error(result.error.getUserMessage(context))
        else -> Unit
      }
      _isRefreshing.value = false
    }
  }

  /**
   * Load the next page of notifications and append to the list. No-op if not in Ready state,
   * hasNextPage is false, or a load-more is already in progress.
   */
  fun loadNextPage() {
    val current = _uiState.value
    if (current !is NotificationsListUiState.Ready || !current.hasNextPage || current.isLoadingMore)
        return
    viewModelScope.launch {
      _uiState.value = current.copy(isLoadingMore = true)
      when (val result = repo.listNotifications(page = nextPageToLoad)) {
        is ApiResult.Success -> {
          val combined = current.notifications + result.data.results
          nextPageToLoad++
          _uiState.value =
              NotificationsListUiState.Ready(
                  notifications = combined,
                  hasNextPage = result.data.hasNextPage,
                  isLoadingMore = false)
        }
        is ApiResult.Error -> {
          _uiState.value = current.copy(isLoadingMore = false)
          _errorMessage.emit(result.error.getUserMessage(context))
        }
        else -> _uiState.value = current.copy(isLoadingMore = false)
      }
    }
  }

  fun markAllRead() {
    viewModelScope.launch {
      when (val result = repo.markAllRead()) {
        is ApiResult.Success -> {
          _unreadCountInvalidated.emit(Unit)
          refresh()
        }
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
          _unreadCountInvalidated.emit(Unit)
          _navigateToChild.emit(childId)
          refresh()
        }
        is ApiResult.Error -> _errorMessage.emit(result.error.getUserMessage(context))
        else -> Unit
      }
    }
  }
}
