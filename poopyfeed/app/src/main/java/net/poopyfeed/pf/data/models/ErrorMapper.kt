package net.poopyfeed.pf.data.models

/**
 * Convert exceptions to typed ApiError.
 * Used by all repositories for consistent error handling.
 */
internal fun Exception.toApiError(): ApiError = when (this) {
    is retrofit2.HttpException -> {
        try {
            val errorBody = this.response()?.errorBody()?.string()
            ApiError.HttpError(
                statusCode = this.code(),
                errorMessage = this.message() ?: "HTTP ${this.code()}",
                detail = errorBody
            )
        } catch (_: Exception) {
            ApiError.HttpError(
                statusCode = this.code(),
                errorMessage = this.message() ?: "HTTP ${this.code()}"
            )
        }
    }
    is java.io.IOException -> ApiError.NetworkError(this.message ?: "Network error")
    else -> ApiError.UnknownError(this.message ?: "Unknown error")
}
