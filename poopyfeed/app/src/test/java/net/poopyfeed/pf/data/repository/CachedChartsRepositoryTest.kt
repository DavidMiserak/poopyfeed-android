package net.poopyfeed.pf.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.ChartDao
import net.poopyfeed.pf.data.db.FeedingTrendDayEntity
import net.poopyfeed.pf.data.db.SleepSummaryDayEntity
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.DailyData
import net.poopyfeed.pf.data.models.FeedingTrendsResponse
import net.poopyfeed.pf.data.models.SleepSummaryResponse
import net.poopyfeed.pf.data.models.WeeklySummaryData
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedChartsRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var chartDao: ChartDao
  private lateinit var repository: CachedChartsRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = mockk()
    chartDao = mockk(relaxed = true)
    repository = CachedChartsRepository(apiService, chartDao, testDispatcher)
  }

  @Test
  fun `getFeedingTrends returns cached data from Room`() = runTest {
    val cached =
        listOf(
            FeedingTrendDayEntity(
                id = 1,
                childId = 1,
                date = "2026-03-01",
                count = 5,
                average_duration = 12,
                total_oz = 18.5,
                period = 30))
    coEvery { chartDao.getFeedingTrends(1, 30) } returns flowOf(cached)

    val results = repository.getFeedingTrends(1, 30).toList()

    assertEquals(1, results.size)
    assertEquals(1, results[0].size)
    assertEquals(5, results[0][0].count)
  }

  @Test
  fun `getFeedingTrends returns empty list when no cache`() = runTest {
    coEvery { chartDao.getFeedingTrends(1, 30) } returns flowOf(emptyList())

    val results = repository.getFeedingTrends(1, 30).toList()

    assertEquals(1, results.size)
    assertEquals(0, results[0].size)
  }

  @Test
  fun `refreshFeedingTrends clears old data and inserts new`() = runTest {
    val response = makeFeedingTrendsResponse()
    coEvery { apiService.getFeedingTrends(1, 30) } returns response

    val result = repository.refreshFeedingTrends(1, 30)

    assertIs<ApiResult.Success<FeedingTrendsResponse>>(result)
    coVerify(exactly = 1) { chartDao.clearFeedingTrends(1, 30) }
    coVerify(exactly = 1) { chartDao.insertFeedingTrends(any()) }
  }

  @Test
  fun `refreshFeedingTrends returns error on API failure without touching Room`() = runTest {
    coEvery { apiService.getFeedingTrends(1, 30) } throws java.io.IOException("network")

    val result = repository.refreshFeedingTrends(1, 30)

    assertIs<ApiResult.Error<FeedingTrendsResponse>>(result)
    coVerify(exactly = 0) { chartDao.clearFeedingTrends(any(), any()) }
    coVerify(exactly = 0) { chartDao.insertFeedingTrends(any()) }
  }

  @Test
  fun `getSleepSummary returns cached data from Room`() = runTest {
    val cached =
        listOf(
            SleepSummaryDayEntity(
                id = 1,
                childId = 1,
                date = "2026-03-01",
                count = 2,
                total_minutes = 120,
                period = 30))
    coEvery { chartDao.getSleepSummary(1, 30) } returns flowOf(cached)

    val results = repository.getSleepSummary(1, 30).toList()

    assertEquals(1, results.size)
    assertEquals(2, results[0][0].count)
  }

  @Test
  fun `refreshSleepSummary clears old data and inserts new`() = runTest {
    val response = makeSleepSummaryResponse()
    coEvery { apiService.getSleepSummary(1, 30) } returns response

    val result = repository.refreshSleepSummary(1, 30)

    assertIs<ApiResult.Success<SleepSummaryResponse>>(result)
    coVerify(exactly = 1) { chartDao.clearSleepSummary(1, 30) }
    coVerify(exactly = 1) { chartDao.insertSleepSummary(any()) }
  }

  @Test
  fun `refreshSleepSummary returns error on API failure without touching Room`() = runTest {
    coEvery { apiService.getSleepSummary(1, 30) } throws java.io.IOException("network")

    val result = repository.refreshSleepSummary(1, 30)

    assertIs<ApiResult.Error<SleepSummaryResponse>>(result)
    coVerify(exactly = 0) { chartDao.clearSleepSummary(any(), any()) }
    coVerify(exactly = 0) { chartDao.insertSleepSummary(any()) }
  }

  private fun makeFeedingTrendsResponse() =
      FeedingTrendsResponse(
          period = "30",
          childId = 1,
          dailyData =
              listOf(
                  DailyData(date = "2026-03-01", count = 5, averageDuration = 12, totalOz = 18.5)),
          weeklySummary = WeeklySummaryData(avgPerDay = 5.2, trend = "increasing", variance = 1.3),
          lastUpdated = "2026-03-23T10:30:00Z",
      )

  private fun makeSleepSummaryResponse() =
      SleepSummaryResponse(
          period = "30",
          childId = 1,
          dailyData = listOf(DailyData(date = "2026-03-01", count = 2, totalMinutes = 120)),
          weeklySummary = WeeklySummaryData(avgPerDay = 2.1, trend = "stable", variance = 0.5),
          lastUpdated = "2026-03-23T10:30:00Z",
      )
}
