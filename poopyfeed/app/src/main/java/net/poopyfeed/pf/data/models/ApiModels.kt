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
    val feeding_reminder_interval: Int? = null // 2, 3, 4, or 6 hours; null = off
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
)

/** Feeding event - bottle or breastfeeding. */
data class Feeding(
    val id: Int,
    val child: Int,
    val feeding_type: String, // 'bottle' or 'breast'
    val amount_oz: Double? = null, // Only for bottle feeding
    val timestamp: String, // ISO 8601 datetime
    val created_at: String,
    val updated_at: String
)

/**
 * Backend list response for feedings. API returns [fed_at] and [amount_oz] as string; [child] is
 * omitted (known from URL). Map to [Feeding] in the repository with [childId].
 */
@Serializable
data class FeedingListResponse(
    val id: Int,
    val feeding_type: String,
    @kotlinx.serialization.SerialName("fed_at") val fed_at: String,
    val amount_oz: String? = null,
    val created_at: String,
    val updated_at: String
) {
  fun toFeeding(childId: Int): Feeding =
      Feeding(
          id = id,
          child = childId,
          feeding_type = feeding_type,
          amount_oz = amount_oz?.toDoubleOrNull(),
          timestamp = fed_at,
          created_at = created_at,
          updated_at = updated_at)
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
    @kotlinx.serialization.SerialName("changed_at") val changed_at: String,
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

/** DTO for ending an ongoing nap (PATCH with [ended_at]). */
@Serializable data class UpdateNapRequest(@SerialName("ended_at") val end_time: String)

/**
 * Backend list response for naps. API returns [napped_at] and [ended_at]; [child] is omitted. Map
 * to [Nap] in the repository with [childId].
 */
@Serializable
data class NapListResponse(
    val id: Int,
    @kotlinx.serialization.SerialName("napped_at") val napped_at: String,
    @kotlinx.serialization.SerialName("ended_at") val ended_at: String? = null,
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

/** Child sharing relationship. */
@Serializable
data class ChildShare(
    val id: Int,
    val child: Int,
    val shared_with_user: String, // Email of the shared-with user
    val role: String, // 'co-parent' or 'caregiver'
    val created_at: String,
    val updated_at: String
)

/** DTO for creating a share invite. */
@Serializable data class CreateShareRequest(val email: String, val role: String)

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

/** Share invite (pending or accepted). */
@Serializable
data class ShareInvite(
    val id: Int,
    val child: Int,
    val invited_email: String,
    val role: String,
    val status: String, // 'pending' or 'accepted'
    val created_at: String,
    val updated_at: String
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
