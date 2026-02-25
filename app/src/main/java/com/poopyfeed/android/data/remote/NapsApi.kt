package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.CreateNapRequest
import com.poopyfeed.android.data.remote.dto.Nap
import com.poopyfeed.android.data.remote.dto.NapListResponse
import com.poopyfeed.android.data.remote.dto.UpdateNapRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface NapsApi {
    @GET("api/v1/children/{childId}/naps/")
    suspend fun getNaps(
        @Path("childId") childId: Int,
    ): Response<NapListResponse>

    @GET("api/v1/children/{childId}/naps/{id}/")
    suspend fun getNap(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
    ): Response<Nap>

    @POST("api/v1/children/{childId}/naps/")
    suspend fun createNap(
        @Path("childId") childId: Int,
        @Body request: CreateNapRequest,
    ): Response<Nap>

    @PATCH("api/v1/children/{childId}/naps/{id}/")
    suspend fun updateNap(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
        @Body request: UpdateNapRequest,
    ): Response<Nap>

    @DELETE("api/v1/children/{childId}/naps/{id}/")
    suspend fun deleteNap(
        @Path("childId") childId: Int,
        @Path("id") id: Int,
    ): Response<Unit>
}
