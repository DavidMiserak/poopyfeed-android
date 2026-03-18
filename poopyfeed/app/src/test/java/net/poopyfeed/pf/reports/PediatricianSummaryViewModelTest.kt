package net.poopyfeed.pf.reports

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.SummaryDiapers
import net.poopyfeed.pf.data.models.SummaryFeedings
import net.poopyfeed.pf.data.models.SummarySleep
import net.poopyfeed.pf.data.models.WeeklySummary
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PediatricianSummaryViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockRepo: AnalyticsRepository
  private lateinit var mockTracker: AnalyticsTracker

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepo = mockk(relaxed = true)
    mockTracker = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(childId: Int = 1): PediatricianSummaryViewModel {
    val handle = SavedStateHandle(mapOf("childId" to childId))
    return PediatricianSummaryViewModel(handle, mockRepo, mockTracker)
  }

  @Test
  fun `initial state is Loading`() = runTest {
    coEvery { mockRepo.getWeeklySummary(1) } returns
        ApiResult.Success(TestFixtures.mockWeeklySummary())

    val vm = createViewModel()
    // Before advancing, state should be Loading
    assertTrue(vm.uiState.value is PediatricianSummaryUiState.Loading)
  }

  @Test
  fun `load success emits Ready with daily averages`() = runTest {
    val summary =
        WeeklySummary(
            childId = 1,
            period = "2026-03-04 to 2026-03-11",
            feedings = SummaryFeedings(count = 35, totalOz = 87.5),
            diapers = SummaryDiapers(count = 28, wet = 14, dirty = 10, both = 4),
            sleep = SummarySleep(naps = 14, totalMinutes = 840),
        )
    coEvery { mockRepo.getWeeklySummary(1) } returns ApiResult.Success(summary)

    val vm = createViewModel()
    advanceUntilIdle()

    val state = assertIs<PediatricianSummaryUiState.Ready>(vm.uiState.first())
    assertEquals(summary, state.summary)
    assertEquals(5, state.feedingsPerDay) // 35 / 7
    assertEquals(12.5, state.ozPerDay) // 87.5 / 7.0
    assertEquals(4, state.diapersPerDay) // 28 / 7
    assertEquals(2, state.napsPerDay) // 14 / 7
    assertEquals(120, state.sleepMinutesPerDay) // 840 / 7
  }

  @Test
  fun `load empty data emits Empty`() = runTest {
    val emptySummary = WeeklySummary(childId = 1)
    coEvery { mockRepo.getWeeklySummary(1) } returns ApiResult.Success(emptySummary)

    val vm = createViewModel()
    advanceUntilIdle()

    assertIs<PediatricianSummaryUiState.Empty>(vm.uiState.first())
  }

  @Test
  fun `load error emits Error with message`() = runTest {
    coEvery { mockRepo.getWeeklySummary(1) } returns
        ApiResult.Error(ApiError.NetworkError("No internet"))

    val vm = createViewModel()
    advanceUntilIdle()

    val state = assertIs<PediatricianSummaryUiState.Error>(vm.uiState.first())
    assertEquals("No internet", state.message)
  }

  @Test
  fun `refresh reloads data`() = runTest {
    coEvery { mockRepo.getWeeklySummary(1) } returns
        ApiResult.Success(TestFixtures.mockWeeklySummary())

    val vm = createViewModel()
    advanceUntilIdle()

    vm.refresh()
    advanceUntilIdle()

    coVerify(exactly = 2) { mockRepo.getWeeklySummary(1) }
  }

  @Test
  fun `childId is extracted from SavedStateHandle`() = runTest {
    coEvery { mockRepo.getWeeklySummary(42) } returns
        ApiResult.Success(TestFixtures.mockWeeklySummary(childId = 42))

    val vm = createViewModel(childId = 42)
    assertEquals(42, vm.childId)
    advanceUntilIdle()

    coVerify { mockRepo.getWeeklySummary(42) }
  }
}
