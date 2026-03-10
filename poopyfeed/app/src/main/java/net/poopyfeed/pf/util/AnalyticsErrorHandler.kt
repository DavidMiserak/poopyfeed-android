package net.poopyfeed.pf.util

import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiError

/**
 * Helper function to log errors consistently across the app. Wraps [AnalyticsTracker] to provide a
 * unified error handling pattern.
 *
 * @param analyticsTracker The analytics tracker instance to use for logging
 * @param error The API error to log
 * @param context Optional context string describing where the error occurred (e.g., "listChildren",
 *   "createFeeding")
 * @return User-facing error message derived from the error type and context
 */
fun handleAndLogError(
    analyticsTracker: AnalyticsTracker,
    error: ApiError,
    context: String = "Unknown",
): String {
  val (errorType, errorMessage) =
      when (error) {
        is ApiError.HttpError ->
            "HttpError_${error.statusCode}" to error.errorMessage
        is ApiError.NetworkError ->
            "NetworkError" to error.errorMessage
        is ApiError.SerializationError ->
            "SerializationError" to error.errorMessage
        is ApiError.UnknownError ->
            "UnknownError" to error.errorMessage
      }

  analyticsTracker.logError(errorType, "$context: $errorMessage")
  return errorMessage
}
