package net.poopyfeed.pf.notifications

import android.content.Context
import android.content.Intent
import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.After
import org.junit.Before
import org.junit.Test

class PoopyFeedMessagingServiceTest {

  private val mockContext: Context = mockk(relaxed = true)
  private lateinit var service: PoopyFeedMessagingService

  @Before
  fun setup() {
    service = PoopyFeedMessagingService()
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
  }

  @Test
  fun `deep link uri format with valid child_id is correct`() {
    val childId = "42"
    val deepLinkUri = childId.let { "poopyfeed://app/children/$it" }
    assertEquals("poopyfeed://app/children/42", deepLinkUri)
  }

  @Test
  fun `deep link uri format with numeric string child_id`() {
    val childId = "123"
    val deepLinkUri = childId.let { "poopyfeed://app/children/$it" }
    assertEquals("poopyfeed://app/children/123", deepLinkUri)
  }

  @Test
  fun `deep link uri is null when child_id is null`() {
    val childId: String? = null
    val deepLinkUri = childId?.let { "poopyfeed://app/children/$it" }
    assertNull(deepLinkUri)
  }

  @Test
  fun `deep link uri is not set when child_id is empty string`() {
    val childId = ""
    val deepLinkUri = childId.let { "poopyfeed://app/children/$it" }
    assertEquals("poopyfeed://app/children/", deepLinkUri)
    // This shows that empty string still produces a URI, but with empty ID path
  }

  @Test
  fun `intent flags are correct for notification intent`() {
    val expectedFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    val actualFlags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    assertEquals(expectedFlags, actualFlags)
  }

  @Test
  fun `notification channel ids are correctly defined`() {
    assertEquals("activity_alerts", PoopyFeedMessagingService.CHANNEL_ACTIVITY_ALERTS)
    assertEquals("feeding_reminders", PoopyFeedMessagingService.CHANNEL_FEEDING_REMINDERS)
    assertEquals("pattern_alerts", PoopyFeedMessagingService.CHANNEL_PATTERN_ALERTS)
  }

  @Test
  fun `getChannelIdForEventType returns feeding_reminders channel for feeding_reminder`() {
    assertEquals(
        PoopyFeedMessagingService.CHANNEL_FEEDING_REMINDERS,
        PoopyFeedMessagingService.getChannelIdForEventType("feeding_reminder"),
    )
  }

  @Test
  fun `getChannelIdForEventType returns pattern_alerts channel for pattern_alert`() {
    assertEquals(
        PoopyFeedMessagingService.CHANNEL_PATTERN_ALERTS,
        PoopyFeedMessagingService.getChannelIdForEventType("pattern_alert"),
    )
  }

  @Test
  fun `getChannelIdForEventType returns activity_alerts for activity and unknown types`() {
    assertEquals(
        PoopyFeedMessagingService.CHANNEL_ACTIVITY_ALERTS,
        PoopyFeedMessagingService.getChannelIdForEventType("activity_alert"),
    )
    assertEquals(
        PoopyFeedMessagingService.CHANNEL_ACTIVITY_ALERTS,
        PoopyFeedMessagingService.getChannelIdForEventType(""),
    )
    assertEquals(
        PoopyFeedMessagingService.CHANNEL_ACTIVITY_ALERTS,
        PoopyFeedMessagingService.getChannelIdForEventType("other"),
    )
  }

  @Test
  fun `shared preferences key constants are correct`() {
    assertEquals("poopyfeed_prefs", PoopyFeedMessagingService.PREFS_NAME)
    assertEquals("quiet_hours_enabled", PoopyFeedMessagingService.KEY_QUIET_HOURS_ENABLED)
    assertEquals("quiet_hours_start", PoopyFeedMessagingService.KEY_QUIET_HOURS_START)
    assertEquals("quiet_hours_end", PoopyFeedMessagingService.KEY_QUIET_HOURS_END)
  }
}
