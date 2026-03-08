package net.poopyfeed.pf.data.repository

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.PatternAlertsResponse
import net.poopyfeed.pf.data.models.toApiError
import net.poopyfeed.pf.di.IoDispatcher

/**
 * Network-only repository for analytics data (pattern alerts, etc.). No local cache; server caches
 * for 15 minutes.
 */
class AnalyticsRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  /** Get pattern alerts for a child (feeding and nap pattern warnings). */
  suspend fun getPatternAlerts(childId: Int): ApiResult<PatternAlertsResponse> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.getPatternAlerts(childId)
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}
