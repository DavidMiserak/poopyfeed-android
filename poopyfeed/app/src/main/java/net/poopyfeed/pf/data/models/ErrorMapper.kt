package net.poopyfeed.pf.data.models

/** Convert exceptions to typed ApiError. Used by all repositories for consistent error handling. */
internal fun Exception.toApiError(): ApiError =
    when (this) {
      is retrofit2.HttpException -> {
        try {
          val errorBody = this.response()?.errorBody()?.string()
          ApiError.HttpError(
              statusCode = this.code(),
              errorMessage = this.message() ?: "HTTP ${this.code()}",
              detail = errorBody)
        } catch (_: Exception) {
          ApiError.HttpError(
              statusCode = this.code(), errorMessage = this.message() ?: "HTTP ${this.code()}")
        }
      }
      is java.io.IOException -> ApiError.NetworkError(this.message ?: "Network error")
      else -> ApiError.UnknownError(this.message ?: "Unknown error")
    }

/** Convert ApiError to user-friendly toast message. */
fun ApiError.toToastMessage(): String =
    when (this) {
      is ApiError.NetworkError -> "✗ Failed to save - no internet"
      is ApiError.HttpError ->
          when {
            this.statusCode == 401 -> "✗ Session expired - please login again"
            this.statusCode in 500..599 -> "✗ Server error - please try again"
            else -> "✗ Failed to save - error ${this.statusCode}"
          }
      is ApiError.SerializationError -> "✗ Failed to save - please try again"
      is ApiError.UnknownError -> "✗ Failed to save - please try again"
    }
