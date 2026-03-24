package net.poopyfeed.pf.util

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import net.poopyfeed.pf.R

/**
 * Utility functions for formatting dates and times. All functions handle ISO 8601 strings from the
 * backend (always UTC).
 *
 * Timezone behavior (FR-20): Absolute timestamps ([formatTimestampForDisplay],
 * [formatDateForDisplay]) are converted from UTC and displayed in the **device's local timezone**
 * so users see correct local date/time. [DateUtils] uses the system default timezone. Relative
 * times ([formatRelativeTime], [formatRelativeTimeShort]) are timezone-agnostic (epoch diff).
 * [formatAge] uses the device local date for "today" so child age is correct by location.
 */

/**
 * Formats a relative time span from an ISO 8601 datetime string (e.g. "1 hour ago", "3 days ago").
 * Null input returns a localized "Never" string. Uses device locale for wording; the span is
 * timezone-agnostic.
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
    val millis = kotlin.time.Instant.parse(isoString).toEpochMilliseconds()
    val diffMs = nowMillis - millis
    if (diffMs in 0 until DateUtils.MINUTE_IN_MILLIS) {
      context.getString(R.string.child_detail_just_now)
    } else {
      DateUtils.getRelativeTimeSpanString(millis, nowMillis, DateUtils.MINUTE_IN_MILLIS).toString()
    }
  } catch (e: Exception) {
    context.getString(R.string.child_detail_never)
  }
}

/**
 * Short relative time for lists (e.g. "now", "5m", "2h", "3d"). Null returns "—".
 *
 * @param context Context for optional localization (currently uses literal "—" and "now")
 * @param isoString ISO 8601 datetime or null
 * @param nowMillis Current time in millis for testing; defaults to [System.currentTimeMillis]
 * @return "—", "now", or abbreviated span like "1m", "1h", "1d"
 */
fun formatRelativeTimeShort(
    context: Context,
    isoString: String?,
    nowMillis: Long = System.currentTimeMillis(),
): String {
  if (isoString == null) return "—"
  return try {
    val millis = kotlin.time.Instant.parse(isoString).toEpochMilliseconds()
    val diffMs = nowMillis - millis
    val diffMinutes = diffMs / 60_000
    val diffHours = diffMs / 3_600_000
    val diffDays = diffMs / 86_400_000
    when {
      diffMinutes < 1 -> "now"
      diffMinutes < 60 -> "${diffMinutes}m"
      diffHours < 24 -> "${diffHours}h"
      else -> "${diffDays}d"
    }
  } catch (e: Exception) {
    "—"
  }
}

/**
 * Formats the age of a child from a date of birth string (ISO 8601 date format). Returns "X months"
 * for infants under 12 months, or "X yr Y mo" for older children. Uses the device local date for
 * "today" so age is correct by the user's location.
 *
 * @param dobString ISO 8601 date string (format: "YYYY-MM-DD")
 * @param nowMillis Current time in millis for testing; defaults to [System.currentTimeMillis]
 * @return Age formatted as "X months" or "X yr Y mo"
 */
fun formatAge(dobString: String, nowMillis: Long = System.currentTimeMillis()): String {
  return try {
    val dob = LocalDate.parse(dobString)
    val now =
        kotlin.time.Instant.fromEpochMilliseconds(nowMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

    var years = now.year - dob.year
    var months = now.month.number - dob.month.number

    if (months < 0) {
      years--
      months += 12
    }

    if (now.day < dob.day) {
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
 * Formats nap duration between two ISO 8601 datetimes (e.g. "1h 30m").
 *
 * @param context Context for localization (reserved for future i18n)
 * @param startIso Start time ISO 8601 string
 * @param endIso End time ISO 8601 string
 * @return Human-readable duration (e.g. "1h 30m", "45m")
 */
fun formatNapDuration(context: Context, startIso: String, endIso: String): String {
  return try {
    val startMs = kotlin.time.Instant.parse(startIso).toEpochMilliseconds()
    val endMs = kotlin.time.Instant.parse(endIso).toEpochMilliseconds()
    val diffMs = endMs - startMs
    val totalMinutes = (diffMs / (60 * 1000)).toInt()
    when {
      totalMinutes < 60 -> "${totalMinutes}m"
      totalMinutes % 60 == 0 -> "${totalMinutes / 60}h"
      else -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
    }
  } catch (e: Exception) {
    ""
  }
}

/**
 * Formats an ISO 8601 datetime string as time-only for display (e.g. "2:30 PM") in the device's
 * local timezone.
 */
fun formatTimeForDisplay(context: Context, isoString: String): String {
  return try {
    val millis = kotlin.time.Instant.parse(isoString).toEpochMilliseconds()
    DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME)
  } catch (e: Exception) {
    "—"
  }
}

/**
 * Formats an ISO 8601 datetime string as time-only for display (e.g. "2:30 PM") in the given
 * timezone. Falls back to the device timezone when [timezoneId] is null or invalid. Respects the
 * user's 12/24‑hour clock preference.
 */
fun formatTimeForDisplayWithTimezone(
    context: Context,
    isoString: String,
    timezoneId: String?,
): String {
  return try {
    val instant = kotlin.time.Instant.parse(isoString)
    val tz =
        timezoneId?.let { runCatching { TimeZone.of(it) }.getOrNull() }
            ?: TimeZone.currentSystemDefault()
    val localDateTime = instant.toLocalDateTime(tz)

    val is24Hour = DateFormat.is24HourFormat(context)
    val hour = localDateTime.hour
    val minute = localDateTime.minute

    if (is24Hour) {
      String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    } else {
      val hour12 =
          when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
          }
      val amPm = if (hour < 12) "AM" else "PM"
      String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
    }
  } catch (e: Exception) {
    "—"
  }
}

/**
 * Formats an ISO 8601 datetime string for display (e.g. "Mar 6, 2025 2:30 PM") in the device's
 * local timezone.
 */
fun formatTimestampForDisplay(context: Context, isoString: String): String {
  return try {
    val millis = kotlin.time.Instant.parse(isoString).toEpochMilliseconds()
    val dateFlags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME
    DateUtils.formatDateTime(context, millis, dateFlags)
  } catch (e: Exception) {
    isoString
  }
}

/**
 * Formats an ISO 8601 datetime string as date-only for display (e.g. "Jan 15, 2024") in the
 * device's local timezone.
 */
fun formatDateForDisplay(context: Context, isoString: String): String {
  return try {
    val millis = kotlin.time.Instant.parse(isoString).toEpochMilliseconds()
    DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_DATE)
  } catch (e: Exception) {
    isoString
  }
}
