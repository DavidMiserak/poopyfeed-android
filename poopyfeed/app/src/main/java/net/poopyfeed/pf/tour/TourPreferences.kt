package net.poopyfeed.pf.tour

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TourPreferences @Inject constructor(private val prefs: SharedPreferences) {

  fun shouldShowPart(part: Int): Boolean {
    val key = keyForPart(part) ?: return false
    return !prefs.getBoolean(key, false)
  }

  fun markPartSeen(part: Int) {
    val key = keyForPart(part) ?: return
    prefs.edit { putBoolean(key, true) }
  }

  fun resetAll() {
    prefs.edit {
      remove(KEY_PART_1)
      remove(KEY_PART_2)
      remove(KEY_PART_3)
    }
  }

  fun clearAll() = resetAll()

  private fun keyForPart(part: Int): String? =
      when (part) {
        1 -> KEY_PART_1
        2 -> KEY_PART_2
        3 -> KEY_PART_3
        else -> null
      }

  companion object {
    const val PREFS_NAME = "tour_prefs"
    private const val KEY_PART_1 = "has_seen_tour_part1"
    private const val KEY_PART_2 = "has_seen_tour_part2"
    private const val KEY_PART_3 = "has_seen_tour_part3"
  }
}
