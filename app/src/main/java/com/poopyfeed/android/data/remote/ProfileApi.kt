package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.ChangePasswordRequest
import com.poopyfeed.android.data.remote.dto.ChangePasswordResponse
import com.poopyfeed.android.data.remote.dto.DeleteAccountRequest
import com.poopyfeed.android.data.remote.dto.UpdateProfileRequest
import com.poopyfeed.android.data.remote.dto.UserProfile
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST

interface ProfileApi {

    @GET("api/v1/account/profile/")
    suspend fun getProfile(): Response<UserProfile>

    @PATCH("api/v1/account/profile/")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserProfile>

    @POST("api/v1/account/password/")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ChangePasswordResponse>

    @POST("api/v1/account/delete/")
    suspend fun deleteAccount(@Body request: DeleteAccountRequest): Response<Unit>
}
