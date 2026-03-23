package net.poopyfeed.pf.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.db.FeedingTrendDayEntity
import net.poopyfeed.pf.data.db.SleepSummaryDayEntity
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.WeeklySummaryData
import net.poopyfeed.pf.data.repository.CachedChartsRepository

/** UI state for a single chart card. */
sealed interface ChartUiState<out T> {
  data object Loading : ChartUiState<Nothing>

  data class Ready<T>(val data: T, val weeklySummary: WeeklySummaryData? = null) : ChartUiState<T>

  data class Error(val message: String) : ChartUiState<Nothing>
}

@HiltViewModel
class ChartsViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val chartsRepository: CachedChartsRepository,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _feedingTrendsState =
      MutableStateFlow<ChartUiState<List<FeedingTrendDayEntity>>>(ChartUiState.Loading)
  val feedingTrendsState: StateFlow<ChartUiState<List<FeedingTrendDayEntity>>> =
      _feedingTrendsState.asStateFlow()

  private val _sleepSummaryState =
      MutableStateFlow<ChartUiState<List<SleepSummaryDayEntity>>>(ChartUiState.Loading)
  val sleepSummaryState: StateFlow<ChartUiState<List<SleepSummaryDayEntity>>> =
      _sleepSummaryState.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  private var feedingCollectJob: Job? = null
  private var sleepCollectJob: Job? = null

  private var lastFeedingSummary: WeeklySummaryData? = null
  private var lastSleepSummary: WeeklySummaryData? = null

  /** Load charts for the given day range. Collects from Room and refreshes from API. */
  fun loadCharts(days: Int) {
    feedingCollectJob?.cancel()
    sleepCollectJob?.cancel()

    _feedingTrendsState.value = ChartUiState.Loading
    _sleepSummaryState.value = ChartUiState.Loading

    feedingCollectJob =
        viewModelScope.launch {
          chartsRepository.getFeedingTrends(childId, days).collect { entities ->
            if (entities.isNotEmpty()) {
              _feedingTrendsState.value = ChartUiState.Ready(entities, lastFeedingSummary)
            }
          }
        }

    sleepCollectJob =
        viewModelScope.launch {
          chartsRepository.getSleepSummary(childId, days).collect { entities ->
            if (entities.isNotEmpty()) {
              _sleepSummaryState.value = ChartUiState.Ready(entities, lastSleepSummary)
            }
          }
        }

    viewModelScope.launch {
      _isRefreshing.value = true
      val feedingResult = launch { refreshFeedings(days) }
      val sleepResult = launch { refreshSleep(days) }
      feedingResult.join()
      sleepResult.join()
      _isRefreshing.value = false
    }
  }

  /** Pull-to-refresh. */
  fun refresh(days: Int) {
    loadCharts(days)
  }

  private suspend fun refreshFeedings(days: Int) {
    when (val result = chartsRepository.refreshFeedingTrends(childId, days)) {
      is ApiResult.Success -> {
        lastFeedingSummary = result.data.weeklySummary
        val current = _feedingTrendsState.value
        if (current is ChartUiState.Ready) {
          _feedingTrendsState.value = current.copy(weeklySummary = result.data.weeklySummary)
        }
      }
      is ApiResult.Error -> {
        if (_feedingTrendsState.value is ChartUiState.Loading) {
          _feedingTrendsState.value = ChartUiState.Error(result.error.displayMessage)
        }
      }
      else -> Unit
    }
  }

  private suspend fun refreshSleep(days: Int) {
    when (val result = chartsRepository.refreshSleepSummary(childId, days)) {
      is ApiResult.Success -> {
        lastSleepSummary = result.data.weeklySummary
        val current = _sleepSummaryState.value
        if (current is ChartUiState.Ready) {
          _sleepSummaryState.value = current.copy(weeklySummary = result.data.weeklySummary)
        }
      }
      is ApiResult.Error -> {
        if (_sleepSummaryState.value is ChartUiState.Loading) {
          _sleepSummaryState.value = ChartUiState.Error(result.error.displayMessage)
        }
      }
      else -> Unit
    }
  }
}
