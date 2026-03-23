package net.poopyfeed.pf.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.ChartDao
import net.poopyfeed.pf.data.db.FeedingTrendDayEntity
import net.poopyfeed.pf.data.db.SleepSummaryDayEntity
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.FeedingTrendsResponse
import net.poopyfeed.pf.data.models.SleepSummaryResponse
import net.poopyfeed.pf.data.models.toApiError
import net.poopyfeed.pf.di.IoDispatcher

/**
 * Cached repository for chart data. Room is source of truth; API refreshes update Room and Flows
 * emit automatically.
 */
@Singleton
class CachedChartsRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    private val chartDao: ChartDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  /** Get feeding trends from cache as Flow. */
  fun getFeedingTrends(childId: Int, days: Int): Flow<List<FeedingTrendDayEntity>> =
      chartDao.getFeedingTrends(childId, days).flowOn(ioDispatcher)

  /** Get sleep summary from cache as Flow. */
  fun getSleepSummary(childId: Int, days: Int): Flow<List<SleepSummaryDayEntity>> =
      chartDao.getSleepSummary(childId, days).flowOn(ioDispatcher)

  /** Fetch feeding trends from API and update Room cache. */
  suspend fun refreshFeedingTrends(childId: Int, days: Int): ApiResult<FeedingTrendsResponse> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.getFeedingTrends(childId, days)
          val entities =
              response.dailyData.map { FeedingTrendDayEntity.fromDailyData(childId, days, it) }
          chartDao.clearFeedingTrends(childId, days)
          chartDao.insertFeedingTrends(entities)
          ApiResult.Success(response)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Fetch sleep summary from API and update Room cache. */
  suspend fun refreshSleepSummary(childId: Int, days: Int): ApiResult<SleepSummaryResponse> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.getSleepSummary(childId, days)
          val entities =
              response.dailyData.map { SleepSummaryDayEntity.fromDailyData(childId, days, it) }
          chartDao.clearSleepSummary(childId, days)
          chartDao.insertSleepSummary(entities)
          ApiResult.Success(response)
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}
