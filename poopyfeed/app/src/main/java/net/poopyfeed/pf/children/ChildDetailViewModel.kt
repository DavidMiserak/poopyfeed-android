package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.models.DashboardSummaryResponse
import net.poopyfeed.pf.data.models.PatternAlertsResponse
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.util.formatAge
import net.poopyfeed.pf.util.formatRelativeTime

/** UI state for the child detail screen. */
sealed interface ChildDetailUiState {
  /** Loading child data. */
  data object Loading : ChildDetailUiState

  /**
   * Child loaded and displayed. [dashboardSummary] is null until batch load completes (show
   * skeleton). [patternAlerts] is null until async load completes.
   */
  data class Ready(
      val child: Child,
      val ageFormatted: String,
      val lastFeedingFormatted: String,
      val lastDiaperFormatted: String,
      val lastNapFormatted: String,
      val isOwner: Boolean,
      val canEdit: Boolean,
      val dashboardSummary: DashboardSummaryResponse? = null,
      val patternAlerts: PatternAlertsResponse? = null,
  ) : ChildDetailUiState

  /** Failed to load child; [message] is user-facing. */
  data class Error(val message: String) : ChildDetailUiState
}

/**
 * ViewModel for [ChildDetailFragment]. Loads a child by ID and displays their profile and last
 * activities. Delete is handled from the edit screen.
 */
@HiltViewModel
class ChildDetailViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedChildrenRepository,
    private val analyticsRepo: AnalyticsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<ChildDetailUiState> =
      MutableStateFlow(ChildDetailUiState.Loading)
  val uiState: StateFlow<ChildDetailUiState> = _uiState.asStateFlow()

  init {
    observeChild()
    loadDashboardSummary()
    loadPatternAlerts()
    refresh()
  }

  /** Loads batch dashboard summary (today + weekly + unread). Updates Ready state when done. */
  private fun loadDashboardSummary() {
    viewModelScope.launch {
      when (val result = repo.getDashboardSummary(childId)) {
        is ApiResult.Success -> {
          val current = _uiState.value
          if (current is ChildDetailUiState.Ready) {
            _uiState.value = current.copy(dashboardSummary = result.data)
          }
        }
        is ApiResult.Error -> {
          // Leave dashboardSummary null so Today card shows skeleton/error; no toast to avoid noise
        }
        else -> Unit
      }
    }
  }

  /** Loads pattern alerts (feeding and nap warnings). Updates Ready state when done. */
  private fun loadPatternAlerts() {
    viewModelScope.launch {
      when (val result = analyticsRepo.getPatternAlerts(childId)) {
        is ApiResult.Success -> {
          val current = _uiState.value
          if (current is ChildDetailUiState.Ready) {
            _uiState.value = current.copy(patternAlerts = result.data)
          }
        }
        is ApiResult.Error -> {
          // Silent suppression per spec: if API fails, no alert cards shown
        }
        else -> Unit
      }
    }
  }

  /** Refresh pattern alerts asynchronously. Called after feeding or nap mutations. */
  fun refreshPatternAlerts() = loadPatternAlerts()

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

        val age = formatAge(child.date_of_birth)
        val gender =
            when (child.gender) {
              "M" -> "Boy"
              "F" -> "Girl"
              else -> "Other"
            }
        val ageFormatted = "$age • $gender"
        val lastFeedingFormatted = formatRelativeTime(context, child.last_feeding)
        val lastDiaperFormatted = formatRelativeTime(context, child.last_diaper_change)
        val lastNapFormatted = formatRelativeTime(context, child.last_nap)
        val isOwner = child.user_role == "owner"
        val canEdit = child.can_edit

        _uiState.value =
            ChildDetailUiState.Ready(
                child = child,
                ageFormatted = ageFormatted,
                lastFeedingFormatted = lastFeedingFormatted,
                lastDiaperFormatted = lastDiaperFormatted,
                lastNapFormatted = lastNapFormatted,
                isOwner = isOwner,
                canEdit = canEdit,
                dashboardSummary = (_uiState.value as? ChildDetailUiState.Ready)?.dashboardSummary,
            )
      }
    }
  }
}
