package net.poopyfeed.pf.data.models

import android.content.Context
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.poopyfeed.pf.R

/**
 * API Response wrapper for paginated endpoints. Matches Django REST Framework's default pagination
 * response.
 */
@Serializable
data class PaginatedResponse<T>(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T>
) {

  /** True when there is another page of results; derived from [next], independent of page size. */
  val hasNextPage: Boolean
    get() = next != null
}

/** Child profile resource. Represents a baby being tracked in the app. */
@Serializable
data class Child(
    val id: Int,
    val name: String,
    val date_of_birth: String, // ISO 8601 format: YYYY-MM-DD
    val gender: String, // 'M' or 'F'
    val user_role: String, // 'owner', 'co-parent', 'caregiver'
    val created_at: String, // ISO 8601 datetime
    val updated_at: String, // ISO 8601 datetime
    val last_feeding: String? = null, // ISO 8601 datetime or null
    val last_diaper_change: String? = null, // ISO 8601 datetime or null
    val last_nap: String? = null, // ISO 8601 datetime or null
    val can_edit: Boolean = false, // true for owner or co-parent
    val feeding_reminder_interval: Int? = null, // 2, 3, 4, or 6 hours; null = off
    val custom_bottle_low_oz: String? = null, // Quick-log bottle amount (oz)
    val custom_bottle_mid_oz: String? = null,
    val custom_bottle_high_oz: String? = null,
)

/** DTO for creating a Child. */
@Serializable
data class CreateChildRequest(val name: String, val date_of_birth: String, val gender: String)

/**
 * DTO for PATCH update of a Child. Only non-null fields are sent so the backend performs partial
 * update. Used for editing profile and/or feeding reminder interval (owner/co-parent only).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdateChildRequest(
    @EncodeDefault(EncodeDefault.Mode.NEVER) val name: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val date_of_birth: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val gender: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val feeding_reminder_interval: Int? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val custom_bottle_low_oz: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val custom_bottle_mid_oz: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val custom_bottle_high_oz: String? = null,
)

/** Per-child notification preference. Auto-created by backend for each accessible child. */
@Serializable
data class NotificationPreference(
    val id: Int,
    @SerialName("child_id") val childId: Int,
    @SerialName("child_name") val childName: String,
    @SerialName("notify_feedings") val notifyFeedings: Boolean,
    @SerialName("notify_diapers") val notifyDiapers: Boolean,
    @SerialName("notify_naps") val notifyNaps: Boolean,
)

/** DTO for PATCH update of a notification preference. Only non-null fields are sent. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdateNotificationPreferenceRequest(
    @SerialName("notify_feedings")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val notifyFeedings: Boolean? = null,
    @SerialName("notify_diapers")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val notifyDiapers: Boolean? = null,
    @SerialName("notify_naps")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val notifyNaps: Boolean? = null,
)

/** Feeding event - bottle or breastfeeding. */
data class Feeding(
    val id: Int,
    val child: Int,
    val feeding_type: String, // 'bottle' or 'breast'
    val amount_oz: Double? = null, // Only for bottle feeding
    val timestamp: String, // ISO 8601 datetime
    val created_at: String,
    val updated_at: String,
    val duration_minutes: Int? = null, // Breast only
    val side: String? = null, // Breast only: 'left', 'right', 'both'
)

/**
 * Backend list response for feedings. API returns [fed_at] and [amount_oz] as string; [child] is
 * omitted (known from URL). Map to [Feeding] in the repository with [childId].
 */
@Serializable
data class FeedingListResponse(
    val id: Int,
    val feeding_type: String,
    @SerialName("fed_at") val fed_at: String,
    val amount_oz: String? = null,
    val created_at: String,
    val updated_at: String,
    @SerialName("duration_minutes") val duration_minutes: Int? = null,
    val side: String? = null,
) {
  fun toFeeding(childId: Int): Feeding =
      Feeding(
          id = id,
          child = childId,
          feeding_type = feeding_type,
          amount_oz = amount_oz?.toDoubleOrNull(),
          timestamp = fed_at,
          created_at = created_at,
          updated_at = updated_at,
          duration_minutes = duration_minutes,
          side = side?.takeIf { it.isNotBlank() },
      )
}

/**
 * DTO for creating/updating a Feeding. Backend expects [fed_at]. Breast requires [duration_minutes]
 * and [side].
 */
