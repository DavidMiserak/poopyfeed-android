package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Diaper(
    val id: Int,
    @SerialName("change_type")
    val changeType: String,
    @SerialName("changed_at")
    val changedAt: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class DiaperListResponse(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<Diaper> = emptyList(),
)

@Serializable
data class CreateDiaperRequest(
    @SerialName("change_type")
    val changeType: String,
    @SerialName("changed_at")
    val changedAt: String,
)

@Serializable
data class UpdateDiaperRequest(
    @SerialName("change_type")
    val changeType: String? = null,
    @SerialName("changed_at")
    val changedAt: String? = null,
)
