package net.poopyfeed.pf.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.poopyfeed.pf.MainActivity
import net.poopyfeed.pf.R
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.QuietHours
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.util.QuietHoursChecker

/**
 * FCM message handler for PoopyFeed push notifications.
 *
 * Handles two concerns:
 * 1. Token refresh — re-registers with backend when FCM rotates the device token
 * 2. Data messages — displays Android notifications respecting quiet hours
 *
 * Feeding reminders bypass quiet hours (matching backend behavior).
 */
@AndroidEntryPoint
class PoopyFeedMessagingService : FirebaseMessagingService() {

  @Inject lateinit var notificationsRepository: NotificationsRepository
  @Inject lateinit var tokenManager: TokenManager
  @Inject lateinit var analyticsTracker: AnalyticsTracker

  private val serviceJob = SupervisorJob()
  private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d(TAG, "FCM token refreshed")

    // Only register if user is logged in
    if (tokenManager.getToken() == null) return

    serviceScope.launch {
      try {
        notificationsRepository.registerDeviceToken(token)
        Log.d(TAG, "FCM token registered with backend")
      } catch (e: Exception) {
        Log.w(TAG, "Failed to register FCM token", e)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }

  override fun onMessageReceived(message: RemoteMessage) {
    super.onMessageReceived(message)

    val data = message.data
    val title = data["title"] ?: return
    val body = data["body"] ?: return
    val eventType = data["event_type"] ?: ""
    val childId = data["child_id"]

    // Check quiet hours (feeding reminders bypass, matching backend)
    if (eventType != "feeding_reminder") {
      val quietHours = loadCachedQuietHours()
      val timezoneId = tokenManager.getProfileTimezone() ?: "UTC"
      if (QuietHoursChecker.isQuietNow(quietHours, timezoneId)) {
        Log.d(TAG, "Suppressed notification during quiet hours: $eventType")
        return
      }
    }

    showNotification(title, body, eventType, childId)
  }

  private fun showNotification(title: String, body: String, eventType: String, childId: String?) {
    analyticsTracker.logNotificationOpened(eventType, childId)
    val channelId = getChannelId(eventType)

    // Build deep link URI from child_id (if present)
    val deepLinkUri = childId?.let { "poopyfeed://app/children/$it" }

    val intent =
        Intent(this, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          if (deepLinkUri != null) putExtra("deep_link", deepLinkUri)
        }

    val pendingIntent =
        PendingIntent.getActivity(
            this,
            childId?.hashCode() ?: 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    val notification =
        NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(
                if (eventType == "feeding_reminder") NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT)
            .build()

    val notificationManager = getSystemService(NotificationManager::class.java)
    notificationManager.notify(notificationIdCounter.getAndIncrement(), notification)
  }

  private fun loadCachedQuietHours(): QuietHours? {
    // Load from SharedPreferences cache (set by AccountSettingsViewModel)
    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    val enabled = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false)
    if (!enabled) return null
    val startTime = prefs.getString(KEY_QUIET_HOURS_START, "22:00:00") ?: "22:00:00"
    val endTime = prefs.getString(KEY_QUIET_HOURS_END, "07:00:00") ?: "07:00:00"
    return QuietHours(enabled = true, startTime = startTime, endTime = endTime)
  }

  private fun getChannelId(eventType: String): String = getChannelIdForEventType(eventType)

  companion object {
    /** Maps FCM event_type to notification channel ID. Exposed for unit tests. */
    internal fun getChannelIdForEventType(eventType: String): String =
        when (eventType) {
          "feeding_reminder" -> CHANNEL_FEEDING_REMINDERS
          "pattern_alert" -> CHANNEL_PATTERN_ALERTS
          else -> CHANNEL_ACTIVITY_ALERTS
        }

    private const val TAG = "PoopyFeedFCM"
    const val PREFS_NAME = "poopyfeed_prefs"
    const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
    const val KEY_QUIET_HOURS_START = "quiet_hours_start"
    const val KEY_QUIET_HOURS_END = "quiet_hours_end"
    const val CHANNEL_ACTIVITY_ALERTS = "activity_alerts"
    const val CHANNEL_FEEDING_REMINDERS = "feeding_reminders"
    const val CHANNEL_PATTERN_ALERTS = "pattern_alerts"
    private val notificationIdCounter = AtomicInteger(0)

    /** Create notification channels. Call from Application.onCreate(). */
    fun createNotificationChannels(context: android.content.Context) {
      val manager = context.getSystemService(NotificationManager::class.java)

      val activityChannel =
          NotificationChannel(
                  CHANNEL_ACTIVITY_ALERTS,
                  "Activity Alerts",
                  NotificationManager.IMPORTANCE_DEFAULT,
              )
              .apply { description = "Notifications when someone logs a feeding, diaper, or nap" }

      val feedingChannel =
          NotificationChannel(
                  CHANNEL_FEEDING_REMINDERS,
                  "Feeding Reminders",
                  NotificationManager.IMPORTANCE_HIGH,
              )
              .apply { description = "Reminders when it's been a while since the last feeding" }

      val patternChannel =
          NotificationChannel(
                  CHANNEL_PATTERN_ALERTS,
                  "Pattern Alerts",
                  NotificationManager.IMPORTANCE_DEFAULT,
              )
              .apply { description = "Alerts when feeding or nap patterns deviate from normal" }

      manager.createNotificationChannels(listOf(activityChannel, feedingChannel, patternChannel))
    }
  }
}
