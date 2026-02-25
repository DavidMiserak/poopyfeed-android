package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.ChildrenResponse
import com.poopyfeed.android.data.remote.dto.CreateChildRequest
import com.poopyfeed.android.data.remote.dto.UpdateChildRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ChildrenApi {
    @GET("api/v1/children/")
    suspend fun getChildren(): Response<ChildrenResponse>

    @GET("api/v1/children/{id}/")
    suspend fun getChild(
        @Path("id") id: Int,
    ): Response<Child>

    @POST("api/v1/children/")
    suspend fun createChild(
        @Body request: CreateChildRequest,
    ): Response<Child>

    @PATCH("api/v1/children/{id}/")
    suspend fun updateChild(
        @Path("id") id: Int,
        @Body request: UpdateChildRequest,
    ): Response<Child>

    @DELETE("api/v1/children/{id}/")
    suspend fun deleteChild(
        @Path("id") id: Int,
    ): Response<Unit>
}
