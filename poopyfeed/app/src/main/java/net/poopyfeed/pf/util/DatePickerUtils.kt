package net.poopyfeed.pf.util

import java.time.LocalDateTime as JavaLocalDateTime
import java.time.ZoneId
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Utilities for MaterialDatePicker and MaterialTimePicker timezone handling.
 *
 * IMPORTANT: MaterialDatePicker uses UTC internally.
 * - setSelection() expects midnight UTC of the desired date.
 * - addOnPositiveButtonClickListener returns midnight UTC of the selected date.
 * - NEVER convert picker millis to a non-UTC timezone to extract the date, as this shifts the date
 *   backward for timezones behind UTC.
 */
object DatePickerUtils {

  /**
   * Gets the epoch millis to pass to MaterialDatePicker.setSelection() so it displays the correct
   * date for the given UTC timestamp in the specified timezone.
   *
   * Example: UTC "2024-03-10T02:00:00Z" in "US/Eastern" (UTC-5) is March 9. Returns midnight UTC of
   * March 9 so the picker shows March 9.
   *
   * @param utcIso UTC ISO 8601 timestamp
   * @param timezoneId Profile timezone ID (e.g., "US/Eastern"). Null defaults to UTC.
   * @return Epoch millis representing midnight UTC of the date in the given timezone
   */
  fun datePickerSelectionMillis(utcIso: String, timezoneId: String?): Long {
    return try {
      val tzId = timezoneId ?: "UTC"
      val instant = Instant.parse(utcIso)
      val localDate = instant.toLocalDateTime(TimeZone.of(tzId)).date

      // MaterialDatePicker expects midnight UTC for the desired date
      val javaLdt =
          JavaLocalDateTime.of(localDate.year, localDate.monthNumber, localDate.dayOfMonth, 0, 0, 0)
      javaLdt.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli()
    } catch (_: Exception) {
      System.currentTimeMillis()
    }
  }

  /**
   * Extracts the hour and minute from a UTC ISO timestamp, converted to the given timezone. Used to
   * initialize MaterialTimePicker with the correct local time.
   *
   * @param utcIso UTC ISO 8601 timestamp
   * @param timezoneId Profile timezone ID. Null defaults to UTC.
   * @return Pair of (hour, minute) in the given timezone
   */
  fun timePickerHourMinute(utcIso: String, timezoneId: String?): Pair<Int, Int> {
    return try {
      val tzId = timezoneId ?: "UTC"
      val instant = Instant.parse(utcIso)
      val local = instant.toLocalDateTime(TimeZone.of(tzId))
      Pair(local.hour, local.minute)
    } catch (_: Exception) {
      Pair(0, 0)
    }
  }

  /**
   * Extracts the date the user selected from MaterialDatePicker's returned millis.
   *
   * MaterialDatePicker returns midnight UTC of the selected date. We read it back in UTC to get the
   * correct date. NEVER read in a non-UTC timezone — this shifts the date.
   *
   * @param pickerMillis Epoch millis returned by
   *   MaterialDatePicker.addOnPositiveButtonClickListener
   * @return Triple of (year, month, day) representing the selected date
   */
  fun extractSelectedDate(pickerMillis: Long): Triple<Int, Int, Int> {
    val utcDate = Instant.fromEpochMilliseconds(pickerMillis).toLocalDateTime(TimeZone.UTC)
    return Triple(utcDate.year, utcDate.monthNumber, utcDate.dayOfMonth)
  }

  /**
   * Builds a UTC ISO 8601 string from a date (from the date picker) and time (from the time
   * picker), treating both as being in the given timezone.
   *
   * @param year Year from date picker
   * @param month Month from date picker (1-12)
   * @param day Day from date picker
   * @param hour Hour from time picker
   * @param minute Minute from time picker
   * @param timezoneId Profile timezone ID. Null defaults to UTC.
   * @return UTC ISO 8601 string (e.g., "2024-03-09T09:00:00Z")
   */
  fun toUtcIso(
      year: Int,
      month: Int,
      day: Int,
      hour: Int,
      minute: Int,
      timezoneId: String?
  ): String {
    return try {
      val tzId = timezoneId ?: "UTC"
      val javaLdt = JavaLocalDateTime.of(year, month, day, hour, minute, 0)
      val zonedDt = javaLdt.atZone(ZoneId.of(tzId))
      Instant.fromEpochMilliseconds(zonedDt.toInstant().toEpochMilli()).toString()
    } catch (_: Exception) {
      "${String.format("%04d-%02d-%02dT%02d:%02d:00", year, month, day, hour, minute)}Z"
    }
  }
}
