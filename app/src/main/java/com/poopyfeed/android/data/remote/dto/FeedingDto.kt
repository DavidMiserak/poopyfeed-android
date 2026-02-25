package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Feeding(
    val id: Int,
    @SerialName("feeding_type")
    val feedingType: String,
    @SerialName("fed_at")
    val fedAt: String,
    @SerialName("amount_oz")
    val amountOz: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Int? = null,
    val side: String? = null,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
)

@Serializable
data class FeedingListResponse(
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<Feeding> = emptyList(),
)

@Serializable
data class CreateFeedingRequest(
    @SerialName("feeding_type")
    val feedingType: String,
    @SerialName("fed_at")
    val fedAt: String,
    @SerialName("amount_oz")
    val amountOz: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Int? = null,
    val side: String? = null,
)

@Serializable
data class UpdateFeedingRequest(
    @SerialName("feeding_type")
    val feedingType: String? = null,
    @SerialName("fed_at")
    val fedAt: String? = null,
    @SerialName("amount_oz")
    val amountOz: String? = null,
    @SerialName("duration_minutes")
    val durationMinutes: Int? = null,
    val side: String? = null,
)
