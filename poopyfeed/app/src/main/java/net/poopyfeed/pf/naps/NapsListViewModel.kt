package net.poopyfeed.pf.naps

import android.content.Context
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.datetime.Clock
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.models.UpdateNapRequest
import net.poopyfeed.pf.data.repository.CachedNapsRepository

/** UI state for the naps list screen. */
sealed interface NapsListUiState {
  data object Loading : NapsListUiState

  data class Ready(val naps: List<Nap>) : NapsListUiState

  data object Empty : NapsListUiState

  data class Error(val message: String) : NapsListUiState
}

/**
 * ViewModel for [NapsListFragment]. Loads and displays naps for a child with pull-to-refresh,
 * delete, and end-nap support.
 */
@HiltViewModel
class NapsListViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedNapsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<NapsListUiState> =
      MutableStateFlow(NapsListUiState.Loading)
  val uiState: StateFlow<NapsListUiState> = _uiState.asStateFlow()

  private val _deleteError: MutableSharedFlow<String> = MutableSharedFlow(replay = 0)
  val deleteError = _deleteError.asSharedFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  init {
    observeNaps()
    refresh()
  }

  /** Offline-first: when cache has data, show it (Ready) even if we haven't synced this session. */
  private fun observeNaps() {
    viewModelScope.launch {
      combine(repo.listNapsCached(childId), repo.hasSyncedFlow(childId)) { result, hasSynced ->
            when {
              result is ApiResult.Success && result.data.isNotEmpty() ->
                  NapsListUiState.Ready(result.data)
              result is ApiResult.Success && result.data.isEmpty() && hasSynced ->
                  NapsListUiState.Empty
              result is ApiResult.Success && result.data.isEmpty() && !hasSynced ->
                  NapsListUiState.Loading
              result is ApiResult.Error ->
                  NapsListUiState.Error(result.error.getUserMessage(context))
              else -> NapsListUiState.Loading
            }
          }
          .collect { newState -> _uiState.value = newState }
    }
  }

  fun refresh() {
    viewModelScope.launch {
      _isRefreshing.value = true
      try {
        val result = repo.refreshNaps(childId)
        when {
          result is ApiResult.Error && _uiState.value is NapsListUiState.Loading ->
              _uiState.value = NapsListUiState.Error(result.error.getUserMessage(context))
          result is ApiResult.Error && _uiState.value is NapsListUiState.Ready ->
              _uiState.value = NapsListUiState.Error(result.error.getUserMessage(context))
        }
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  fun deleteNap(napId: Int) {
    viewModelScope.launch {
      val result = repo.deleteNap(childId, napId)
      if (result is ApiResult.Error) {
        _deleteError.emit(result.error.getUserMessage(context))
      }
    }
  }

  /** Ends an ongoing nap by setting end_time to now (UTC ISO 8601). */
  fun endNap(napId: Int) {
    viewModelScope.launch {
      val result = repo.updateNap(childId, napId, UpdateNapRequest(end_time = nowIso8601()))
      if (result is ApiResult.Error) {
        _deleteError.emit(result.error.getUserMessage(context))
      }
    }
  }

  private fun nowIso8601(): String = Clock.System.now().toString()
}
