package net.poopyfeed.pf.data.models

import kotlinx.serialization.Serializable

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
  val totalPages: Int
    get() = (count + results.size - 1) / results.size.coerceAtLeast(1)
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
    val last_nap: String? = null // ISO 8601 datetime or null
)

/** DTO for creating/updating a Child. */
@Serializable
data class CreateChildRequest(val name: String, val date_of_birth: String, val gender: String)

/** Feeding event - bottle or breastfeeding. */
@Serializable
data class Feeding(
    val id: Int,
    val child: Int,
    val feeding_type: String, // 'bottle' or 'breast'
    val amount_oz: Double? = null, // Only for bottle feeding
    val timestamp: String, // ISO 8601 datetime
    val created_at: String,
    val updated_at: String
)

/** DTO for creating/updating a Feeding. */
@Serializable
data class CreateFeedingRequest(
    val feeding_type: String,
    val amount_oz: Double? = null,
    val timestamp: String
)

/** Diaper change event. */
@Serializable
data class Diaper(
    val id: Int,
    val child: Int,
    val change_type: String, // 'wet', 'dirty', 'both'
    val timestamp: String, // ISO 8601 datetime
    val created_at: String,
    val updated_at: String
)

/** DTO for creating/updating a Diaper change. */
@Serializable data class CreateDiaperRequest(val change_type: String, val timestamp: String)

/** Nap event. */
@Serializable
data class Nap(
    val id: Int,
    val child: Int,
    val start_time: String, // ISO 8601 datetime
    val end_time: String? = null, // ISO 8601 datetime or null (if nap is ongoing)
    val created_at: String,
    val updated_at: String
)

/** DTO for creating a Nap. */
@Serializable data class CreateNapRequest(val start_time: String, val end_time: String? = null)

/** DTO for ending an ongoing nap. */
@Serializable data class UpdateNapRequest(val end_time: String)

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

/** Sealed class for API error responses. Type-safe error handling across the app. */
sealed class ApiError : Exception() {
  data class HttpError(
      val statusCode: Int,
      val errorMessage: String,
      val detail: String? = null,
      val fields: Map<String, List<String>>? = null
  ) : ApiError()

  data class NetworkError(val errorMessage: String) : ApiError()

  data class SerializationError(val errorMessage: String) : ApiError()

  data class UnknownError(val errorMessage: String) : ApiError()

  /** Extract user-friendly error message. */
  fun getUserMessage(): String =
      when (this) {
        is HttpError -> detail ?: errorMessage
        is NetworkError -> "Network error. Please check your connection."
        is SerializationError -> "Data format error. Please try again."
        is UnknownError -> "Something went wrong. Please try again."
      }
}

/** Sealed class for API result state. Use for loading, success, and error states in UI. */
sealed class ApiResult<out T> {
  data class Success<T>(val data: T) : ApiResult<T>()

  class Loading<T> : ApiResult<T>()

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

/** User authentication token response from browser token endpoint. */
@Serializable data class AuthTokenResponse(val auth_token: String)

/** Backwards-compatible auth token model (legacy key-based token endpoints). */
@Serializable
data class AuthToken(
    val key: String // The auth token to use in Authorization header
)

/**
 * User sign-up request for django-allauth headless API.
 *
 * Matches the web frontend `SignupRequest` shape; `re_password` is optional and not required by the
 * backend but kept for parity.
 */
@Serializable
data class SignupRequest(val email: String, val password: String, val re_password: String? = null)

/** User login request. */
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
