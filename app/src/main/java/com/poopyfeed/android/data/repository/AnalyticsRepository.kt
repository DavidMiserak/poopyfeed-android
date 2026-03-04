package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.AnalyticsApi
import com.poopyfeed.android.data.remote.ExportPdfRequest
import com.poopyfeed.android.data.remote.ExportStatusResponse
import com.poopyfeed.android.data.remote.dto.PatternAlertsResponse
import com.poopyfeed.android.data.remote.dto.TodaySummaryResponse
import com.poopyfeed.android.data.remote.dto.WeeklySummaryResponse
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository
    @Inject
    constructor(
        private val analyticsApi: AnalyticsApi,
    ) {
        suspend fun getTodaySummary(childId: Int): Result<TodaySummaryResponse> {
            return try {
                val response = analyticsApi.getTodaySummary(childId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val body = response.body() ?: return Result.failure(Exception("Empty today summary response"))
                Result.success(body)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun getPatternAlerts(childId: Int): Result<PatternAlertsResponse> {
            return try {
                val response = analyticsApi.getPatternAlerts(childId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val body = response.body() ?: return Result.failure(Exception("Empty pattern alerts response"))
                Result.success(body)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun getWeeklySummary(childId: Int): Result<WeeklySummaryResponse> {
            return try {
                val response = analyticsApi.getWeeklySummary(childId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val body = response.body() ?: return Result.failure(Exception("Empty weekly summary response"))
                Result.success(body)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun exportCsv(
            childId: Int,
            days: Int = 30,
        ): Result<ResponseBody> {
            return try {
                val response = analyticsApi.exportCsv(childId, days)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                Result.success(response.body()!!)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun exportPdf(
            childId: Int,
            days: Int = 30,
        ): Result<String> {
            return try {
                val response = analyticsApi.exportPdf(childId, ExportPdfRequest(days = days))
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                val body = response.body() ?: return Result.failure(Exception("Empty export response"))
                Result.success(body.task_id)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun getExportStatus(
            childId: Int,
            taskId: String,
        ): Result<ExportStatusResponse> {
            return try {
                val response = analyticsApi.getExportStatus(childId, taskId)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                Result.success(response.body()!!)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }

        suspend fun downloadPdf(filename: String): Result<ResponseBody> {
            return try {
                val response = analyticsApi.downloadPdf(filename)
                if (!response.isSuccessful) {
                    return Result.failure(
                        Exception(ChildrenRepository.parseErrorBody(response.errorBody()?.string())),
                    )
                }
                Result.success(response.body()!!)
            } catch (e: Exception) {
                Result.failure(Exception(ChildrenRepository.getNetworkErrorMessage(e)))
            }
        }
    }