@Serializable
data class CreateFeedingRequest(
    val feeding_type: String,
    val amount_oz: Double? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    val side: String? = null,
    @SerialName("fed_at") val timestamp: String
)

/** Diaper change event. */
data class Diaper(
    val id: Int,
    val child: Int,
    val change_type: String, // 'wet', 'dirty', 'both'
    val timestamp: String, // ISO 8601 datetime
    val created_at: String,
    val updated_at: String
)

/**
 * DTO for creating a diaper change. Backend expects [changed_at]. [change_type]: `"wet"`,
 * `"dirty"`, or `"both"`.
 */
@Serializable
data class CreateDiaperRequest(
    val change_type: String,
    @SerialName("changed_at") val timestamp: String
)

/**
 * Backend list response for diapers. API returns [changed_at]; [child] is omitted. Map to [Diaper]
 * in the repository with [childId].
 */
@Serializable
data class DiaperListResponse(
    val id: Int,
    val change_type: String,
    @SerialName("changed_at") val changed_at: String,
    val created_at: String,
    val updated_at: String
) {
  fun toDiaper(childId: Int): Diaper =
      Diaper(
          id = id,
          child = childId,
          change_type = change_type,
          timestamp = changed_at,
          created_at = created_at,
          updated_at = updated_at)
}

/** Nap event. */
data class Nap(
    val id: Int,
    val child: Int,
    val start_time: String, // ISO 8601 datetime
    val end_time: String? = null, // ISO 8601 datetime or null (if nap is ongoing)
    val created_at: String,
    val updated_at: String
)

/**
 * DTO for creating a nap. Backend expects [napped_at] and [ended_at]. [end_time] is null when the
 * nap is ongoing.
 */
@Serializable
data class CreateNapRequest(
    @SerialName("napped_at") val start_time: String,
    @SerialName("ended_at") val end_time: String? = null
)

