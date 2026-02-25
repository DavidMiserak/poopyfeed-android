package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.TodaySummaryResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface AnalyticsApi {
    @GET("api/v1/analytics/children/{id}/today-summary/")
    suspend fun getTodaySummary(@Path("id") id: Int): Response<TodaySummaryResponse>
}
