package net.poopyfeed.pf.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AnalyticsTrackerTest {

  private lateinit var firebaseAnalytics: FirebaseAnalytics
  private lateinit var tracker: AnalyticsTracker

  @Before
  fun setup() {
    firebaseAnalytics = mockk(relaxed = true)
    tracker = AnalyticsTracker(firebaseAnalytics)
    assertNotNull(tracker)
  }

  @Test
  fun `tracker is created successfully`() {
    assertNotNull(tracker)
  }

  @Test
  fun `logScreenView calls logEvent with correct event name`() {
    tracker.logScreenView(screenName = "HomeFragment", screenClass = "HomeFragment")

    verify(exactly = 1) { firebaseAnalytics.logEvent("screen_view", any()) }
  }

  @Test
  fun `logLoginSuccess calls logEvent with correct event name`() {
    tracker.logLoginSuccess()

    verify(exactly = 1) { firebaseAnalytics.logEvent("login", any()) }
  }

  @Test
  fun `logSignupSuccess calls logEvent with correct event name`() {
    tracker.logSignupSuccess()

    verify(exactly = 1) { firebaseAnalytics.logEvent("sign_up", any()) }
  }

  @Test
  fun `logLogout calls logEvent with correct event name`() {
    tracker.logLogout()

    verify(exactly = 1) { firebaseAnalytics.logEvent("logout", any()) }
  }

  @Test
  fun `logChildCreated calls logEvent with correct event name`() {
    tracker.logChildCreated(childCount = 2)

    verify(exactly = 1) { firebaseAnalytics.logEvent("child_created", any()) }
  }

  @Test
  fun `logChildDeleted calls logEvent with correct event name`() {
    tracker.logChildDeleted(childCount = 1)

    verify(exactly = 1) { firebaseAnalytics.logEvent("child_deleted", any()) }
  }

  @Test
  fun `logFeedingLogged calls logEvent with correct event name`() {
    tracker.logFeedingLogged(feedingType = "breast")

    verify(exactly = 1) { firebaseAnalytics.logEvent("feeding_logged", any()) }
  }

  @Test
  fun `logDiaperLogged calls logEvent with correct event name`() {
    tracker.logDiaperLogged(changeType = "wet")

    verify(exactly = 1) { firebaseAnalytics.logEvent("diaper_logged", any()) }
  }

  @Test
  fun `logNapLogged calls logEvent with correct event name`() {
    tracker.logNapLogged(durationMinutes = 60)

    verify(exactly = 1) { firebaseAnalytics.logEvent("nap_logged", any()) }
  }

  @Test
  fun `logPasswordChanged calls logEvent with correct event name`() {
    tracker.logPasswordChanged()

    verify(exactly = 1) { firebaseAnalytics.logEvent("password_changed", any()) }
  }

  @Test
  fun `logAccountDeleted calls logEvent with correct event name`() {
    tracker.logAccountDeleted()

    verify(exactly = 1) { firebaseAnalytics.logEvent("account_deleted", any()) }
  }

  @Test
  fun `logDeepLinkOpened calls logEvent with correct event name`() {
    tracker.logDeepLinkOpened(uriPath = "poopyfeed://app/children/123")

    verify(exactly = 1) { firebaseAnalytics.logEvent("deep_link_opened", any()) }
  }

  @Test
  fun `logOfflineSyncCompleted calls logEvent with correct event name`() {
    tracker.logOfflineSyncCompleted(itemsSynced = 5)

    verify(exactly = 1) { firebaseAnalytics.logEvent("offline_sync_completed", any()) }
  }

  @Test
  fun `logNotificationOpened calls logEvent with correct event name`() {
    tracker.logNotificationOpened(eventType = "feeding", childId = "child-123")

    verify(exactly = 1) { firebaseAnalytics.logEvent("notification_opened", any()) }
  }

  @Test
  fun `logNotificationOpened handles different event types correctly`() {
    tracker.logNotificationOpened(eventType = "nap", childId = "child-456")

    verify(exactly = 1) { firebaseAnalytics.logEvent("notification_opened", any()) }
  }

  @Test
  fun `logError calls logEvent with correct event name`() {
    tracker.logError(errorType = "NetworkError", errorMessage = "Connection timeout")

    verify(exactly = 1) { firebaseAnalytics.logEvent("app_error", any()) }
  }

  @Test
  fun `logFeedingLogged with different feeding types`() {
    tracker.logFeedingLogged(feedingType = "bottle")

    verify(exactly = 1) { firebaseAnalytics.logEvent("feeding_logged", any()) }
  }

  @Test
  fun `logDiaperLogged with different change types`() {
    tracker.logDiaperLogged(changeType = "poop")

    verify(exactly = 1) { firebaseAnalytics.logEvent("diaper_logged", any()) }
  }
}