/** DTO for PATCH nap (partial update). Omit nulls so backend receives only provided fields. */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UpdateNapRequest(
    @SerialName("napped_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val start_time: String? = null,
    @SerialName("ended_at") @EncodeDefault(EncodeDefault.Mode.NEVER) val end_time: String? = null,
)

/**
 * Backend list response for naps. API returns [napped_at] and [ended_at]; [child] is omitted. Map
 * to [Nap] in the repository with [childId].
 */
@Serializable
data class NapListResponse(
    val id: Int,
    @SerialName("napped_at") val napped_at: String,
    @SerialName("ended_at") val ended_at: String? = null,
    val created_at: String,
    val updated_at: String
) {
  fun toNap(childId: Int): Nap =
      Nap(
          id = id,
          child = childId,
          start_time = napped_at,
          end_time = ended_at,
          created_at = created_at,
          updated_at = updated_at)
}

/** Child share (existing access). From GET /api/v1/children/{id}/shares/. */
@Serializable
data class ChildShare(
    val id: Int,
    @SerialName("user_email") val userEmail: String,
    val role: String, // 'co-parent' or 'caregiver'
    @SerialName("role_display") val roleDisplay: String? = null,
    @SerialName("created_at") val createdAt: String,
)

/** DTO for creating an invite. Backend expects only role. POST /api/v1/children/{id}/invites/. */
@Serializable data class CreateShareRequest(val role: String)

/** Response from creating an invite. Contains token for share link. */
@Serializable
data class ShareInviteResponse(
    val id: Int,
    val token: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("invite_url") val inviteUrl: String? = null,
)

/** Invite link for a child (from GET/PATCH /api/v1/children/{id}/invites/). */
@Serializable
data class ChildInvite(
    val id: Int,
    val token: String,
    val role: String,
    @SerialName("role_display") val roleDisplay: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("invite_url") val inviteUrl: String? = null,
)

/** DTO for accepting an invite. POST /api/v1/invites/accept/. */
@Serializable data class AcceptInviteRequest(val token: String)

// ========================
// Notifications
// ========================

/** In-app notification for activity alerts (feeding, diaper, nap). */
@Serializable
data class Notification(
    val id: Int,
    @SerialName("event_type") val eventType: String,
    val message: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("actor_name") val actorName: String,
    @SerialName("child_name") val childName: String,
    @SerialName("child_id") val childId: Int
)

@Serializable data class UnreadCountResponse(val count: Int)

@Serializable data class MarkAllReadResponse(val updated: Int)

/** Request body for PATCH notification (mark as read). */
@Serializable data class MarkReadRequest(@SerialName("is_read") val isRead: Boolean = true)

/**
 * Share invite (from GET /api/v1/children/{id}/invites/). Token-based; no "pending for user"
 * endpoint. Accept via POST /api/v1/invites/accept/ with { "token": "..." }.
 */
@Serializable
data class ShareInvite(
    val id: Int,
    val child: Int,
    val token: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String,
    @SerialName("invite_url") val inviteUrl: String? = null,
)

/**
 * Sealed class for API error responses. Type-safe error handling across the app. Use
 * [getUserMessage] for user-facing strings.
 */
sealed class ApiError : Exception() {
  /** HTTP error (4xx/5xx) with optional [detail] body and [fields] validation errors. */
  data class HttpError(
      val statusCode: Int,
      val errorMessage: String,
      val detail: String? = null,
      val fields: Map<String, List<String>>? = null
  ) : ApiError()

  /** Network failure (e.g. timeout, no connectivity). */
  data class NetworkError(val errorMessage: String) : ApiError()

  /** Response body could not be deserialized. */
  data class SerializationError(val errorMessage: String) : ApiError()

  /** Any other error. */
  data class UnknownError(val errorMessage: String) : ApiError()

  /** Extract user-friendly error message. Requires [Context] for i18n string resources. */
  fun getUserMessage(context: Context): String =
      when (this) {
        is HttpError -> detail ?: errorMessage
        is NetworkError -> context.getString(R.string.error_network)
        is SerializationError -> context.getString(R.string.error_serialization)
        is UnknownError -> context.getString(R.string.error_unknown)
      }
}

/**
 * Sealed class for API result state. Use for loading, success, and error states in UI. Flow-based
 * repos typically emit [Loading] first, then [Success] or [Error].
 */
sealed class ApiResult<out T> {
  /** Successful response with [data]. */
  data class Success<T>(val data: T) : ApiResult<T>()

  /** Request in progress. */
  class Loading<T> : ApiResult<T>()

  /** Request failed with [error]. */
  data class Error<T>(val error: ApiError) : ApiResult<T>()
}

/**
 * Session login response from django-allauth headless browser auth. We only care that status is
 * 200; data/meta are ignored.
 */
@Serializable
data class SessionLoginResponse(
    val status: Int,
    val data: kotlinx.serialization.json.JsonElement? = null,
    val meta: kotlinx.serialization.json.JsonElement? = null
)

/** Response from the browser token endpoint; contains the token for the Authorization header. */
@Serializable data class AuthTokenResponse(val auth_token: String)

/** Backwards-compatible auth token model (legacy key-based token endpoints). */
@Serializable
data class AuthToken(
    val key: String // The auth token to use in Authorization header
)

/**
 * Request for changing authenticated user's password.
 *
 * All three fields are required. Backend validates password strength and token rotation.
 */
@Serializable
data class ChangePasswordRequest(
    val current_password: String,
    val new_password: String,
    val new_password_confirm: String
)

/**
 * Response from password change endpoint. Includes detail message and new auth token.
 *
 * The new token must be stored to replace the old one (token rotation).
 */
@Serializable data class ChangePasswordResponse(val detail: String, val auth_token: String)

/**
 * Request for deleting authenticated user's account.
 *
 * Requires password confirmation for security. Backend will delete the user and all related data
 * irreversibly.
 */
@Serializable data class DeleteAccountRequest(val current_password: String)

/**
 * User sign-up request for django-allauth headless API.
 *
 * Matches the web frontend `SignupRequest` shape; `re_password` is optional and not required by the
 * backend but kept for parity.
 */
@Serializable
data class SignupRequest(val email: String, val password: String, val re_password: String? = null)

/** User login request for the session login endpoint. */
@Serializable data class LoginRequest(val email: String, val password: String)

/** Authenticated user profile response. */
@Serializable
data class UserProfile(
    val id: Int,
    val email: String,
    val first_name: String,
    val last_name: String,
    val timezone: String
)

/**
 * Partial update payload for the authenticated user's profile.
 *
 * Mirrors the web frontend `UserProfileUpdate` shape and allows updating a subset of fields such as
 * first name, last name, or timezone.
 */
@Serializable
data class UserProfileUpdate(
    val first_name: String? = null,
    val last_name: String? = null,
    val email: String? = null,
    val timezone: String? = null
)
