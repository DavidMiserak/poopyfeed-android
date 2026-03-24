package net.poopyfeed.pf.tour

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test

class TourManagerTest {

  private lateinit var tourPreferences: TourPreferences
  private lateinit var tourAnalytics: TourAnalytics
  private lateinit var tourManager: TourManager

  @Before
  fun setup() {
    tourPreferences = mockk(relaxed = true)
    tourAnalytics = mockk(relaxed = true)
    tourManager = TourManager(tourPreferences, tourAnalytics)
  }

  @Test
  fun `shouldShowPart delegates to TourPreferences`() {
    every { tourPreferences.shouldShowPart(1) } returns true
    assertTrue(tourManager.shouldShowPart(1))
    verify { tourPreferences.shouldShowPart(1) }
  }

  @Test
  fun `shouldShowPart returns false when already seen`() {
    every { tourPreferences.shouldShowPart(1) } returns false
    assertFalse(tourManager.shouldShowPart(1))
  }

  @Test
  fun `markPartComplete marks part seen and logs completion`() {
    tourManager.markPartComplete(2)
    verify { tourPreferences.markPartSeen(2) }
    verify { tourAnalytics.logTourCompleted(2) }
  }

  @Test
  fun `markPartSkipped marks part seen and logs skip`() {
    tourManager.markPartSkipped(1, 3)
    verify { tourPreferences.markPartSeen(1) }
    verify { tourAnalytics.logTourSkipped(1, 3) }
  }

  @Test
  fun `resetForReplay resets all tour preferences`() {
    tourManager.resetForReplay()
    verify { tourPreferences.resetAll() }
  }
}
