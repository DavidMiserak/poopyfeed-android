package net.poopyfeed.pf

import android.content.Context
import android.text.format.DateUtils
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Utility functions for formatting dates and times. All functions handle ISO 8601 strings from the
 * backend (always UTC).
 */

/**
 * Formats a relative time span from an ISO 8601 datetime string (e.g. "1 hour ago", "3 days ago").
 * Null input returns a localized "Never" string.
 *
 * @param context Context for localization
 * @param isoString ISO 8601 datetime or null (e.g. "2024-01-15T10:00:00Z" or with fractional
 *   seconds "2024-01-15T10:00:00.123456Z")
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
    val millis = Instant.parse(isoString).toEpochMilliseconds()
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
    val dob = LocalDate.parse(dobString)
    val now = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.UTC).date

    var years = now.year - dob.year
    var months = now.monthNumber - dob.monthNumber

    if (months < 0) {
      years--
      months += 12
    }

    if (now.dayOfMonth < dob.dayOfMonth) {
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
