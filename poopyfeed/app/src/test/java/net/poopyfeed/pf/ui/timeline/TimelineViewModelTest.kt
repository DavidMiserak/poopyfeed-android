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
import net.poopyfeed.pf.data.repository.AnalyticsRepository
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
    coEvery { mockAnalyticsRepository.getTimeline(123) } returns
        ApiResult.Success(PaginatedResponse(count = events.size, results = events))
    viewModel =
        TimelineViewModel(
            savedStateHandle,
            mockAnalyticsRepository,
            mockNapsRepository,
            mockSyncScheduler,
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
  fun `API returning zero events shows Ready not Loading`() = runTest {
    createViewModel(events = emptyList())

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Ready>(state)
    assertEquals(emptyList<TimelineItem>(), state.items)
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
            mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.first()
    assertIs<TimelineUiState.Error>(state)
  }
}
