package net.poopyfeed.pf.feedings

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
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository

/** UI state for the feedings list screen. */
sealed interface FeedingsListUiState {
  data object Loading : FeedingsListUiState

  data class Ready(val feedings: List<Feeding>) : FeedingsListUiState

  data object Empty : FeedingsListUiState

  data class Error(val message: String) : FeedingsListUiState
}

/**
 * ViewModel for [FeedingsListFragment]. Loads and displays feedings for a child with
 * pull-to-refresh and delete support.
 */
@HiltViewModel
class FeedingsListViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedFeedingsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<FeedingsListUiState> =
      MutableStateFlow(FeedingsListUiState.Loading)
  val uiState: StateFlow<FeedingsListUiState> = _uiState.asStateFlow()

  private val _deleteError: MutableSharedFlow<String> = MutableSharedFlow(replay = 0)
  val deleteError = _deleteError.asSharedFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  init {
    observeFeedings()
    refresh()
  }

  private fun observeFeedings() {
    viewModelScope.launch {
      combine(repo.listFeedingsCached(childId), repo.hasSyncedFlow(childId)) { result, hasSynced ->
            when {
              !hasSynced -> FeedingsListUiState.Loading
              result is ApiResult.Success && result.data.isEmpty() -> FeedingsListUiState.Empty
              result is ApiResult.Success -> FeedingsListUiState.Ready(result.data)
              result is ApiResult.Error ->
                  FeedingsListUiState.Error(result.error.getUserMessage(context))
              else -> FeedingsListUiState.Loading
            }
          }
          .collect { newState -> _uiState.value = newState }
    }
  }

  fun refresh() {
    viewModelScope.launch {
      _isRefreshing.value = true
      try {
        val result = repo.refreshFeedings(childId)
        when {
          result is ApiResult.Error && _uiState.value is FeedingsListUiState.Loading ->
              _uiState.value = FeedingsListUiState.Empty
          result is ApiResult.Error && _uiState.value is FeedingsListUiState.Ready ->
              _uiState.value = FeedingsListUiState.Error(result.error.getUserMessage(context))
        }
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  fun deleteFeeding(feedingId: Int) {
    viewModelScope.launch {
      val result = repo.deleteFeeding(childId, feedingId)
      if (result is ApiResult.Error) {
        _deleteError.emit(result.error.getUserMessage(context))
      }
    }
  }
}
