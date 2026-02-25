package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.CreateDiaperRequest
import com.poopyfeed.android.data.remote.dto.Diaper
import com.poopyfeed.android.data.remote.dto.DiaperListResponse
import com.poopyfeed.android.data.remote.dto.UpdateDiaperRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface DiapersApi {
    @GET("api/v1/children/{childId}/diapers/")
    suspend fun getDiapers(
        @Path("childId") childId: Int,
    ): Response<DiaperListResponse>

    @GET("api/v1/children/{childId}/diapers/{id}/")
    suspend fun getDiaper(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
    ): Response<Diaper>

    @POST("api/v1/children/{childId}/diapers/")
    suspend fun createDiaper(
        @Path("childId") childId: Int,
        @Body request: CreateDiaperRequest,
    ): Response<Diaper>

    @PATCH("api/v1/children/{childId}/diapers/{id}/")
    suspend fun updateDiaper(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
        @Body request: UpdateDiaperRequest,
    ): Response<Diaper>

    @DELETE("api/v1/children/{childId}/diapers/{id}/")
    suspend fun deleteDiaper(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
    ): Response<Unit>
}
