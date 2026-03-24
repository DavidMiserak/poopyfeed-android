package net.poopyfeed.pf.tour

import android.os.Bundle
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.assertEquals
import net.poopyfeed.pf.analytics.AnalyticsTracker
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TourAnalyticsTest {

  private lateinit var analyticsTracker: AnalyticsTracker
  private lateinit var tourAnalytics: TourAnalytics

  @Before
  fun setup() {
    analyticsTracker = mockk(relaxed = true)
    tourAnalytics = TourAnalytics(analyticsTracker)
  }

  @Test
  fun `logTourStarted logs event with part number`() {
    val bundleSlot = slot<Bundle>()
    tourAnalytics.logTourStarted(1)
    verify { analyticsTracker.logEvent("tour_started", capture(bundleSlot)) }
    assertEquals(1, bundleSlot.captured.getInt("part"))
  }

  @Test
  fun `logTourStepViewed logs event with part and step`() {
    val bundleSlot = slot<Bundle>()
    tourAnalytics.logTourStepViewed(2, 3)
    verify { analyticsTracker.logEvent("tour_step_viewed", capture(bundleSlot)) }
    assertEquals(2, bundleSlot.captured.getInt("part"))
    assertEquals(3, bundleSlot.captured.getInt("step"))
  }

  @Test
  fun `logTourCompleted logs event with part number`() {
    val bundleSlot = slot<Bundle>()
    tourAnalytics.logTourCompleted(1)
    verify { analyticsTracker.logEvent("tour_completed", capture(bundleSlot)) }
    assertEquals(1, bundleSlot.captured.getInt("part"))
  }

  @Test
  fun `logTourSkipped logs event with part and step`() {
    val bundleSlot = slot<Bundle>()
    tourAnalytics.logTourSkipped(2, 1)
    verify { analyticsTracker.logEvent("tour_skipped", capture(bundleSlot)) }
    assertEquals(2, bundleSlot.captured.getInt("part"))
    assertEquals(1, bundleSlot.captured.getInt("step"))
  }
}
