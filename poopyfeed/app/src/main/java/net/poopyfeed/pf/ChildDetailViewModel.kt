package net.poopyfeed.pf

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
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.util.formatAge
import net.poopyfeed.pf.util.formatRelativeTime

/** UI state for the child detail screen. */
sealed interface ChildDetailUiState {
  /** Loading child data. */
  data object Loading : ChildDetailUiState

  /** Child loaded and displayed. */
  data class Ready(
      val child: Child,
      val ageFormatted: String,
      val lastFeedingFormatted: String,
      val lastDiaperFormatted: String,
      val lastNapFormatted: String,
      val isOwner: Boolean,
  ) : ChildDetailUiState

  /** Failed to load child; [message] is user-facing. */
  data class Error(val message: String) : ChildDetailUiState
}

/**
 * ViewModel for [ChildDetailFragment]. Loads a child by ID and displays their profile and last
 * activities. Handles deletion with confirmation.
 */
@HiltViewModel
class ChildDetailViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedChildrenRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<ChildDetailUiState> =
      MutableStateFlow(ChildDetailUiState.Loading)
  val uiState: StateFlow<ChildDetailUiState> = _uiState.asStateFlow()

  private val _navigateBack: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 0)
  val navigateBack = _navigateBack.asSharedFlow()

  private val _deleteError: MutableSharedFlow<String> = MutableSharedFlow(replay = 0)
  val deleteError = _deleteError.asSharedFlow()

  init {
    observeChild()
    refresh()
  }

  /**
   * Refresh child data from API to sync with latest changes (e.g., new feedings added elsewhere).
   */
  fun refresh() {
    viewModelScope.launch { repo.refreshChildren() }
  }

  /** Observes the child from cache and formats data for display. */
  private fun observeChild() {
    viewModelScope.launch {
      repo.getChildCached(childId).collect { child ->
        if (child == null) {
          _uiState.value = ChildDetailUiState.Error("Child not found")
          return@collect
        }

        val ageFormatted = formatAge(child.date_of_birth)
        val lastFeedingFormatted = formatRelativeTime(context, child.last_feeding)
        val lastDiaperFormatted = formatRelativeTime(context, child.last_diaper_change)
        val lastNapFormatted = formatRelativeTime(context, child.last_nap)
        val isOwner = child.user_role == "owner"

        _uiState.value =
            ChildDetailUiState.Ready(
                child = child,
                ageFormatted = ageFormatted,
                lastFeedingFormatted = lastFeedingFormatted,
                lastDiaperFormatted = lastDiaperFormatted,
                lastNapFormatted = lastNapFormatted,
                isOwner = isOwner,
            )
      }
    }
  }

  /** Deletes the child from the API and cache. Navigates back on success or emits error. */
  fun deleteChild() {
    viewModelScope.launch {
      val result = repo.deleteChild(childId)
      when (result) {
        is ApiResult.Success -> {
          _navigateBack.emit(Unit)
        }
        is ApiResult.Error -> {
          _deleteError.emit(result.error.getUserMessage(context))
        }
        is ApiResult.Loading -> {
          // no-op; we manage loading locally
        }
      }
    }
  }
}
