package net.poopyfeed.pf.notifications

import android.app.Activity
import android.widget.FrameLayout
import android.widget.TextView
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.Notification
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Unit tests for [NotificationAdapter] binding and click handling. */
@RunWith(RobolectricTestRunner::class)
class NotificationAdapterTest {

  private lateinit var adapter: NotificationAdapter
  private lateinit var activity: Activity
  private var clickedNotification: Notification? = null

  @Before
  fun setup() {
    clickedNotification = null
    adapter =
        NotificationAdapter(
            onNotificationClick = { clickedNotification = it },
        )
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    activity.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight)
  }

  private fun createParent(): FrameLayout =
      FrameLayout(activity).apply {
        layoutParams =
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
      }

  @Test
  fun `bind notification item populates message and click fires callback`() {
    val notif = TestFixtures.mockNotification(id = 5, message = "Baby was fed")
    val parent = createParent()
    val holder = adapter.onCreateViewHolder(parent, 0)
    holder.bind(notif)
    val messageView = holder.itemView.findViewById<TextView>(net.poopyfeed.pf.R.id.text_message)
    assertEquals("Baby was fed", messageView.text.toString())
    holder.itemView.performClick()
    assertEquals(5, clickedNotification?.id)
    assertEquals("Baby was fed", clickedNotification?.message)
  }

  @Test
  fun `bind notification with unread shows unread indicator`() {
    val notif = TestFixtures.mockNotification(id = 1, isRead = false)
    val parent = createParent()
    val holder = adapter.onCreateViewHolder(parent, 0)
    holder.bind(notif)
    val dotUnread = holder.itemView.findViewById<android.view.View>(net.poopyfeed.pf.R.id.dot_unread)
    assertEquals(android.view.View.VISIBLE, dotUnread.visibility)
  }

  @Test
  fun `bind notification with read hides unread indicator`() {
    val notif = TestFixtures.mockNotification(id = 1, isRead = true)
    val parent = createParent()
    val holder = adapter.onCreateViewHolder(parent, 0)
    holder.bind(notif)
    val dotUnread = holder.itemView.findViewById<android.view.View>(net.poopyfeed.pf.R.id.dot_unread)
    assertEquals(android.view.View.GONE, dotUnread.visibility)
  }

  @Test
  fun `notification diff callback identifies same items by id`() {
    val diff = NotificationAdapter.NotificationDiffCallback()
    val notif1 = TestFixtures.mockNotification(id = 1, message = "Old message")
    val notif2 = TestFixtures.mockNotification(id = 1, message = "New message")
    assertEquals(true, diff.areItemsTheSame(notif1, notif2))
  }

  @Test
  fun `notification diff callback identifies different items by id`() {
    val diff = NotificationAdapter.NotificationDiffCallback()
    val notif1 = TestFixtures.mockNotification(id = 1)
    val notif2 = TestFixtures.mockNotification(id = 2)
    assertEquals(false, diff.areItemsTheSame(notif1, notif2))
  }

  @Test
  fun `notification diff callback detects content changes`() {
    val diff = NotificationAdapter.NotificationDiffCallback()
    val notif1 = TestFixtures.mockNotification(id = 1, message = "Old", isRead = false)
    val notif2 = TestFixtures.mockNotification(id = 1, message = "New", isRead = true)
    assertEquals(false, diff.areContentsTheSame(notif1, notif2))
  }

  @Test
  fun `notification diff callback ignores unchanged content`() {
    val diff = NotificationAdapter.NotificationDiffCallback()
    val notif1 = TestFixtures.mockNotification(id = 1, message = "Same")
    val notif2 = TestFixtures.mockNotification(id = 1, message = "Same")
    assertEquals(true, diff.areContentsTheSame(notif1, notif2))
  }
}
