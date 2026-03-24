package net.poopyfeed.pf.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that wraps Firebase Analytics and provides methods to log key user behavior events.
 *
 * All methods create an event bundle with relevant parameters and call [FirebaseAnalytics.logEvent]
 * with standardized event names and parameter names.
 */
@Singleton
class AnalyticsTracker @Inject constructor(private val firebaseAnalytics: FirebaseAnalytics) {

  /**
   * Logs a screen view event.
   *
   * @param screenName The name of the screen being viewed.
   * @param screenClass The class of the screen being viewed.
   */
  fun logScreenView(screenName: String, screenClass: String) {
    val bundle =
        Bundle().apply {
          putString("screen_name", screenName)
          putString("screen_class", screenClass)
        }
    firebaseAnalytics.logEvent("screen_view", bundle)
  }

  /** Logs a successful login event. */
  fun logLoginSuccess() {
    val bundle = Bundle().apply { putString("method", "email") }
    firebaseAnalytics.logEvent("login", bundle)
  }

  /** Logs a successful signup event. */
  fun logSignupSuccess() {
    val bundle = Bundle().apply { putString("method", "email") }
    firebaseAnalytics.logEvent("sign_up", bundle)
  }

  /** Logs a logout event. */
  fun logLogout() {
    firebaseAnalytics.logEvent("logout", Bundle())
  }

  /**
   * Logs when a child profile is created.
   *
   * @param childCount The total number of children after creation.
   */
  fun logChildCreated(childCount: Int) {
    val bundle = Bundle().apply { putInt("child_count", childCount) }
    firebaseAnalytics.logEvent("child_created", bundle)
  }

  /**
   * Logs when a child profile is deleted.
   *
   * @param childCount The total number of children after deletion.
   */
  fun logChildDeleted(childCount: Int) {
    val bundle = Bundle().apply { putInt("child_count", childCount) }
    firebaseAnalytics.logEvent("child_deleted", bundle)
  }

  /**
   * Logs when a feeding is recorded.
   *
   * @param feedingType The type of feeding (e.g., "breast", "bottle", "solid").
   */
  fun logFeedingLogged(feedingType: String) {
    val bundle = Bundle().apply { putString("feeding_type", feedingType) }
    firebaseAnalytics.logEvent("feeding_logged", bundle)
  }

  /**
   * Logs when a diaper change is recorded.
   *
   * @param changeType The type of change (e.g., "wet", "poop").
   */
  fun logDiaperLogged(changeType: String) {
    val bundle = Bundle().apply { putString("change_type", changeType) }
    firebaseAnalytics.logEvent("diaper_logged", bundle)
  }

  /**
   * Logs when a nap is recorded.
   *
   * @param durationMinutes The duration of the nap in minutes.
   */
  fun logNapLogged(durationMinutes: Int) {
    val bundle = Bundle().apply { putInt("duration_minutes", durationMinutes) }
    firebaseAnalytics.logEvent("nap_logged", bundle)
  }

  /** Logs when a user changes their password. */
  fun logPasswordChanged() {
    firebaseAnalytics.logEvent("password_changed", Bundle())
  }

  /** Logs when a user account is deleted. */
  fun logAccountDeleted() {
    firebaseAnalytics.logEvent("account_deleted", Bundle())
  }

  /**
   * Logs when a deep link is opened.
   *
   * @param uriPath The URI path that was opened.
   */
  fun logDeepLinkOpened(uriPath: String) {
    val bundle = Bundle().apply { putString("uri_path", uriPath) }
    firebaseAnalytics.logEvent("deep_link_opened", bundle)
  }

  /**
   * Logs when offline sync is completed.
   *
   * @param itemsSynced The number of items that were synced.
   */
  fun logOfflineSyncCompleted(itemsSynced: Int) {
    val bundle = Bundle().apply { putInt("items_synced", itemsSynced) }
    firebaseAnalytics.logEvent("offline_sync_completed", bundle)
  }

  /**
   * Logs when a push notification is opened.
   *
   * @param eventType The type of event the notification was about (e.g., "feeding", "nap").
   * @param childId The ID of the child the event is related to (optional).
   */
  fun logNotificationOpened(eventType: String, childId: String?) {
    val bundle =
        Bundle().apply {
          putString("event_type", eventType)
          if (childId != null) putString("item_id", childId)
        }
    firebaseAnalytics.logEvent("notification_opened", bundle)
  }

  /**
   * Logs a simple named event with no parameters.
   *
   * @param name The event name.
   */
  fun logEvent(name: String) {
    firebaseAnalytics.logEvent(name, Bundle())
  }

  /**
   * Logs a named event with parameters.
   *
   * @param name The event name.
   * @param params Firebase Analytics parameter bundle.
   */
  fun logEvent(name: String, params: Bundle) {
    firebaseAnalytics.logEvent(name, params)
  }

  /**
   * Logs an error event.
   *
   * @param errorType The type of error that occurred.
   * @param errorMessage The error message.
   */
  fun logError(errorType: String, errorMessage: String) {
    val bundle =
        Bundle().apply {
          putString("error_type", errorType)
          putString("error_message", errorMessage)
        }
    firebaseAnalytics.logEvent("app_error", bundle)
  }
}
