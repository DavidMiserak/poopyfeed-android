package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response from POST /api/v1/children/{childId}/batch/
 *
 * Success (201): created list and count.
 * Error (400): errors list with index, type, and field-level messages.
 */
@Serializable
data class BatchResponse(
    val created: List<BatchCreatedItem> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class BatchCreatedItem(
    val type: String,
    val id: Int,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    // Type-specific fields (e.g. fed_at, changed_at, napped_at) - ignore for success handling
)

@Serializable
data class BatchErrorResponse(
    val errors: List<BatchEventError> = emptyList(),
)

@Serializable
data class BatchEventError(
    val index: Int,
    val type: String,
    val errors: Map<String, List<String>> = emptyMap(),
)
