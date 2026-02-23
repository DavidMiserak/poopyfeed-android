package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.ChildrenResponse
import com.poopyfeed.android.data.remote.dto.CreateChildRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ChildrenApi {

    @GET("api/v1/children/")
    suspend fun getChildren(): Response<ChildrenResponse>

    @POST("api/v1/children/")
    suspend fun createChild(@Body request: CreateChildRequest): Response<Child>
}
