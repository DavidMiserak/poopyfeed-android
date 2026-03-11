package net.poopyfeed.pf.data.repository

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.ExportJobResponse
import net.poopyfeed.pf.data.models.ExportPdfRequest
import net.poopyfeed.pf.data.models.JobStatusResponse
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.models.PatternAlertsResponse
import net.poopyfeed.pf.data.models.TimelineEvent
import net.poopyfeed.pf.data.models.WeeklySummary
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

  /** Get timeline events for a child (merged feedings, diapers, naps). */
  suspend fun getTimeline(
      childId: Int,
      page: Int = 1,
      pageSize: Int = 100,
  ): ApiResult<PaginatedResponse<TimelineEvent>> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.getTimeline(childId, page, pageSize)
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Export CSV for a child. Returns raw response body for file saving. */
  suspend fun exportCsv(childId: Int, days: Int = 30): ApiResult<okhttp3.ResponseBody> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.exportCsv(childId, days)
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Start async PDF export. Returns task ID for polling. */
  suspend fun exportPdf(childId: Int, days: Int = 30): ApiResult<ExportJobResponse> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.exportPdf(childId, ExportPdfRequest(days))
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Poll PDF export status. */
  suspend fun getExportStatus(childId: Int, taskId: String): ApiResult<JobStatusResponse> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.getExportStatus(childId, taskId)
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Download generated PDF file. Returns raw response body. */
  suspend fun downloadPdf(filename: String): ApiResult<okhttp3.ResponseBody> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.downloadPdf(filename)
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Get weekly summary for pediatrician view. */
  suspend fun getWeeklySummary(childId: Int): ApiResult<WeeklySummary> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.getWeeklySummary(childId)
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}
