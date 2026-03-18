package net.poopyfeed.pf.ui.timeline

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateNapRequest
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.models.TimelineEvent
import net.poopyfeed.pf.data.models.TimelineNapPayload
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimelineViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val mockAnalyticsRepository: AnalyticsRepository = mockk()
  private val mockNapsRepository: net.poopyfeed.pf.data.repository.CachedNapsRepository = mockk()
  private val mockSyncScheduler: net.poopyfeed.pf.sync.SyncScheduler = mockk(relaxed = true)
  private val mockContext: Context = mockk(relaxed = true)
  private val mockTokenManager: TokenManager = mockk(relaxed = true)
  private lateinit var mockAnalyticsTracker: net.poopyfeed.pf.analytics.AnalyticsTracker
  private lateinit var viewModel: TimelineViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockAnalyticsTracker = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
    Dispatchers.resetMain()
  }

  private fun createViewModel(events: List<TimelineEvent> = emptyList()) {
    val savedStateHandle = SavedStateHandle().apply { set("childId", 123) }
    coEvery { mockTokenManager.getProfileTimezone() } returns null
    coEvery { mockAnalyticsRepository.getTimeline(123) } returns
        ApiResult.Success(PaginatedResponse(count = events.size, results = events))
    viewModel =
        TimelineViewModel(
            savedStateHandle,
            mockAnalyticsRepository,
            mockNapsRepository,
            mockSyncScheduler,
            mockTokenManager,
            mockContext,
            mockAnalyticsTracker)
    testDispatcher.scheduler.advanceUntilIdle()
  }

  private fun getTodayDateString(): String {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
    return today.toString()
  }

  private fun getYesterdayDateString(): String {
    val now = Instant.fromEpochMilliseconds(System.currentTimeMillis())
    val tz = TimeZone.currentSystemDefault()
    val today = now.toLocalDateTime(tz).date
    // Subtract one day
    val yesterdayMs = Instant.parse("${today}T00:00:00Z").toEpochMilliseconds() - 86_400_000L
    val yesterday = Instant.fromEpochMilliseconds(yesterdayMs).toLocalDateTime(tz).date
    return yesterday.toString()
  }

  @Test
  fun `loading events emits Ready state with correct day header`() = runTest {
    val today = getTodayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "${today}T10:00:00Z"),
        )
    createViewModel(events = events)

    val state = viewModel.uiState.first()

    // Verify Ready state exists with correct day header
    assertIs<TimelineUiState.Ready>(state)
    assertEquals("Today", state.dayHeader)
  }

  @Test
  fun `previousDay updates day offset and header`() = runTest {
    val today = getTodayDateString()
    val yesterday = getYesterdayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "${yesterday}T15:00:00Z"),
        )
    createViewModel(events = events)

    val initialState = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(initialState)
    assertEquals("Today", initialState.dayHeader)

    // Move to yesterday
    viewModel.previousDay()
    testDispatcher.scheduler.advanceUntilIdle()

    val nextState = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(nextState)
    assertEquals("Yesterday", nextState.dayHeader)
  }

  @Test
  fun `nextDay returns to today after previousDay`() = runTest {
    val today = getTodayDateString()
    val yesterday = getYesterdayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "${yesterday}T15:00:00Z"),
        )
    createViewModel(events = events)

    viewModel.previousDay()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.nextDay()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertEquals("Today", state.dayHeader)
  }

  @Test
  fun `canGoPrevious is true on day 0`() = runTest {
    val today = getTodayDateString()
    val events = listOf(TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"))
    createViewModel(events = events)

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertEquals(true, state.canGoPrevious)
  }

  @Test
  fun `canGoNext is false on day 0`() = runTest {
    val today = getTodayDateString()
    val events = listOf(TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"))
    createViewModel(events = events)

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertEquals(false, state.canGoNext)
  }

  @Test
  fun `canGoNext is true after previousDay`() = runTest {
    val today = getTodayDateString()
    val yesterday = getYesterdayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "${yesterday}T15:00:00Z"),
        )
    createViewModel(events = events)

    viewModel.previousDay()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertEquals(true, state.canGoNext)
  }

  @Test
  fun `empty events for a day shows empty list`() = runTest {
    val yesterday = getYesterdayDateString()
    val events = listOf(TestFixtures.mockTimelineEvent(at = "${yesterday}T14:00:00Z"))
    createViewModel(events = events)

    // Today has no events
    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertEquals(emptyList<TimelineItem>(), state.items)
  }

  @Test
  fun `today header with only yesterday events shows empty today list`() = runTest {
    val yesterday = getYesterdayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(at = "${yesterday}T10:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "${yesterday}T18:00:00Z"),
        )

    // Load ViewModel with only yesterday's events
    createViewModel(events = events)

    // Today page should have header "Today" and no items (no leakage from yesterday)
    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertEquals("Today", state.dayHeader)
    assertEquals(emptyList<TimelineItem>(), state.items)
  }

  @Test
  fun `API returning zero events shows Ready not Loading`() = runTest {
    createViewModel(events = emptyList())

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertEquals(emptyList<TimelineItem>(), state.items)
  }

  @Test
  fun `gap after nap uses end time not start time`() = runTest {
    val today = getTodayDateString()
    // Feeding at 15:00, nap 13:00–14:30, feeding at 10:00
    // Gap between 15:00 feeding and nap ending@14:30 = 30min (< 5 min threshold, backend returns
    // null)
    // Gap between nap start@13:00 and 10:00 feeding = 180min (3h), backend provides this
    // With gap shifting: 15:00 feeding shows nap's gap (180), nap shows 10:00's gap (null), 10:00
    // shows nothing
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T15:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null), // 30 min gap suppressed (< 5 min backend threshold)
            TestFixtures.mockTimelineEvent(
                type = "nap",
                at = "${today}T13:00:00Z",
                feeding = null,
                nap =
                    TestFixtures.mockTimelineNapPayload(
                        nappedAt = "${today}T13:00:00Z",
                        endedAt = "${today}T14:30:00Z",
                        durationMinutes = 90),
                gapAfterMinutes = 180L, // Gap to 10:00 feeding: 13:00 - 10:00
                gapAfterStart = "${today}T13:00:00Z",
                gapAfterEnd = "${today}T10:00:00Z",
                isNapEligible = true),
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T10:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null), // Last event, no gap
        )
    createViewModel(events = events)

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)

    // Should show 1 gap: 15:00 feeding displays the nap's gap of 180 min (gap shift: show next
    // event's gap)
    val gaps = state.items.filterIsInstance<TimelineItem.Gap>()
    assertEquals(
        1, gaps.size, "Expected exactly 1 gap (15:00 feeding shows nap's gap after gap shift)")
    assertEquals(180L, gaps[0].durationMinutes)
  }

  @Test
  fun `gap before nap without end time falls back to start time`() = runTest {
    val today = getTodayDateString()
    // Feeding at 15:00, ongoing nap started at 13:00 (no end time)
    // Backend calculates gap from nap start (13:00) to feeding (15:00) = 120 min
    // With gap shift: 15:00 feeding shows nap's gap (120), nap shows nothing (last event)
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T15:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null), // Last event, no gap
            TestFixtures.mockTimelineEvent(
                type = "nap",
                at = "${today}T13:00:00Z",
                feeding = null,
                nap =
                    TimelineNapPayload(
                        id = 1,
                        nappedAt = "${today}T13:00:00Z",
                        endedAt = null,
                        durationMinutes = null),
                gapAfterMinutes = 120L, // Gap from nap start (13:00) to feeding (15:00)
                gapAfterStart = "${today}T13:00:00Z",
                gapAfterEnd = "${today}T15:00:00Z",
                isNapEligible = true),
        )
    createViewModel(events = events)

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)

    val gaps = state.items.filterIsInstance<TimelineItem.Gap>()
    assertEquals(1, gaps.size, "Expected 1 gap (15:00 feeding shows nap's gap after gap shift)")
    assertEquals(120L, gaps[0].durationMinutes)
  }

  @Test
  fun `gap before nap uses nappedAt when at equals endedAt`() = runTest {
    val today = getTodayDateString()
    // Backend may send TimelineEvent.at equal to nap ended_at for analytics.
    // Feeding at 15:00, nap 13:00–14:30 (at = 14:30), feeding at 10:00.
    // Gap between 15:00 feeding and nap end (14:30) = 30min (< 5 min threshold, suppressed)
    // Gap between nap start (13:00) and 10:00 feeding = 180min (3h), backend provides this
    // With gap shift: 15:00 feeding shows nap's gap (180), nap shows 10:00's gap (null), 10:00
    // shows nothing
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T15:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null), // 30 min gap suppressed
            TimelineEvent(
                type = "nap",
                at = "${today}T14:30:00Z", // ended_at used for at
                feeding = null,
                diaper = null,
                nap =
                    TimelineNapPayload(
                        id = 1,
                        nappedAt = "${today}T13:00:00Z",
                        endedAt = "${today}T14:30:00Z",
                        durationMinutes = 90),
                gapAfterMinutes = 180L, // Gap from nap start (13:00) to feeding (10:00)
                gapAfterStart = "${today}T13:00:00Z",
                gapAfterEnd = "${today}T10:00:00Z",
                isNapEligible = true),
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T10:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null), // Last event, no gap
        )
    createViewModel(events = events)

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)

    val gaps = state.items.filterIsInstance<TimelineItem.Gap>()
    assertEquals(
        1, gaps.size, "Expected exactly 1 gap (15:00 feeding shows nap's gap after gap shift)")
    assertEquals(180L, gaps[0].durationMinutes)
  }

  @Test
  fun `createNapFromGap with gap too small emits message and does not call repository`() = runTest {
    createViewModel(events = emptyList())
    // Gap of 1 min: nap would be older+1min to newer-1min = same instant → invalid
    val newerEventAt = "2024-01-15T12:01:00Z"
    val olderEventAt = "2024-01-15T12:00:00Z"

    viewModel.createNapFromGap(newerEventAt, olderEventAt)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Gap too small to add a nap", viewModel.napCreationResult.first())
    coVerify(exactly = 0) { mockNapsRepository.createNap(any(), any()) }
  }

  @Test
  fun `createNapFromGap with exactly 2 min gap emits gap too small and does not call repository`() =
      runTest {
        createViewModel(events = emptyList())
        // 2 min gap: nap 12:01 to 12:01 → duration 0 → invalid
        val newerEventAt = "2024-01-15T12:02:00Z"
        val olderEventAt = "2024-01-15T12:00:00Z"

        viewModel.createNapFromGap(newerEventAt, olderEventAt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Gap too small to add a nap", viewModel.napCreationResult.first())
        coVerify(exactly = 0) { mockNapsRepository.createNap(any(), any()) }
      }

  @Test
  fun `createNapFromGap when API returns error emits error message`() = runTest {
    createViewModel(events = emptyList())
    coEvery { mockNapsRepository.createNap(123, any()) } returns
        ApiResult.Error(ApiError.NetworkError("offline"))
    coEvery { mockContext.getString(any<Int>()) } returns "Network error"
    val newerEventAt = "2024-01-15T11:00:00Z"
    val olderEventAt = "2024-01-15T10:00:00Z"

    viewModel.createNapFromGap(newerEventAt, olderEventAt)
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals("Network error", viewModel.napCreationResult.first())
    coVerify(exactly = 1) { mockNapsRepository.createNap(123, any()) }
  }

  @Test
  fun `refresh calls getTimeline again`() = runTest {
    createViewModel(events = emptyList())
    coVerify(exactly = 1) { mockAnalyticsRepository.getTimeline(123) }

    viewModel.refresh()
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 2) { mockAnalyticsRepository.getTimeline(123) }
  }

  @Test
  fun `createNapFromGap with invalid timestamps does not call repository or set result`() =
      runTest {
        createViewModel(events = emptyList())
        viewModel.createNapFromGap("not-a-valid-iso-date", "2024-01-15T10:00:00Z")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockNapsRepository.createNap(any(), any()) }
        assertEquals(null, viewModel.napCreationResult.first())
      }

  @Test
  fun `clearNapCreationResult clears nap creation result after success`() = runTest {
    createViewModel(events = emptyList())
    coEvery { mockNapsRepository.createNap(123, any()) } returns
        ApiResult.Success(TestFixtures.mockNap(id = 1))
    viewModel.createNapFromGap("2024-01-15T11:00:00Z", "2024-01-15T10:00:00Z")
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals("Nap added", viewModel.napCreationResult.first())

    viewModel.clearNapCreationResult()
    assertEquals(null, viewModel.napCreationResult.first())
  }

  @Test
  fun `day header for offset 2 or more shows weekday and date`() = runTest {
    val today = getTodayDateString()
    val yesterday = getYesterdayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "${yesterday}T15:00:00Z"),
        )
    createViewModel(events = events)
    viewModel.previousDay()
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.previousDay()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertTrue(
        state.dayHeader != "Today" && state.dayHeader != "Yesterday",
        "Day 2+ should show weekday date, got: ${state.dayHeader}")
    assertTrue(
        state.dayHeader.matches(
            Regex(
                "^(Mon|Tue|Wed|Thu|Fri|Sat|Sun), (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec) \\d{1,2}$")),
        "Day header should match 'Mon, Mar 4' pattern, got: ${state.dayHeader}")
  }

  @Test
  fun `previousDay does not go past day 6`() = runTest {
    val today = getTodayDateString()
    val yesterday = getYesterdayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "${yesterday}T15:00:00Z"),
        )
    createViewModel(events = events)
    repeat(7) {
      viewModel.previousDay()
      testDispatcher.scheduler.advanceUntilIdle()
    }

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertFalse(state.canGoPrevious, "At day 6, canGoPrevious should be false")
  }

  @Test
  fun `nextDay on day 0 does nothing`() = runTest {
    val today = getTodayDateString()
    val events = listOf(TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"))
    createViewModel(events = events)
    val stateBefore = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(stateBefore)
    assertFalse(stateBefore.canGoNext)

    viewModel.nextDay()
    testDispatcher.scheduler.advanceUntilIdle()
    val stateAfter = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(stateAfter)
    assertEquals("Today", stateAfter.dayHeader)
    assertFalse(stateAfter.canGoNext)
  }

  @Test
  fun `gap of exactly 60 minutes does not show Add nap button`() = runTest {
    val today = getTodayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T14:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null),
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T10:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = 60L,
                gapAfterStart = "${today}T14:00:00Z",
                gapAfterEnd = "${today}T10:00:00Z"),
        )
    createViewModel(events = events)
    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    val gaps = state.items.filterIsInstance<TimelineItem.Gap>()
    assertEquals(1, gaps.size)
    assertEquals(60L, gaps[0].durationMinutes)
    assertFalse(gaps[0].showAddNapButton)
  }

  @Test
  fun `events with unparseable at are excluded from day items`() = runTest {
    val today = getTodayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(at = "${today}T10:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "not-a-valid-datetime"),
        )
    createViewModel(events = events)
    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    val eventItems = state.items.filterIsInstance<TimelineItem.Event>()
    assertEquals(1, eventItems.size, "Unparseable event.at should be excluded")
    assertEquals("${today}T10:00:00Z", eventItems[0].event.at)
  }

  @Test
  fun `gap with backend sending start before end still yields correct newer and older for Add nap`() =
      runTest {
        val today = getTodayDateString()
        // Backend sends gap in chronological order: gap_after_start = older, gap_after_end = newer
        val events =
            listOf(
                TestFixtures.mockTimelineEvent(
                    type = "feeding",
                    at = "${today}T14:00:00Z",
                    feeding = TestFixtures.mockTimelineFeedingPayload(),
                    gapAfterMinutes = null),
                TestFixtures.mockTimelineEvent(
                    type = "feeding",
                    at = "${today}T10:00:00Z",
                    feeding = TestFixtures.mockTimelineFeedingPayload(),
                    gapAfterMinutes = 240L,
                    gapAfterStart = "${today}T10:00:00Z",
                    gapAfterEnd = "${today}T14:00:00Z"),
            )
        createViewModel(events = events)
        val state = viewModel.uiState.first()
        assertIs<TimelineUiState.Ready>(state)
        val gaps = state.items.filterIsInstance<TimelineItem.Gap>()
        assertEquals(1, gaps.size)
        assertEquals("${today}T14:00:00Z", gaps[0].newerEventAt)
        assertEquals("${today}T10:00:00Z", gaps[0].olderEventAt)
        assertTrue(gaps[0].showAddNapButton)
      }

  @Test
  fun `createNapFromGap with valid gap calls repository with 1 min buffers and emits Nap added`() =
      runTest {
        createViewModel(events = emptyList())
        coEvery { mockNapsRepository.createNap(123, any()) } returns
            ApiResult.Success(TestFixtures.mockNap(id = 1))
        // 60 min gap: 10:00 to 11:00 → nap 10:01 to 10:59
        val newerEventAt = "2024-01-15T11:00:00Z"
        val olderEventAt = "2024-01-15T10:00:00Z"

        viewModel.createNapFromGap(newerEventAt, olderEventAt)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Nap added", viewModel.napCreationResult.first())
        val requestSlot = slot<CreateNapRequest>()
        coVerify { mockNapsRepository.createNap(123, capture(requestSlot)) }
        assertEquals("2024-01-15T10:01:00Z", requestSlot.captured.start_time)
        assertEquals("2024-01-15T10:59:00Z", requestSlot.captured.end_time)
      }

  @Test
  fun `all gaps from backend are displayed regardless of duration`() = runTest {
    val today = getTodayDateString()
    // Backend sends gap of 59 min; we show all gaps the backend provides
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T14:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null),
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T10:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = 59L,
                gapAfterStart = "${today}T14:00:00Z",
                gapAfterEnd = "${today}T10:00:00Z"),
        )
    createViewModel(events = events)

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    val gaps = state.items.filterIsInstance<TimelineItem.Gap>()
    assertEquals(1, gaps.size)
    assertEquals(59L, gaps[0].durationMinutes)
    assertFalse(gaps[0].showAddNapButton, "Nap button should be hidden for gaps <= 60 min")
  }

  @Test
  fun `gap over 60 minutes shows Add nap button`() = runTest {
    val today = getTodayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T14:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null),
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T10:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = 61L,
                gapAfterStart = "${today}T14:00:00Z",
                gapAfterEnd = "${today}T10:00:00Z"),
        )
    createViewModel(events = events)

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    val gaps = state.items.filterIsInstance<TimelineItem.Gap>()
    assertEquals(1, gaps.size)
    assertEquals(61L, gaps[0].durationMinutes)
    assertTrue(gaps[0].showAddNapButton, "Nap button should be shown for gaps over 60 min")
  }

  @Test
  fun `API error shows Error state`() = runTest {
    val savedStateHandle = SavedStateHandle().apply { set("childId", 123) }
    coEvery { mockAnalyticsRepository.getTimeline(123) } returns
        ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("offline"))
    coEvery { mockContext.getString(any<Int>()) } returns "Network error"
    viewModel =
        TimelineViewModel(
            savedStateHandle,
            mockAnalyticsRepository,
            mockNapsRepository,
            mockSyncScheduler,
            mockTokenManager,
            mockContext,
            mockAnalyticsTracker)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Error>(state)
  }

  @Test
  fun `refresh failure after successful load keeps existing data and shows transient error`() =
      runTest {
        val today = getTodayDateString()
        val events =
            listOf(
                TestFixtures.mockTimelineEvent(at = "${today}T14:00:00Z"),
                TestFixtures.mockTimelineEvent(at = "${today}T10:00:00Z"),
            )
        createViewModel(events = events)

        // Verify initial load succeeded
        val initialState = viewModel.uiState.first()
        assertIs<TimelineUiState.Ready>(initialState)
        val initialEventCount = initialState.items.filterIsInstance<TimelineItem.Event>().size
        assertTrue(initialEventCount > 0, "Should have events after initial load")

        // Now make the API return an error on refresh
        coEvery { mockAnalyticsRepository.getTimeline(123) } returns
            ApiResult.Error(ApiError.NetworkError("offline"))
        coEvery { mockContext.getString(any<Int>()) } returns "Network error"

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        // Timeline should still show existing data (Ready), NOT Error state
        val stateAfterRefresh = viewModel.uiState.first()
        assertIs<TimelineUiState.Ready>(stateAfterRefresh)
        val eventCountAfterRefresh =
            stateAfterRefresh.items.filterIsInstance<TimelineItem.Event>().size
        assertEquals(
            initialEventCount,
            eventCountAfterRefresh,
            "Existing events should be preserved after refresh failure")

        // Error should be surfaced as a transient message (refreshError, not napCreationResult)
        assertEquals("Network error", viewModel.refreshError.first())
      }

  @Test
  fun `createNapFromGap offline keeps timeline visible and shows error toast`() = runTest {
    val today = getTodayDateString()
    val events =
        listOf(
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T14:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = null),
            TestFixtures.mockTimelineEvent(
                type = "feeding",
                at = "${today}T10:00:00Z",
                feeding = TestFixtures.mockTimelineFeedingPayload(),
                gapAfterMinutes = 240L,
                gapAfterStart = "${today}T14:00:00Z",
                gapAfterEnd = "${today}T10:00:00Z"),
        )
    createViewModel(events = events)

    // Nap creation succeeds (offline mode), but timeline refresh fails
    coEvery { mockNapsRepository.createNap(123, any()) } returns
        ApiResult.Success(TestFixtures.mockNap(id = 1))
    coEvery { mockAnalyticsRepository.getTimeline(123) } returns
        ApiResult.Error(ApiError.NetworkError("offline"))
    coEvery { mockContext.getString(any<Int>()) } returns "Network error"

    viewModel.createNapFromGap("${today}T14:00:00Z", "${today}T10:00:00Z")
    testDispatcher.scheduler.advanceUntilIdle()

    // Timeline should still show Ready state with existing data
    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertTrue(
        state.items.filterIsInstance<TimelineItem.Event>().isNotEmpty(),
        "Timeline should still show events after offline nap creation")
  }
}
