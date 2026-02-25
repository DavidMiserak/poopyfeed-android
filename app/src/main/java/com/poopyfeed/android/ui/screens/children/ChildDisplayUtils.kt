package com.poopyfeed.android.ui.screens.children

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar

/**
 * Shared display helpers for child-related UI (list and dashboard).
 */
object ChildDisplayUtils {
    fun getChildAge(dateOfBirth: String): String {
        return try {
            val parts = dateOfBirth.split("-").map { it.toIntOrNull() ?: return "Unknown age" }
            if (parts.size < 3) return "Unknown age"
            val year = parts[0]
            val month = parts[1]
            val day = parts[2]
            val today = Calendar.getInstance()
            val birth = Calendar.getInstance().apply { set(year, month - 1, day) }
            var years = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
            var months = today.get(Calendar.MONTH) - birth.get(Calendar.MONTH)
            if (months < 0) {
                years--
                months += 12
            }
            if (today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH)) {
                months--
                if (months < 0) {
                    years--
                    months += 12
                }
            }
            when {
                years > 0 && months > 0 -> "$years y $months m"
                years > 0 -> "$years y"
                months > 0 -> "$months m"
                else -> "0 m"
            }
        } catch (_: Exception) {
            "Unknown age"
        }
    }

    fun getGenderEmoji(gender: String?): String =
        when (gender?.uppercase()) {
            "M" -> "👦"
            "F" -> "👧"
            else -> "👶"
        }

    /**
     * Formats an ISO-8601 timestamp for display (e.g. "Today 2:30 PM", "Yesterday", "Jan 15").
     */
    fun formatTimestamp(iso: String?): String {
        if (iso.isNullOrBlank()) return "—"
        return try {
            val instant = Instant.parse(iso)
            val zoned = instant.atZone(ZoneId.systemDefault())
            val date = zoned.toLocalDate()
            val today = LocalDate.now()
            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
            val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
            when {
                date == today -> "Today ${zoned.format(timeFormatter)}"
                date == today.minusDays(1) -> "Yesterday ${zoned.format(timeFormatter)}"
                else -> zoned.format(dateFormatter)
            }
        } catch (_: Exception) {
            "—"
        }
    }

    fun formatMinutes(minutes: Int): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h ${m}m"
    }
}
