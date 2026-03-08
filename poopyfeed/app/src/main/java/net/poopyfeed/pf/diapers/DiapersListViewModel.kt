package net.poopyfeed.pf.diapers

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
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.repository.CachedDiapersRepository

/** UI state for the diapers list screen. */
sealed interface DiapersListUiState {
  data object Loading : DiapersListUiState

  data class Ready(val diapers: List<Diaper>) : DiapersListUiState

  data object Empty : DiapersListUiState

  data class Error(val message: String) : DiapersListUiState
}

/**
 * ViewModel for [DiapersListFragment]. Loads and displays diaper changes for a child with
 * pull-to-refresh and delete support.
 */
@HiltViewModel
class DiapersListViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedDiapersRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<DiapersListUiState> =
      MutableStateFlow(DiapersListUiState.Loading)
  val uiState: StateFlow<DiapersListUiState> = _uiState.asStateFlow()

  private val _deleteError: MutableSharedFlow<String> = MutableSharedFlow(replay = 0)
  val deleteError = _deleteError.asSharedFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  init {
    observeDiapers()
    refresh()
  }

  /** Offline-first: when cache has data, show it (Ready) even if we haven't synced this session. */
  private fun observeDiapers() {
    viewModelScope.launch {
      combine(repo.listDiapersCached(childId), repo.hasSyncedFlow(childId)) { result, hasSynced ->
            when {
              result is ApiResult.Success && result.data.isNotEmpty() ->
                  DiapersListUiState.Ready(result.data)
              result is ApiResult.Success && result.data.isEmpty() && hasSynced ->
                  DiapersListUiState.Empty
              result is ApiResult.Success && result.data.isEmpty() && !hasSynced ->
                  DiapersListUiState.Loading
              result is ApiResult.Error ->
                  DiapersListUiState.Error(result.error.getUserMessage(context))
              else -> DiapersListUiState.Loading
            }
          }
          .collect { newState -> _uiState.value = newState }
    }
  }

  fun refresh() {
    viewModelScope.launch {
      _isRefreshing.value = true
      try {
        val result = repo.refreshDiapers(childId)
        when {
          result is ApiResult.Error && _uiState.value is DiapersListUiState.Loading ->
              _uiState.value = DiapersListUiState.Empty
          result is ApiResult.Error && _uiState.value is DiapersListUiState.Ready ->
              Unit // keep showing cached list so user can view logs offline
        }
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  fun deleteDiaper(diaperId: Int) {
    viewModelScope.launch {
      val result = repo.deleteDiaper(childId, diaperId)
      if (result is ApiResult.Error) {
        _deleteError.emit(result.error.getUserMessage(context))
      }
    }
  }
}
