package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.BatchResponse
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Batch creation API for catch-up mode.
 *
 * POST /api/v1/children/{childId}/batch/
 * Body: { "events": [ { "type": "feeding"|"diaper"|"nap", "data": { ... } }, ... ] }
 */
interface BatchApi {
    @POST("api/v1/children/{childId}/batch/")
    suspend fun createBatch(
        @Path("childId") childId: Int,
        @Body body: RequestBody,
    ): Response<BatchResponse>
}
