package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class SignupRequest(
    val email: String,
    val password: String,
)

@Serializable
data class AllauthUser(
    val id: Int,
    val email: String,
)

@Serializable
data class AllauthData(
    val user: AllauthUser,
)

@Serializable
data class AllauthResponse(
    val status: Int,
    val data: AllauthData,
)

@Serializable
data class TokenResponse(
    @SerialName("auth_token")
    val authToken: String,
)
