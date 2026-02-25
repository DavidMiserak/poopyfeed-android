package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.CreateFeedingRequest
import com.poopyfeed.android.data.remote.dto.Feeding
import com.poopyfeed.android.data.remote.dto.FeedingListResponse
import com.poopyfeed.android.data.remote.dto.UpdateFeedingRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface FeedingsApi {
    @GET("api/v1/children/{childId}/feedings/")
    suspend fun getFeedings(
        @Path("childId") childId: Int,
    ): Response<FeedingListResponse>

    @GET("api/v1/children/{childId}/feedings/{id}/")
    suspend fun getFeeding(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
    ): Response<Feeding>

    @POST("api/v1/children/{childId}/feedings/")
    suspend fun createFeeding(
        @Path("childId") childId: Int,
        @Body request: CreateFeedingRequest,
    ): Response<Feeding>

    @PATCH("api/v1/children/{childId}/feedings/{id}/")
    suspend fun updateFeeding(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
        @Body request: UpdateFeedingRequest,
    ): Response<Feeding>

    @DELETE("api/v1/children/{childId}/feedings/{id}/")
    suspend fun deleteFeeding(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
    ): Response<Unit>
}
