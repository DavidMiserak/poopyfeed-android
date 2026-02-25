package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Share(
    val id: Int,
    @SerialName("user_email")
    val userEmail: String,
    val role: String,
    @SerialName("role_display")
    val roleDisplay: String,
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class Invite(
    val id: Int,
    val token: String,
    val role: String,
    @SerialName("role_display")
    val roleDisplay: String,
    @SerialName("is_active")
    val isActive: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("invite_url")
    val inviteUrl: String,
)

@Serializable
data class CreateInviteRequest(
    val role: String,
)
