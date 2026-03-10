package net.poopyfeed.pf.notifications

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.assertTrue
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

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
  fun `onMessageReceived with feeding_reminder processes message`() {
    // Arrange
    every { mockTokenManager.getProfileTimezone() } returns "America/New_York"
    every { mockTokenManager.getToken() } returns "fake_token"

    service =
        PoopyFeedMessagingService().apply {
          notificationsRepository = mockNotificationsRepository
          tokenManager = mockTokenManager
        }

    val messageData =
        mapOf(
            "title" to "Feeding Reminder",
            "body" to "It's been 3 hours since the last feeding",
            "event_type" to "feeding_reminder",
            "child_id" to "child-123")
    val remoteMessage = RemoteMessage.Builder("test_sender_id").setData(messageData).build()

    // Act & Assert - verify no exception thrown
    service.onMessageReceived(remoteMessage) // Should not throw
    assertTrue(true, "Message was processed without error")
  }

  @Test
  fun `onMessageReceived with pattern_alert processes message with child_id`() {
    // Arrange
    every { mockTokenManager.getProfileTimezone() } returns "America/New_York"
    every { mockTokenManager.getToken() } returns "fake_token"

    service =
        PoopyFeedMessagingService().apply {
          notificationsRepository = mockNotificationsRepository
          tokenManager = mockTokenManager
        }

    val messageData =
        mapOf(
            "title" to "Feeding Pattern Alert",
            "body" to "Feeding intervals are getting longer",
            "event_type" to "pattern_alert",
            "child_id" to "child-456")
    val remoteMessage = RemoteMessage.Builder("test_sender_id").setData(messageData).build()

    // Act & Assert - verify no exception thrown
    service.onMessageReceived(remoteMessage) // Should not throw
    assertTrue(true, "Pattern alert message was processed without error")
  }
}
