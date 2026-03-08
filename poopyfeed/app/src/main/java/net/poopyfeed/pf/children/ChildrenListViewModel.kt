package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.repository.CachedChildrenRepository

/** UI state for the children list screen. */
sealed interface ChildrenListUiState {
  /** Loading initial data. */
  data object Loading : ChildrenListUiState

  /** Children loaded and displayed; may be empty. */
  data class Ready(val children: List<Child>) : ChildrenListUiState

  /** Cache is empty and never synced; show empty state. */
  data object Empty : ChildrenListUiState

  /** Failed to load children; [message] is user-facing. */
  data class Error(val message: String) : ChildrenListUiState
}

/**
 * ViewModel for [ChildrenListFragment]. Loads and displays a list of children with pull-to-refresh
 * support. Emits [uiState] and one-shot events via [deleteError].
 */
@HiltViewModel
class ChildrenListViewModel
@Inject
constructor(
    private val repo: CachedChildrenRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<ChildrenListUiState> =
      MutableStateFlow(ChildrenListUiState.Loading)
  val uiState: StateFlow<ChildrenListUiState> = _uiState.asStateFlow()

  private val _deleteError: MutableSharedFlow<String> = MutableSharedFlow(replay = 0)
  val deleteError = _deleteError.asSharedFlow()

  /**
   * True while a pull-to-refresh (or explicit refresh) is in progress. Stops SwipeRefreshLayout
   * spinner when done.
   */
  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  init {
    observeChildren()
    refresh()
  }

  /**
   * Observes children from cache and synced status. Offline-first: when cache has data, show it
   * (Ready) even if we haven't synced this session. Loading/Error only when cache is empty.
   */
  private fun observeChildren() {
    viewModelScope.launch {
      combine(repo.listChildrenCached(), repo.hasSyncedFlow) { result, hasSynced ->
            when {
              result is ApiResult.Success && result.data.isNotEmpty() ->
                  ChildrenListUiState.Ready(result.data)
              result is ApiResult.Success && result.data.isEmpty() && hasSynced ->
                  ChildrenListUiState.Empty
              result is ApiResult.Success && result.data.isEmpty() && !hasSynced ->
                  ChildrenListUiState.Loading
              result is ApiResult.Error ->
                  ChildrenListUiState.Error(result.error.getUserMessage(context))
              else -> ChildrenListUiState.Loading
            }
          }
          .collect { newState -> _uiState.value = newState }
    }
  }

  /**
   * Refreshes children from API and updates cache. Only emits Error if still in Loading state (to
   * avoid clobbering Ready state during pull-to-refresh). Sets [isRefreshing] so the fragment can
   * stop the SwipeRefreshLayout spinner when done (even when uiState does not change).
   */
  fun refresh() {
    viewModelScope.launch {
      _isRefreshing.value = true
      try {
        val result = repo.refreshChildren()
        if (result is ApiResult.Error && _uiState.value is ChildrenListUiState.Loading) {
          _uiState.value = ChildrenListUiState.Error(result.error.getUserMessage(context))
        }
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  /** Deletes a child from the API and cache. Emits error to [deleteError] on failure. */
  fun deleteChild(childId: Int) {
    viewModelScope.launch {
      val result = repo.deleteChild(childId)
      if (result is ApiResult.Error) {
        _deleteError.emit(result.error.getUserMessage(context))
      }
    }
  }
}
