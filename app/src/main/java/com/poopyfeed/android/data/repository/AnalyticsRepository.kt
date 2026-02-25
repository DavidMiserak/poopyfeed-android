package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.remote.AnalyticsApi
import com.poopyfeed.android.data.remote.dto.TodaySummaryResponse
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
    }
