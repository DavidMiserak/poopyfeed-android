package net.poopyfeed.pf.util

import net.poopyfeed.pf.data.models.QuietHours
import java.util.Calendar
import java.util.TimeZone

/**
 * Checks whether the current time falls within a user's quiet hours in a given timezone.
 * Used for local enforcement (e.g. when FCM is implemented, suppress displaying notifications
 * during quiet hours).
 *
 * Logic matches backend [notifications.models.QuietHours.is_quiet_now]: same-day range
 * (start <= end) means quiet when start <= now <= end; overnight range (start > end) means
 * quiet when now >= start || now <= end. Times are interpreted in the user's profile timezone.
 */
object QuietHoursChecker {

  /**
   * Returns true if [quietHours] is enabled and the current time in [timezoneId] falls within
   * the configured window. Returns false if [quietHours] is null or disabled.
   *
   * @param quietHours Loaded quiet hours (e.g. from API); null or enabled=false → false
   * @param timezoneId IANA timezone ID (e.g. "America/New_York") from user profile
   */
  @JvmStatic
  fun isQuietNow(quietHours: QuietHours?, timezoneId: String): Boolean {
    val nowSeconds = nowLocalSecondsSinceMidnight(timezoneId)
    return isQuietNow(quietHours, nowSeconds)
  }

  /**
   * Same as [isQuietNow] but with [nowSecondsSinceMidnight] (0–86399) injected for testing.
   * Production code uses the two-arg overload.
   */
  @JvmStatic
  internal fun isQuietNow(quietHours: QuietHours?, nowSecondsSinceMidnight: Int): Boolean {
    if (quietHours == null || !quietHours.enabled) return false

    val startSeconds = parseTimeToSecondsSinceMidnight(quietHours.startTime) ?: return false
    val endSeconds = parseTimeToSecondsSinceMidnight(quietHours.endTime) ?: return false
    val nowSeconds = nowSecondsSinceMidnight

    return if (startSeconds <= endSeconds) {
      // Same-day range (e.g. 09:00 to 17:00)
      nowSeconds in startSeconds..endSeconds
    } else {
      // Overnight range (e.g. 22:00 to 07:00)
      nowSeconds >= startSeconds || nowSeconds <= endSeconds
    }
  }

  /**
   * Parses "HH:mm:ss" or "HH:mm" to seconds since midnight. Returns null if invalid.
   */
  private fun parseTimeToSecondsSinceMidnight(value: String): Int? {
    val trimmed = value.trim()
    if (trimmed.isEmpty()) return null
    val parts = trimmed.split(":")
    if (parts.size == 2) {
      val h = parts[0].toIntOrNull() ?: return null
      val m = parts[1].toIntOrNull() ?: return null
      if (h in 0..23 && m in 0..59) return h * 3600 + m * 60
      return null
    }
    if (parts.size == 3) {
      val h = parts[0].toIntOrNull() ?: return null
      val m = parts[1].toIntOrNull() ?: return null
      val s = parts[2].toIntOrNull() ?: return null
      if (h in 0..23 && m in 0..59 && s in 0..59) return h * 3600 + m * 60 + s
      return null
    }
    return null
  }

  private fun nowLocalSecondsSinceMidnight(timezoneId: String): Int {
    val zone = TimeZone.getTimeZone(timezoneId)
    val cal = Calendar.getInstance(zone)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    return hour * 3600 + minute * 60 + second
  }
}
