package net.poopyfeed.pf.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.WeeklySummary
import net.poopyfeed.pf.data.repository.AnalyticsRepository

/** UI state for the pediatrician summary screen. */
sealed interface PediatricianSummaryUiState {
  data object Loading : PediatricianSummaryUiState

  data class Ready(
      val summary: WeeklySummary,
      val feedingsPerDay: Int,
      val ozPerDay: Double,
      val diapersPerDay: Int,
      val napsPerDay: Int,
      val sleepMinutesPerDay: Int,
  ) : PediatricianSummaryUiState

  data object Empty : PediatricianSummaryUiState
  data class Error(val message: String) : PediatricianSummaryUiState
}

@HiltViewModel
class PediatricianSummaryViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsRepo: AnalyticsRepository,
    val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState =
      MutableStateFlow<PediatricianSummaryUiState>(PediatricianSummaryUiState.Loading)
  val uiState: StateFlow<PediatricianSummaryUiState> = _uiState.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  init {
    load()
  }

  fun refresh() {
    _isRefreshing.value = true
    load()
  }

  private fun load() {
    viewModelScope.launch {
      when (val result = analyticsRepo.getWeeklySummary(childId)) {
        is ApiResult.Success -> {
          val s = result.data
          val totalCount = s.feedings.count + s.diapers.count + s.sleep.naps
          if (totalCount == 0) {
            _uiState.value = PediatricianSummaryUiState.Empty
          } else {
            _uiState.value =
                PediatricianSummaryUiState.Ready(
                    summary = s,
                    feedingsPerDay = s.feedings.count / 7,
                    ozPerDay = s.feedings.totalOz / 7.0,
                    diapersPerDay = s.diapers.count / 7,
                    napsPerDay = s.sleep.naps / 7,
                    sleepMinutesPerDay = s.sleep.totalMinutes / 7,
                )
          }
        }
        is ApiResult.Error -> {
          _uiState.value =
              PediatricianSummaryUiState.Error(result.error.errorMessage())
        }
        else -> Unit
      }
      _isRefreshing.value = false
    }
  }

  private fun ApiError.errorMessage(): String =
      when (this) {
        is ApiError.HttpError -> detail ?: errorMessage
        is ApiError.NetworkError -> errorMessage
        is ApiError.SerializationError -> errorMessage
        is ApiError.UnknownError -> errorMessage
      }
}
