package com.poopyfeed.android.data.remote

import com.poopyfeed.android.data.remote.dto.AllauthResponse
import com.poopyfeed.android.data.remote.dto.LoginRequest
import com.poopyfeed.android.data.remote.dto.SignupRequest
import com.poopyfeed.android.data.remote.dto.TokenResponse
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

@Serializable
data class EmptyRequest(
    val unused: String = "",
)

interface AuthApi {
    @POST("api/v1/browser/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequest,
    ): Response<AllauthResponse>

    @POST("api/v1/browser/v1/auth/signup")
    suspend fun signup(
        @Body request: SignupRequest,
    ): Response<AllauthResponse>

    @POST("api/v1/browser/v1/auth/token/")
    suspend fun getToken(
        @Body body: EmptyRequest = EmptyRequest(),
    ): Response<TokenResponse>

    @DELETE("api/v1/browser/v1/auth/session")
    suspend fun logout(): Response<Unit>
}
