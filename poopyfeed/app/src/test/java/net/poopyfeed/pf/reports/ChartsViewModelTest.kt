package net.poopyfeed.pf.reports

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.db.FeedingTrendDayEntity
import net.poopyfeed.pf.data.db.SleepSummaryDayEntity
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChartsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChartsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockRepo: CachedChartsRepository
  private val feedingFlow = MutableStateFlow<List<FeedingTrendDayEntity>>(emptyList())
  private val sleepFlow = MutableStateFlow<List<SleepSummaryDayEntity>>(emptyList())

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockRepo = mockk(relaxed = true)
    every { mockRepo.getFeedingTrends(1, any()) } returns feedingFlow
    every { mockRepo.getSleepSummary(1, any()) } returns sleepFlow
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel(childId: Int = 1): ChartsViewModel {
    val handle = SavedStateHandle(mapOf("childId" to childId))
    return ChartsViewModel(handle, mockRepo)
  }

  @Test
  fun `initial state is Loading`() = runTest {
    val vm = createViewModel()
    assertIs<ChartUiState.Loading>(vm.feedingTrendsState.value)
    assertIs<ChartUiState.Loading>(vm.sleepSummaryState.value)
  }

  @Test
  fun `loadCharts emits Ready when cache has data and API succeeds`() = runTest {
    val feedingResponse = TestFixtures.mockFeedingTrendsResponse()
    val sleepResponse = TestFixtures.mockSleepSummaryResponse()
    coEvery { mockRepo.refreshFeedingTrends(1, 30) } returns ApiResult.Success(feedingResponse)
    coEvery { mockRepo.refreshSleepSummary(1, 30) } returns ApiResult.Success(sleepResponse)

    val vm = createViewModel()
    vm.loadCharts(30)
    advanceUntilIdle()

    feedingFlow.value = TestFixtures.mockFeedingTrendDayEntities()
    advanceUntilIdle()

    val state = vm.feedingTrendsState.first()
    assertIs<ChartUiState.Ready<List<FeedingTrendDayEntity>>>(state)
    assertEquals(2, state.data.size)
  }

  @Test
  fun `loadCharts shows Error when API fails and no cache`() = runTest {
    coEvery { mockRepo.refreshFeedingTrends(1, 30) } returns
        ApiResult.Error(ApiError.NetworkError("fail"))
    coEvery { mockRepo.refreshSleepSummary(1, 30) } returns
        ApiResult.Error(ApiError.NetworkError("fail"))

    val vm = createViewModel()
    vm.loadCharts(30)
    advanceUntilIdle()

    val state = vm.feedingTrendsState.first()
    assertIs<ChartUiState.Error>(state)
    assertEquals("fail", state.message)
  }

  @Test
  fun `loadCharts keeps stale cache when API fails`() = runTest {
    coEvery { mockRepo.refreshFeedingTrends(1, 30) } returns
        ApiResult.Error(ApiError.NetworkError("fail"))
    coEvery { mockRepo.refreshSleepSummary(1, 30) } returns
        ApiResult.Error(ApiError.NetworkError("fail"))

    feedingFlow.value = TestFixtures.mockFeedingTrendDayEntities()

    val vm = createViewModel()
    vm.loadCharts(30)
    advanceUntilIdle()

    val state = vm.feedingTrendsState.first()
    assertIs<ChartUiState.Ready<List<FeedingTrendDayEntity>>>(state)
    assertEquals(2, state.data.size)
  }

  @Test
  fun `refresh updates isRefreshing state`() = runTest {
    val feedingResponse = TestFixtures.mockFeedingTrendsResponse()
    val sleepResponse = TestFixtures.mockSleepSummaryResponse()
    coEvery { mockRepo.refreshFeedingTrends(1, 30) } returns ApiResult.Success(feedingResponse)
    coEvery { mockRepo.refreshSleepSummary(1, 30) } returns ApiResult.Success(sleepResponse)

    val vm = createViewModel()
    vm.loadCharts(30)
    advanceUntilIdle()

    assertEquals(false, vm.isRefreshing.first())
  }

  @Test
  fun `loadCharts with different days re-collects from Room`() = runTest {
    val feedingResponse7 = TestFixtures.mockFeedingTrendsResponse(period = "7")
    val sleepResponse7 = TestFixtures.mockSleepSummaryResponse(period = "7")
    coEvery { mockRepo.refreshFeedingTrends(1, 7) } returns ApiResult.Success(feedingResponse7)
    coEvery { mockRepo.refreshSleepSummary(1, 7) } returns ApiResult.Success(sleepResponse7)

    every { mockRepo.getFeedingTrends(1, 7) } returns feedingFlow
    every { mockRepo.getSleepSummary(1, 7) } returns sleepFlow

    val vm = createViewModel()
    vm.loadCharts(7)
    advanceUntilIdle()

    feedingFlow.value = TestFixtures.mockFeedingTrendDayEntities(period = 7)
    advanceUntilIdle()

    val state = vm.feedingTrendsState.first()
    assertIs<ChartUiState.Ready<List<FeedingTrendDayEntity>>>(state)
  }
}
