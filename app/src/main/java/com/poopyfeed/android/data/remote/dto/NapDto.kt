package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Nap(
    val id: Int,
    @SerialName("napped_at")
    val nappedAt: String,
    @SerialName("ended_at")
    val endedAt: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Double? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class NapListResponse(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<Nap> = emptyList(),
)

@Serializable
data class CreateNapRequest(
    @SerialName("napped_at")
    val nappedAt: String,
)

@Serializable
data class UpdateNapRequest(
    @SerialName("napped_at")
    val nappedAt: String? = null,
)
