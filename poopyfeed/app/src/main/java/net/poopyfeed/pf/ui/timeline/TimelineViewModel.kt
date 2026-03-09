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
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
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

  data class Gap(
      val durationMinutes: Long,
      val newerEventAt: String,
      val olderEventAt: String,
  ) : TimelineItem
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
) : ViewModel() {

  companion object {
    /** Minimum gap between events (in minutes) before showing a gap indicator. */
    const val GAP_THRESHOLD_MINUTES = 60L
  }

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  /** All events fetched from API (newest-first, same order as server). */
  private val _allEvents: MutableStateFlow<List<TimelineEvent>> = MutableStateFlow(emptyList())

  /** Day offset: 0 = today, 1 = yesterday, ..., 6 = oldest allowed day. */
  private val _dayOffset: MutableStateFlow<Int> = MutableStateFlow(0)

  /** Tracks fetch lifecycle: null = loading, empty = loaded OK, non-empty = error message. */
  private val _fetchStatus: MutableStateFlow<String?> = MutableStateFlow(null)

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

  /** Loads all timeline events from API and updates [_allEvents]. */
  private fun loadTimeline() {
    _fetchStatus.value = null // loading
    viewModelScope.launch {
      when (val result = analyticsRepository.getTimeline(childId)) {
        is ApiResult.Success -> {
          _allEvents.value = result.data.results
          _fetchStatus.value = "" // loaded OK
        }
        is ApiResult.Error -> {
          _allEvents.value = emptyList()
          _fetchStatus.value = result.error.getUserMessage(context)
        }
        is ApiResult.Loading -> {
          // no-op; we manage Loading locally
        }
      }
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
   * Interleaves [TimelineItem.Gap] markers between consecutive events when the time difference
   * exceeds [GAP_THRESHOLD_MINUTES]. Events are newest-first, so each gap represents the quiet
   * period between the newer event and the older one below it.
   */
  private fun interleaveGaps(events: List<TimelineEvent>): List<TimelineItem> {
    if (events.isEmpty()) return emptyList()
    val items = mutableListOf<TimelineItem>()
    for (i in events.indices) {
      items.add(TimelineItem.Event(events[i]))
      if (i < events.lastIndex) {
        val newerMs = parseEpochMs(events[i].at)
        // For naps, use end time as the effective boundary (nap occupies time until it ends)
        val olderEvent = events[i + 1]
        val olderEffectiveEnd = olderEvent.nap?.endedAt ?: olderEvent.at
        val olderMs = parseEpochMs(olderEffectiveEnd)
        if (newerMs != null && olderMs != null) {
          val gapMinutes = (newerMs - olderMs) / 60_000
          if (gapMinutes >= GAP_THRESHOLD_MINUTES) {
            items.add(
                TimelineItem.Gap(
                    durationMinutes = gapMinutes,
                    newerEventAt = events[i].at,
                    olderEventAt = olderEffectiveEnd,
                ))
          }
        }
      }
    }
    return items
  }

  private fun parseEpochMs(isoString: String): Long? {
    return try {
      Instant.parse(isoString).toEpochMilliseconds()
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Filters events for a specific day. Computes the local date for (today - dayOffset) in
   * [timelineTimeZone] and includes only events whose UTC "at" timestamp converts to that same
   * local date. This avoids relying on the raw ISO date prefix, which may differ from the user's
   * local day near midnight in non-UTC timezones.
   */
  private fun filterEventsByDay(events: List<TimelineEvent>, dayOffset: Int): List<TimelineEvent> {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val localTz = timelineTimeZone
    val today = now.toLocalDateTime(localTz).date
    val targetDate = today.subtract(dayOffset)

    // Filter events that fall on targetDate in the user's profile timezone (newest-first, matching
    // server order)
    return events.filter { event ->
      val eventDate =
          try {
            Instant.parse(event.at).toLocalDateTime(localTz).date
          } catch (_: Exception) {
            null
          }
      eventDate == targetDate
    }
  }

  /** Formats day header label based on offset: "Today", "Yesterday", or "Mon, Mar 4". */
  private fun formatDayHeader(dayOffset: Int): String {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val localTz = timelineTimeZone
    val today = now.toLocalDateTime(localTz).date
    val targetDate = today.subtract(dayOffset)

    return when (dayOffset) {
      0 -> "Today"
      1 -> "Yesterday"
      else -> {
        val dayOfWeek = getDayOfWeekAbbreviation(targetDate.dayOfWeek)
        val monthName = getMonthName(targetDate.monthNumber)
        "${dayOfWeek}, ${monthName} ${targetDate.dayOfMonth}"
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

  /** Clears the nap creation result after it has been consumed by the UI. */
  fun clearNapCreationResult() {
    _napCreationResult.value = null
  }

  /**
   * Creates a completed nap covering a time gap. Start is [newerEventAt] minus 1 minute (the older
   * boundary of the gap), end is [olderEventAt] plus 1 minute (the newer boundary), with 1-minute
   * offsets to avoid timestamp conflicts with adjacent events.
   */
  fun createNapFromGap(newerEventAt: String, olderEventAt: String) {
    viewModelScope.launch {
      val newerMs = parseEpochMs(newerEventAt) ?: return@launch
      val olderMs = parseEpochMs(olderEventAt) ?: return@launch

      // Nap starts 1 min after the older event, ends 1 min before the newer event
      val napStartMs = olderMs + 60_000
      val napEndMs = newerMs - 60_000

      if (napEndMs <= napStartMs) {
        _napCreationResult.value = "Gap too small to add a nap"
        return@launch
      }

      val napStart = Instant.fromEpochMilliseconds(napStartMs).toString()
      val napEnd = Instant.fromEpochMilliseconds(napEndMs).toString()

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
