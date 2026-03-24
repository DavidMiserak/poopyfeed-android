package net.poopyfeed.pf.tour

import android.content.Context
import android.content.SharedPreferences
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TourPreferencesTest {

  private lateinit var prefs: SharedPreferences
  private lateinit var tourPreferences: TourPreferences

  @Before
  fun setup() {
    prefs =
        RuntimeEnvironment.getApplication()
            .getSharedPreferences("tour_prefs_test", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    tourPreferences = TourPreferences(prefs)
  }

  @Test
  fun `shouldShowPart returns true by default`() {
    assertTrue(tourPreferences.shouldShowPart(1))
    assertTrue(tourPreferences.shouldShowPart(2))
    assertTrue(tourPreferences.shouldShowPart(3))
  }

  @Test
  fun `markPartSeen sets flag to false`() {
    tourPreferences.markPartSeen(1)
    assertFalse(tourPreferences.shouldShowPart(1))
    assertTrue(tourPreferences.shouldShowPart(2))
  }

  @Test
  fun `resetAll clears all flags`() {
    tourPreferences.markPartSeen(1)
    tourPreferences.markPartSeen(2)
    tourPreferences.markPartSeen(3)
    tourPreferences.resetAll()
    assertTrue(tourPreferences.shouldShowPart(1))
    assertTrue(tourPreferences.shouldShowPart(2))
    assertTrue(tourPreferences.shouldShowPart(3))
  }

  @Test
  fun `clearAll clears all flags same as resetAll`() {
    tourPreferences.markPartSeen(1)
    tourPreferences.markPartSeen(2)
    tourPreferences.clearAll()
    assertTrue(tourPreferences.shouldShowPart(1))
    assertTrue(tourPreferences.shouldShowPart(2))
  }

  @Test
  fun `invalid part number returns false`() {
    assertFalse(tourPreferences.shouldShowPart(0))
    assertFalse(tourPreferences.shouldShowPart(4))
  }
}
