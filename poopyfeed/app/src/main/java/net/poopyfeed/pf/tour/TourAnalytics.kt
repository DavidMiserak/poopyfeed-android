package net.poopyfeed.pf.tour

import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton
import net.poopyfeed.pf.analytics.AnalyticsTracker

@Singleton
class TourAnalytics @Inject constructor(private val analyticsTracker: AnalyticsTracker) {

  fun logTourStarted(part: Int) {
    analyticsTracker.logEvent("tour_started", Bundle().apply { putInt("part", part) })
  }

  fun logTourStepViewed(part: Int, step: Int) {
    analyticsTracker.logEvent(
        "tour_step_viewed", Bundle().apply {
          putInt("part", part)
          putInt("step", step)
        })
  }

  fun logTourCompleted(part: Int) {
    analyticsTracker.logEvent("tour_completed", Bundle().apply { putInt("part", part) })
  }

  fun logTourSkipped(part: Int, step: Int) {
    analyticsTracker.logEvent(
        "tour_skipped", Bundle().apply {
          putInt("part", part)
          putInt("step", step)
        })
  }
}
