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
  fun `logScreenView calls logEvent with screen_view event name`() {
    tracker.logScreenView(screenName = "HomeFragment", screenClass = "HomeFragment")

    verify(exactly = 1) { firebaseAnalytics.logEvent("screen_view", any<Bundle>()) }
  }

  @Test
  fun `logScreenView includes screen_name parameter`() {
    tracker.logScreenView(screenName = "TestScreen", screenClass = "TestClass")

    verify {
      firebaseAnalytics.logEvent(
          "screen_view",
          match<Bundle> { bundle -> bundle.getString("screen_name") == "TestScreen" })
    }
  }

  @Test
  fun `logLoginSuccess calls logEvent with login event name`() {
    tracker.logLoginSuccess()

    verify(exactly = 1) { firebaseAnalytics.logEvent("login", any<Bundle>()) }
  }

  @Test
  fun `logLoginSuccess includes method parameter`() {
    tracker.logLoginSuccess()

    verify {
      firebaseAnalytics.logEvent(
          "login", match<Bundle> { bundle -> bundle.getString("method") == "email" })
    }
  }

  @Test
  fun `logSignupSuccess calls logEvent with sign_up event name`() {
    tracker.logSignupSuccess()

    verify(exactly = 1) { firebaseAnalytics.logEvent("sign_up", any<Bundle>()) }
  }

  @Test
  fun `logSignupSuccess includes method parameter`() {
    tracker.logSignupSuccess()

    verify {
      firebaseAnalytics.logEvent(
          "sign_up", match<Bundle> { bundle -> bundle.getString("method") == "email" })
    }
  }

  @Test
  fun `logLogout calls logEvent with logout event name`() {
    tracker.logLogout()

    verify(exactly = 1) { firebaseAnalytics.logEvent("logout", any<Bundle>()) }
  }

  @Test
  fun `logChildCreated calls logEvent with child_created event name`() {
    tracker.logChildCreated(childCount = 2)

    verify(exactly = 1) { firebaseAnalytics.logEvent("child_created", any<Bundle>()) }
  }

  @Test
  fun `logChildCreated includes child_count parameter`() {
    tracker.logChildCreated(childCount = 2)

    verify {
      firebaseAnalytics.logEvent(
          "child_created", match<Bundle> { bundle -> bundle.getInt("child_count") == 2 })
    }
  }

  @Test
  fun `logChildDeleted calls logEvent with child_deleted event name`() {
    tracker.logChildDeleted(childCount = 1)

    verify(exactly = 1) { firebaseAnalytics.logEvent("child_deleted", any<Bundle>()) }
  }

  @Test
  fun `logChildDeleted includes child_count parameter`() {
    tracker.logChildDeleted(childCount = 1)

    verify {
      firebaseAnalytics.logEvent(
          "child_deleted", match<Bundle> { bundle -> bundle.getInt("child_count") == 1 })
    }
  }

  @Test
  fun `logFeedingLogged calls logEvent with feeding_logged event name`() {
    tracker.logFeedingLogged(feedingType = "breast")

    verify(exactly = 1) { firebaseAnalytics.logEvent("feeding_logged", any<Bundle>()) }
  }

  @Test
  fun `logFeedingLogged includes feeding_type parameter`() {
    tracker.logFeedingLogged(feedingType = "breast")

    verify {
      firebaseAnalytics.logEvent(
          "feeding_logged",
          match<Bundle> { bundle -> bundle.getString("feeding_type") == "breast" })
    }
  }

  @Test
  fun `logDiaperLogged calls logEvent with diaper_logged event name`() {
    tracker.logDiaperLogged(changeType = "wet")

    verify(exactly = 1) { firebaseAnalytics.logEvent("diaper_logged", any<Bundle>()) }
  }

  @Test
  fun `logDiaperLogged includes change_type parameter`() {
    tracker.logDiaperLogged(changeType = "wet")

    verify {
      firebaseAnalytics.logEvent(
          "diaper_logged", match<Bundle> { bundle -> bundle.getString("change_type") == "wet" })
    }
  }

  @Test
  fun `logNapLogged calls logEvent with nap_logged event name`() {
    tracker.logNapLogged(durationMinutes = 60)

    verify(exactly = 1) { firebaseAnalytics.logEvent("nap_logged", any<Bundle>()) }
  }

  @Test
  fun `logNapLogged includes duration_minutes parameter`() {
    tracker.logNapLogged(durationMinutes = 60)

    verify {
      firebaseAnalytics.logEvent(
          "nap_logged", match<Bundle> { bundle -> bundle.getInt("duration_minutes") == 60 })
    }
  }

  @Test
  fun `logPasswordChanged calls logEvent with password_changed event name`() {
    tracker.logPasswordChanged()

    verify(exactly = 1) { firebaseAnalytics.logEvent("password_changed", any<Bundle>()) }
  }

  @Test
  fun `logAccountDeleted calls logEvent with account_deleted event name`() {
    tracker.logAccountDeleted()

    verify(exactly = 1) { firebaseAnalytics.logEvent("account_deleted", any<Bundle>()) }
  }

  @Test
  fun `logDeepLinkOpened calls logEvent with deep_link_opened event name`() {
    tracker.logDeepLinkOpened(uriPath = "poopyfeed://app/children/123")

    verify(exactly = 1) { firebaseAnalytics.logEvent("deep_link_opened", any<Bundle>()) }
  }

  @Test
  fun `logDeepLinkOpened includes uri_path parameter`() {
    tracker.logDeepLinkOpened(uriPath = "poopyfeed://app/children/123")

    verify {
      firebaseAnalytics.logEvent(
          "deep_link_opened",
          match<Bundle> { bundle ->
            bundle.getString("uri_path") == "poopyfeed://app/children/123"
          })
    }
  }

  @Test
  fun `logOfflineSyncCompleted calls logEvent with offline_sync_completed event name`() {
    tracker.logOfflineSyncCompleted(itemsSynced = 5)

    verify(exactly = 1) { firebaseAnalytics.logEvent("offline_sync_completed", any<Bundle>()) }
  }

  @Test
  fun `logOfflineSyncCompleted includes items_synced parameter`() {
    tracker.logOfflineSyncCompleted(itemsSynced = 5)

    verify {
      firebaseAnalytics.logEvent(
          "offline_sync_completed", match<Bundle> { bundle -> bundle.getInt("items_synced") == 5 })
    }
  }

  @Test
  fun `logNotificationOpened calls logEvent with notification_opened event name`() {
    tracker.logNotificationOpened(eventType = "feeding", childId = "child-123")

    verify(exactly = 1) { firebaseAnalytics.logEvent("notification_opened", any<Bundle>()) }
  }

  @Test
  fun `logNotificationOpened includes event_type and item_id parameters`() {
    tracker.logNotificationOpened(eventType = "feeding", childId = "child-123")

    verify {
      firebaseAnalytics.logEvent(
          "notification_opened",
          match<Bundle> { bundle ->
            bundle.getString("event_type") == "feeding" &&
                bundle.getString("item_id") == "child-123"
          })
    }
  }

  @Test
  fun `logNotificationOpened handles different event types correctly`() {
    tracker.logNotificationOpened(eventType = "nap", childId = "child-456")

    verify {
      firebaseAnalytics.logEvent(
          "notification_opened",
          match<Bundle> { bundle ->
            bundle.getString("event_type") == "nap" && bundle.getString("item_id") == "child-456"
          })
    }
  }

  @Test
  fun `logError calls logEvent with app_error event name`() {
    tracker.logError(errorType = "NetworkError", errorMessage = "Connection timeout")

    verify(exactly = 1) { firebaseAnalytics.logEvent("app_error", any<Bundle>()) }
  }

  @Test
  fun `logError includes error_type and error_message parameters`() {
    tracker.logError(errorType = "NetworkError", errorMessage = "Connection timeout")

    verify {
      firebaseAnalytics.logEvent(
          "app_error",
          match<Bundle> { bundle ->
            bundle.getString("error_type") == "NetworkError" &&
                bundle.getString("error_message") == "Connection timeout"
          })
    }
  }

  @Test
  fun `logFeedingLogged with different feeding types`() {
    tracker.logFeedingLogged(feedingType = "bottle")

    verify {
      firebaseAnalytics.logEvent(
          "feeding_logged",
          match<Bundle> { bundle -> bundle.getString("feeding_type") == "bottle" })
    }
  }

  @Test
  fun `logDiaperLogged with different change types`() {
    tracker.logDiaperLogged(changeType = "poop")

    verify {
      firebaseAnalytics.logEvent(
          "diaper_logged", match<Bundle> { bundle -> bundle.getString("change_type") == "poop" })
    }
  }
}
