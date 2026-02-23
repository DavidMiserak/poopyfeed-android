package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: Int,
    val email: String,
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
    val timezone: String,
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("first_name")
    val firstName: String? = null,
    @SerialName("last_name")
    val lastName: String? = null,
    val timezone: String? = null,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password")
    val currentPassword: String,
    @SerialName("new_password")
    val newPassword: String,
    @SerialName("new_password_confirm")
    val newPasswordConfirm: String,
)

@Serializable
data class ChangePasswordResponse(
    val detail: String,
    @SerialName("auth_token")
    val authToken: String,
)

@Serializable
data class DeleteAccountRequest(
    @SerialName("current_password")
    val currentPassword: String,
)
