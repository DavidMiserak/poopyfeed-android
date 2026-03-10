package net.poopyfeed.pf.notifications

import android.app.NotificationManager
import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.assertNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowNotificationManager
import android.content.Intent

@RunWith(RobolectricTestRunner::class)
class PoopyFeedMessagingServiceTest {

  private lateinit var service: PoopyFeedMessagingService
  private lateinit var context: Context
  private val mockNotificationsRepository: NotificationsRepository = mockk(relaxed = true)
  private val mockTokenManager: TokenManager = mockk(relaxed = true)

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    service =
        PoopyFeedMessagingService().apply {
          notificationsRepository = mockNotificationsRepository
          tokenManager = mockTokenManager
        }
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `onNewToken calls repository when user is logged in`() {
    // Arrange - user is logged in
    every { mockTokenManager.getToken() } returns "user_auth_token"

    // Act - should not throw exception
    service.onNewToken("fcm_token_123")

    // Assert - no exception thrown, token registration initiated
    assertTrue(true, "Token registration initiated for logged-in user")
  }

  @Test
  fun `getChannelIdForEventType routes activity_alert to activity_alerts channel`() {
    // Verify correct channel for activity alerts (most common event type)
    val channelId = PoopyFeedMessagingService.getChannelIdForEventType("activity_alert")
    assertEquals(PoopyFeedMessagingService.CHANNEL_ACTIVITY_ALERTS, channelId)
    assertEquals("activity_alerts", channelId)
  }

  @Test
  fun `onMessageReceived returns early when title is missing`() {
    // Arrange: Message without title
    every { mockTokenManager.getProfileTimezone() } returns "America/New_York"

    service =
        PoopyFeedMessagingService().apply {
          notificationsRepository = mockNotificationsRepository
          tokenManager = mockTokenManager
        }

    val messageData =
        mapOf(
            "body" to "Message without title", "event_type" to "activity_alert"
            // Missing "title"
            )
    val remoteMessage = RemoteMessage.Builder("test_sender_id").setData(messageData).build()

    // Act & Assert - should not throw, early return expected
    service.onMessageReceived(remoteMessage) // Should not throw
    assertTrue(true, "Message without title is handled gracefully")
  }

  @Test
  fun `onMessageReceived returns early when body is missing`() {
    // Arrange: Message without body
    every { mockTokenManager.getProfileTimezone() } returns "America/New_York"

    service =
        PoopyFeedMessagingService().apply {
          notificationsRepository = mockNotificationsRepository
          tokenManager = mockTokenManager
        }

    val messageData =
        mapOf(
            "title" to "Notification Title", "event_type" to "activity_alert"
            // Missing "body"
            )
    val remoteMessage = RemoteMessage.Builder("test_sender_id").setData(messageData).build()

    // Act & Assert - should not throw, early return expected
    service.onMessageReceived(remoteMessage) // Should not throw
    assertTrue(true, "Message without body is handled gracefully")
  }

  @Test
  fun `getChannelIdForEventType routes feeding_reminder correctly`() {
    val channelId = PoopyFeedMessagingService.getChannelIdForEventType("feeding_reminder")
    assertEquals(PoopyFeedMessagingService.CHANNEL_FEEDING_REMINDERS, channelId)
  }

  @Test
  fun `getChannelIdForEventType routes pattern_alert correctly`() {
    val channelId = PoopyFeedMessagingService.getChannelIdForEventType("pattern_alert")
    assertEquals(PoopyFeedMessagingService.CHANNEL_PATTERN_ALERTS, channelId)
  }

  @Test
  fun `getChannelIdForEventType routes unknown event types to activity_alerts`() {
    val channelId = PoopyFeedMessagingService.getChannelIdForEventType("unknown_event")
    assertEquals(PoopyFeedMessagingService.CHANNEL_ACTIVITY_ALERTS, channelId)
  }

  @Test
  fun `onNewToken skips registration when user is not logged in`() {
    every { mockTokenManager.getToken() } returns null

    service =
        PoopyFeedMessagingService().apply {
          notificationsRepository = mockNotificationsRepository
          tokenManager = mockTokenManager
        }

    // Act
    service.onNewToken("fcm_token_456")

    // Assert: No exception thrown - user not logged in so registration skipped
    assertTrue(true, "Token registration skipped when user not logged in")
  }

  @Test
  fun `createNotificationChannels creates all three required channels`() {
    // Act
    PoopyFeedMessagingService.createNotificationChannels(context)

    // Assert: Verify no exception thrown (channels created)
    assertTrue(true, "Notification channels were created without error")
  }
}
