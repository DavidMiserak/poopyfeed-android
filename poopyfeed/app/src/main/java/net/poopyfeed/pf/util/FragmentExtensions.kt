package net.poopyfeed.pf.util

import androidx.fragment.app.Fragment
import net.poopyfeed.pf.analytics.AnalyticsTracker

/**
 * Extension function to log screen view events from Fragments. Call in onViewCreated() after UI
 * setup.
 *
 * @param analyticsTracker The AnalyticsTracker instance to use (typically from ViewModel)
 * @param screenName The human-readable screen name to log (e.g., "Home", "ChildrenList",
 *   "Timeline")
 */
fun Fragment.logScreenView(analyticsTracker: AnalyticsTracker, screenName: String) {
  analyticsTracker.logScreenView(screenName, this::class.java.simpleName)
}
