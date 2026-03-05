package net.poopyfeed.pf

import android.content.Context
import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Utility functions for formatting dates and times. All functions handle ISO 8601 datetime strings
 * with UTC timezone (format: "YYYY-MM-DDTHH:MM:SSZ").
 */

/**
 * Formats a relative time span from an ISO 8601 datetime string (e.g. "1 hour ago", "3 days ago").
 * Null input returns a localized "Never" string.
 *
 * Uses [DateUtils.getRelativeTimeSpanString] with minute granularity for accurate, human-readable
 * output.
 *
 * @param context Context for localization
 * @param isoString ISO 8601 datetime or null (format: "YYYY-MM-DDTHH:MM:SSZ")
 * @param nowMillis Current time in millis for testing; defaults to [System.currentTimeMillis]
 * @return Human-readable relative time (e.g. "2 hours ago") or localized "Never" string
 */
fun formatRelativeTime(
    context: Context,
    isoString: String?,
    nowMillis: Long = System.currentTimeMillis(),
): String {
  if (isoString == null) {
    return context.getString(R.string.child_detail_never)
  }

  return try {
    val millis = parseIso8601DateTime(isoString)
    DateUtils.getRelativeTimeSpanString(millis, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString()
  } catch (e: Exception) {
    context.getString(R.string.child_detail_never)
  }
}

/**
 * Formats the age of a child from a date of birth string (ISO 8601 date format). Returns "X months"
 * for infants under 12 months, or "X yr Y mo" for older children.
 *
 * @param dobString ISO 8601 date string (format: "YYYY-MM-DD")
 * @param nowMillis Current time in millis for testing; defaults to [System.currentTimeMillis]
 * @return Age formatted as "X months" or "X yr Y mo"
 */
fun formatAge(dobString: String, nowMillis: Long = System.currentTimeMillis()): String {
  return try {
    val dob = parseIso8601Date(dobString)
    val now = Calendar.getInstance()
    now.timeInMillis = nowMillis

    var years = now.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
    var months = now.get(Calendar.MONTH) - dob.get(Calendar.MONTH)

    // Adjust for negative month difference
    if (months < 0) {
      years--
      months += 12
    }

    // Adjust for birthday not yet occurred this year
    if (now.get(Calendar.DAY_OF_MONTH) < dob.get(Calendar.DAY_OF_MONTH)) {
      months--
      if (months < 0) {
        years--
        months += 12
      }
    }

    when {
      years == 0 -> "$months month${if (months == 1) "" else "s"}"
      months == 0 -> "$years yr"
      else -> "$years yr $months mo"
    }
  } catch (e: Exception) {
    ""
  }
}

/**
 * Parses an ISO 8601 datetime string (format: "YYYY-MM-DDTHH:MM:SSZ") to milliseconds since epoch.
 *
 * Handles UTC timezone parsing. Throws exception if format is invalid.
 */
private fun parseIso8601DateTime(isoString: String): Long {
  val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
  format.timeZone = TimeZone.getTimeZone("UTC")
  val date = format.parse(isoString) ?: throw IllegalArgumentException("Invalid date: $isoString")
  return date.time
}

/**
 * Parses an ISO 8601 date string (format: "YYYY-MM-DD") to a Calendar object (midnight UTC).
 *
 * Throws exception if format is invalid.
 */
private fun parseIso8601Date(isoString: String): Calendar {
  val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  format.timeZone = TimeZone.getTimeZone("UTC")
  val date = format.parse(isoString) ?: throw IllegalArgumentException("Invalid date: $isoString")
  return Calendar.getInstance().apply {
    timeInMillis = date.time
    timeZone = TimeZone.getTimeZone("UTC")
  }
}
