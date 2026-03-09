package net.poopyfeed.pf.ui.timeline

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
import net.poopyfeed.pf.data.models.ApiResult
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
  private lateinit var viewModel: TimelineViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
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
            mockContext)
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
    // Gap between 15:00 feeding and nap ending@14:30 = 30min (< 5 min threshold, backend returns null)
    // Gap between nap start@13:00 and 10:00 feeding = 180min (3h), backend provides this
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

    // Should show 1 gap: between nap (13:00) and 10:00 feeding (180 min > 60 threshold)
    val gaps = state.items.filterIsInstance<TimelineItem.Gap>()
    assertEquals(1, gaps.size, "Expected exactly 1 gap (between nap and 10:00 feeding)")
    assertEquals(180L, gaps[0].durationMinutes)
  }

  @Test
  fun `gap before nap without end time falls back to start time`() = runTest {
    val today = getTodayDateString()
    // Feeding at 15:00, ongoing nap started at 13:00 (no end time)
    // Backend calculates gap from nap start (13:00) to feeding (15:00) = 120 min
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
    assertEquals(1, gaps.size, "Expected 1 gap (ongoing nap uses start time as fallback)")
    assertEquals(120L, gaps[0].durationMinutes)
  }

  @Test
  fun `gap before nap uses nappedAt when at equals endedAt`() = runTest {
    val today = getTodayDateString()
    // Backend may send TimelineEvent.at equal to nap ended_at for analytics.
    // Feeding at 15:00, nap 13:00–14:30 (at = 14:30), feeding at 10:00.
    // Gap between 15:00 feeding and nap end (14:30) = 30min (< 5 min threshold, suppressed)
    // Gap between nap start (13:00) and 10:00 feeding = 180min (3h), backend provides this
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
    assertEquals(1, gaps.size, "Expected exactly 1 gap (between nap start and 10:00 feeding)")
    assertEquals(180L, gaps[0].durationMinutes)
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
            mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Error>(state)
  }
}
