package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.TodaySummaryResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AnalyticsApi {
    @GET("api/v1/analytics/children/{id}/today-summary/")
    suspend fun getTodaySummary(
        @Path("id") id: Int,
    ): Response<TodaySummaryResponse>

    @POST("api/v1/analytics/children/{id}/export-csv/")
    suspend fun exportCsv(
        @Path("id") childId: Int,
        @Query("days") days: Int? = null,
    ): Response<ResponseBody>

    @POST("api/v1/analytics/children/{id}/export-pdf/")
    suspend fun exportPdf(
        @Path("id") childId: Int,
        @retrofit2.http.Body body: ExportPdfRequest,
    ): Response<ExportPdfResponse>

    @GET("api/v1/analytics/children/{id}/export-status/{taskId}/")
    suspend fun getExportStatus(
        @Path("id") childId: Int,
        @Path("taskId") taskId: String,
    ): Response<ExportStatusResponse>

    @GET("api/v1/analytics/download/{filename}/")
    suspend fun downloadPdf(
        @Path("filename") filename: String,
    ): Response<ResponseBody>
}

@kotlinx.serialization.Serializable
data class ExportPdfRequest(
    val days: Int = 30,
)

@kotlinx.serialization.Serializable
data class ExportPdfResponse(
    val task_id: String,
    val status: String,
    val message: String? = null,
)

@kotlinx.serialization.Serializable
data class ExportStatusResponse(
    val task_id: String,
    val status: String,
    val progress: Int? = null,
    val result: String? = null,
    val error: String? = null,
)
