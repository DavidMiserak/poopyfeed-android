package com.poopyfeed.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Child(
    val id: Int,
    val name: String,
    @SerialName("date_of_birth")
    val dateOfBirth: String,
    val gender: String? = null,
    @SerialName("user_role")
    val userRole: String,
    @SerialName("can_edit")
    val canEdit: Boolean,
    @SerialName("can_manage_sharing")
    val canManageSharing: Boolean,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("last_diaper_change")
    val lastDiaperChange: String? = null,
    @SerialName("last_nap")
    val lastNap: String? = null,
    @SerialName("last_feeding")
    val lastFeeding: String? = null,
)

@Serializable
data class ChildrenResponse(
    val results: List<Child>,
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
)

@Serializable
data class CreateChildRequest(
    val name: String,
    @SerialName("date_of_birth")
    val dateOfBirth: String,
    val gender: String? = null,
)

@Serializable
data class UpdateChildRequest(
    val name: String? = null,
    @SerialName("date_of_birth")
    val dateOfBirth: String? = null,
    val gender: String? = null,
)
