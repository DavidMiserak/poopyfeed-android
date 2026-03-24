package net.poopyfeed.pf.ui.timeline

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateNapRequest
import net.poopyfeed.pf.data.models.TimelineEvent
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.sync.SyncScheduler

/** An item in the timeline list — either a real event or a gap marker between events. */
sealed interface TimelineItem {
  data class Event(val event: TimelineEvent) : TimelineItem

  /**
   * Gap between two events. [showAddNapButton] is true when the gap is over
   * [NAP_BUTTON_MIN_GAP_MINUTES], so the "Add nap" action is only offered for longer gaps.
   */
  data class Gap(
      val durationMinutes: Long,
      val newerEventAt: String,
      val olderEventAt: String,
      val showAddNapButton: Boolean = durationMinutes > NAP_BUTTON_MIN_GAP_MINUTES,
  ) : TimelineItem {
    companion object {
      /** Minimum gap (minutes) above which the "Add nap" button is shown. */
      const val NAP_BUTTON_MIN_GAP_MINUTES = 60L
    }
  }
}

/** UI state for the timeline screen. */
sealed interface TimelineUiState {
  /** Events are loading. */
  data object Loading : TimelineUiState

  /** Events loaded; [items] contains events interleaved with gap markers. */
  data class Ready(
      val items: List<TimelineItem>,
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
    private val napsRepository: CachedNapsRepository,
    private val syncScheduler: SyncScheduler,
    private val tokenManager: TokenManager,
    @param:ApplicationContext private val context: Context,
    val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  /** All events fetched from API (newest-first, same order as server). */
  private val _allEvents: MutableStateFlow<List<TimelineEvent>> = MutableStateFlow(emptyList())

  /** Day offset: 0 = today, 1 = yesterday, ..., 6 = oldest allowed day. */
  private val _dayOffset: MutableStateFlow<Int> = MutableStateFlow(0)

  /** Tracks fetch lifecycle: null = loading, empty = loaded OK, non-empty = error message. */
  private val _fetchStatus: MutableStateFlow<String?> = MutableStateFlow(null)

  /** Whether a refresh is in progress (for pull-to-refresh spinner). */
  private val _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val isRefreshing: Flow<Boolean> = _isRefreshing

  /**
   * Timezone used for timeline day boundaries and headers. Prefers the user's profile timezone when
   * available; falls back to the device timezone.
   */
  private val timelineTimeZone: TimeZone =
      tokenManager.getProfileTimezone()?.let { tzId ->
        runCatching { TimeZone.of(tzId) }.getOrNull() ?: TimeZone.currentSystemDefault()
      } ?: TimeZone.currentSystemDefault()

  /** Combined UI state: filters allEvents by dayOffset, formats day header, checks boundaries. */
  val uiState: Flow<TimelineUiState> =
      combine(_allEvents, _dayOffset, _fetchStatus) { allEvents, dayOffset, fetchStatus ->
        computeUiState(allEvents, dayOffset, fetchStatus)
      }

  init {
    loadTimeline()
  }

  /** Whether [loadTimeline] has completed at least once. */
  private var hasLoadedOnce = false

  /** Loads all timeline events from API and updates [_allEvents]. */
  private fun loadTimeline() {
    // Only show full loading state on initial load; refreshes keep existing data visible
    if (!hasLoadedOnce) {
      _fetchStatus.value = null // loading
    }
    _isRefreshing.value = true
    viewModelScope.launch {
      when (val result = analyticsRepository.getTimeline(childId)) {
        is ApiResult.Success -> {
          _allEvents.value = result.data.results
          _fetchStatus.value = "" // loaded OK
          hasLoadedOnce = true
        }
        is ApiResult.Error -> {
          if (hasLoadedOnce) {
            // Refresh failed — keep existing data, show transient error via dedicated flow
            _refreshError.value = result.error.getUserMessage(context)
          } else {
            // Initial load failed — show error state
            _allEvents.value = emptyList()
            _fetchStatus.value = result.error.getUserMessage(context)
          }
        }
        is ApiResult.Loading -> {
          // no-op; we manage Loading locally
        }
      }
      _isRefreshing.value = false
    }
  }

  /** Computes UI state from all events, current day offset, and fetch status. */
  private fun computeUiState(
      allEvents: List<TimelineEvent>,
      dayOffset: Int,
      fetchStatus: String?,
  ): TimelineUiState {
    // null = still loading
    if (fetchStatus == null) {
      return TimelineUiState.Loading
    }
    // non-empty = error message from API
    if (fetchStatus.isNotEmpty()) {
      return TimelineUiState.Error(fetchStatus)
    }

    // Filter events for the requested day (today - dayOffset)
    val eventsForDay = filterEventsByDay(allEvents, dayOffset)
    val items = interleaveGaps(eventsForDay)
    val dayHeader = formatDayHeader(dayOffset)
    val canGoPrevious = dayOffset < 6
    val canGoNext = dayOffset > 0

    return TimelineUiState.Ready(
        items = items,
        dayHeader = dayHeader,
        canGoPrevious = canGoPrevious,
        canGoNext = canGoNext,
    )
  }

  /**
   * Interleaves [TimelineItem.Gap] markers from backend-provided gap metadata. The backend computes
   * gaps across all events and provides [gap_after_minutes], [gap_after_start], and [gap_after_end]
   * for each event. Events are newest-first, so each event's gap represents the quiet period
   * between that event and the next (older) event.
   *
   * All gaps the backend provides are shown. Gap display is shifted: each event shows the gap from
   * the next (older) event in the array, making gaps appear visually between the two events they
   * connect in the reverse-chronological display.
   */
  private fun interleaveGaps(events: List<TimelineEvent>): List<TimelineItem> {
    if (events.isEmpty()) return emptyList()
    val items = mutableListOf<TimelineItem>()
    for (i in events.indices) {
      val event = events[i]
      items.add(TimelineItem.Event(event))
      // Shift gap display: show the next (older) event's gap on the current event
      if (i < events.lastIndex) {
        val nextEvent = events[i + 1]
        if (nextEvent.gapAfterMinutes != null &&
            nextEvent.gapAfterStart != null &&
            nextEvent.gapAfterEnd != null) {
          val (newer, older) = orderGapBoundaries(nextEvent.gapAfterStart, nextEvent.gapAfterEnd)
          if (newer != null && older != null) {
            items.add(
                TimelineItem.Gap(
                    durationMinutes = nextEvent.gapAfterMinutes,
                    newerEventAt = newer,
                    olderEventAt = older,
                ))
          }
        }
      }
    }
    return items
  }

  private fun parseEpochMs(isoString: String): Long? {
    return try {
      kotlin.time.Instant.parse(isoString).toEpochMilliseconds()
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Returns (newerEventAt, olderEventAt) so that createNapFromGap and the UI always get the correct
   * order regardless of whether the backend sends gap_after_start/end in chronological order or as
   * (newer, older).
   */
  private fun orderGapBoundaries(
      gapAfterStart: String,
      gapAfterEnd: String,
  ): Pair<String?, String?> {
    val startMs = parseEpochMs(gapAfterStart) ?: return Pair(null, null)
    val endMs = parseEpochMs(gapAfterEnd) ?: return Pair(null, null)
    return if (startMs >= endMs) {
      Pair(gapAfterStart, gapAfterEnd)
    } else {
      Pair(gapAfterEnd, gapAfterStart)
    }
  }

  /**
   * Filters events for a specific day. Computes the local date for (today - dayOffset) in
   * [timelineTimeZone] and includes only events whose UTC "at" timestamp converts to that same
   * local date. This avoids relying on the raw ISO date prefix, which may differ from the user's
   * local day near midnight in non-UTC timezones.
   */
  private fun filterEventsByDay(events: List<TimelineEvent>, dayOffset: Int): List<TimelineEvent> {
    val now = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val localTz = timelineTimeZone
    val today = now.toLocalDateTime(localTz).date
    val targetDate = today.subtract(dayOffset)

    // Filter events that fall on targetDate in the user's profile timezone (newest-first, matching
    // server order)
    return events.filter { event ->
      val eventDate =
          try {
            kotlin.time.Instant.parse(event.at).toLocalDateTime(localTz).date
          } catch (_: Exception) {
            null
          }
      eventDate == targetDate
    }
  }

  /** Formats day header label based on offset: "Today", "Yesterday", or "Mon, Mar 4". */
  private fun formatDayHeader(dayOffset: Int): String {
    val now = kotlin.time.Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val localTz = timelineTimeZone
    val today = now.toLocalDateTime(localTz).date
    val targetDate = today.subtract(dayOffset)

    return when (dayOffset) {
      0 -> "Today"
      1 -> "Yesterday"
      else -> {
        val dayOfWeek = getDayOfWeekAbbreviation(targetDate.dayOfWeek)
        val monthName = getMonthName(targetDate.month.number)
        "${dayOfWeek}, ${monthName} ${targetDate.day}"
      }
    }
  }

  private fun getDayOfWeekAbbreviation(dayOfWeek: DayOfWeek): String {
    return when (dayOfWeek) {
      DayOfWeek.MONDAY -> "Mon"
      DayOfWeek.TUESDAY -> "Tue"
      DayOfWeek.WEDNESDAY -> "Wed"
      DayOfWeek.THURSDAY -> "Thu"
      DayOfWeek.FRIDAY -> "Fri"
      DayOfWeek.SATURDAY -> "Sat"
      DayOfWeek.SUNDAY -> "Sun"
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
    return this.minus(DatePeriod(days = days))
  }

  /** One-shot event for nap creation result. Null = idle, non-null = show message then clear. */
  private val _napCreationResult: MutableStateFlow<String?> = MutableStateFlow(null)
  val napCreationResult: Flow<String?> = _napCreationResult

  /** One-shot event for refresh errors (separate from nap creation). */
  private val _refreshError: MutableStateFlow<String?> = MutableStateFlow(null)
  val refreshError: Flow<String?> = _refreshError

  /** Clears the nap creation result after it has been consumed by the UI. */
  fun clearNapCreationResult() {
    _napCreationResult.value = null
  }

  /** Clears the refresh error after it has been consumed by the UI. */
  fun clearRefreshError() {
    _refreshError.value = null
  }

  /**
   * Creates a completed nap covering a time gap. Start is [newerEventAt] minus 1 minute (the older
   * boundary of the gap), end is [olderEventAt] plus 1 minute (the newer boundary), with 1-minute
   * offsets to avoid timestamp conflicts with adjacent events.
   */
  fun createNapFromGap(newerEventAt: String, olderEventAt: String) {
    viewModelScope.launch {
      try {
        val newerMs = parseEpochMs(newerEventAt) ?: return@launch
        val olderMs = parseEpochMs(olderEventAt) ?: return@launch

        // Nap starts 1 min after the older event, ends 1 min before the newer event
        val napStartMs = olderMs + 60_000
        val napEndMs = newerMs - 60_000

        if (napEndMs <= napStartMs) {
          _napCreationResult.value = "Gap too small to add a nap"
          return@launch
        }

        val napStart = kotlin.time.Instant.fromEpochMilliseconds(napStartMs).toString()
        val napEnd = kotlin.time.Instant.fromEpochMilliseconds(napEndMs).toString()

        val request = CreateNapRequest(start_time = napStart, end_time = napEnd)
        when (val result = napsRepository.createNap(childId, request)) {
          is ApiResult.Success -> {
            syncScheduler.enqueueIfPending()
            _napCreationResult.value = "Nap added"
            refresh()
          }
          is ApiResult.Error -> {
            _napCreationResult.value = result.error.getUserMessage(context)
          }
          is ApiResult.Loading -> {}
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _napCreationResult.value = e.message ?: "Could not add nap"
      }
    }
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
