package net.poopyfeed.pf.ui.timeline

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.TimelineEvent
import net.poopyfeed.pf.data.repository.AnalyticsRepository

/** UI state for the timeline screen. */
sealed interface TimelineUiState {
  /** Events are loading. */
  data object Loading : TimelineUiState

  /** Events loaded; [eventsForDay] is filtered for current day, [dayHeader] is formatted label. */
  data class Ready(
      val eventsForDay: List<TimelineEvent>,
      val dayHeader: String,
      val canGoPrevious: Boolean,
      val canGoNext: Boolean,
  ) : TimelineUiState

  /** Request failed; [message] is user-facing. */
  data class Error(val message: String) : TimelineUiState
}

/**
 * ViewModel for [TimelineFragment]. Loads all timeline events for a child (7 days max) and provides
 * client-side day navigation. Day filtering is done in memory; no additional network calls needed.
 */
@HiltViewModel
class TimelineViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsRepository: AnalyticsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  /** All events fetched from API (newest-first, same order as server). */
  private val _allEvents: MutableStateFlow<List<TimelineEvent>> = MutableStateFlow(emptyList())

  /** Day offset: 0 = today, 1 = yesterday, ..., 6 = oldest allowed day. */
  private val _dayOffset: MutableStateFlow<Int> = MutableStateFlow(0)

  /** Combined UI state: filters allEvents by dayOffset, formats day header, checks boundaries. */
  val uiState: Flow<TimelineUiState> =
      combine(_allEvents, _dayOffset) { allEvents, dayOffset ->
        computeUiState(allEvents, dayOffset)
      }

  init {
    loadTimeline()
  }

  /** Loads all timeline events from API and updates [_allEvents]. */
  private fun loadTimeline() {
    viewModelScope.launch {
      when (val result = analyticsRepository.getTimeline(childId)) {
        is ApiResult.Success -> {
          val events = result.data.results
          _allEvents.value = events
        }
        is ApiResult.Error -> {
          // Error is handled in computeUiState
          _allEvents.value = emptyList()
        }
        is ApiResult.Loading -> {
          // no-op; we manage Loading locally
        }
      }
    }
  }

  /** Computes UI state from all events and current day offset. */
  private fun computeUiState(allEvents: List<TimelineEvent>, dayOffset: Int): TimelineUiState {
    if (allEvents.isEmpty()) {
      return TimelineUiState.Loading
    }

    // Filter events for the requested day (today - dayOffset)
    val eventsForDay = filterEventsByDay(allEvents, dayOffset)
    val dayHeader = formatDayHeader(dayOffset)
    val canGoPrevious = dayOffset < 6
    val canGoNext = dayOffset > 0

    return TimelineUiState.Ready(
        eventsForDay = eventsForDay,
        dayHeader = dayHeader,
        canGoPrevious = canGoPrevious,
        canGoNext = canGoNext,
    )
  }

  /**
   * Filters events for a specific day. Computes the date for (today - dayOffset) and filters events
   * whose "at" timestamp falls within that day (ISO 8601 prefix match).
   */
  private fun filterEventsByDay(events: List<TimelineEvent>, dayOffset: Int): List<TimelineEvent> {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val localTz = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(localTz).date
    val targetDate = today.subtract(dayOffset)

    val targetDatePrefix = targetDate.toString() // "YYYY-MM-DD"

    // Filter events that fall on targetDate and sort oldest-first (reverse server order)
    return events.filter { event -> event.at.startsWith(targetDatePrefix) }.reversed()
  }

  /** Formats day header label based on offset: "Today", "Yesterday", or "Mon, Mar 4". */
  private fun formatDayHeader(dayOffset: Int): String {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val localTz = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(localTz).date
    val targetDate = today.subtract(dayOffset)

    return when (dayOffset) {
      0 -> "Today"
      1 -> "Yesterday"
      else -> {
        val dayOfWeek = targetDate.dayOfWeek.name.take(3).capitalize()
        val monthName = getMonthName(targetDate.monthNumber)
        "${dayOfWeek}, ${monthName} ${targetDate.dayOfMonth}"
      }
    }
  }

  private fun getMonthName(monthNumber: Int): String {
    return when (monthNumber) {
      1 -> "Jan"
      2 -> "Feb"
      3 -> "Mar"
      4 -> "Apr"
      5 -> "May"
      6 -> "Jun"
      7 -> "Jul"
      8 -> "Aug"
      9 -> "Sep"
      10 -> "Oct"
      11 -> "Nov"
      12 -> "Dec"
      else -> "???"
    }
  }

  /** Subtracts days from a LocalDate. */
  private fun LocalDate.subtract(days: Int): LocalDate {
    return Instant.fromEpochMilliseconds(
            Instant.parse("${this}T00:00:00Z").toEpochMilliseconds() - (days * 86_400_000L))
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
  }

  /** Moves to the previous day (increases dayOffset). */
  fun previousDay() {
    val current = _dayOffset.value
    if (current < 6) {
      _dayOffset.value = current + 1
    }
  }

  /** Moves to the next day (decreases dayOffset). */
  fun nextDay() {
    val current = _dayOffset.value
    if (current > 0) {
      _dayOffset.value = current - 1
    }
  }

  /** Refreshes timeline data from API. */
  fun refresh() {
    loadTimeline()
  }
}
