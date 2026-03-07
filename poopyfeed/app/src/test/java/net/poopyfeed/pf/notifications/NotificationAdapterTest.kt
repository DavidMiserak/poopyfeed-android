package net.poopyfeed.pf.notifications

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Unit tests for [NotificationAdapter] view types and binding. */
@RunWith(RobolectricTestRunner::class)
class NotificationAdapterTest {

  private lateinit var adapter: NotificationAdapter
  private lateinit var activity: Activity
  private var clickedNotification: Notification? = null
  private var loadMoreClicked = false

  @Before
  fun setup() {
    clickedNotification = null
    loadMoreClicked = false
    adapter =
        NotificationAdapter(
            onNotificationClick = { clickedNotification = it },
            onLoadMoreClick = { loadMoreClicked = true },
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
  fun `getItemViewType returns notification type for NotificationItem`() {
    val list =
        listOf(
            NotificationsListItem.NotificationItem(TestFixtures.mockNotification()),
        )
    adapter.submitList(list)
    assertEquals(0, adapter.getItemViewType(0))
  }

  @Test
  fun `getItemViewType returns load more type for LoadMoreFooter`() {
    val list =
        listOf(
            NotificationsListItem.LoadMoreFooter(isLoading = false),
        )
    adapter.submitList(list)
    assertEquals(1, adapter.getItemViewType(0))
  }

  @Test
  fun `submitList with notification and load more creates correct view types`() {
    val list =
        listOf(
            NotificationsListItem.NotificationItem(TestFixtures.mockNotification(id = 1)),
            NotificationsListItem.LoadMoreFooter(isLoading = true),
        )
    adapter.submitList(list)
    assertEquals(0, adapter.getItemViewType(0))
    assertEquals(1, adapter.getItemViewType(1))
  }

  @Test
  fun `bind notification item populates message and click fires callback`() {
    val notif = TestFixtures.mockNotification(id = 5, message = "Baby was fed")
    val list = listOf(NotificationsListItem.NotificationItem(notif))
    adapter.submitList(list)
    val parent = createParent()
    val holder = adapter.onCreateViewHolder(parent, 0)
    adapter.onBindViewHolder(holder, 0)
    val messageView = holder.itemView.findViewById<TextView>(net.poopyfeed.pf.R.id.text_message)
    assertEquals("Baby was fed", messageView.text.toString())
    holder.itemView.performClick()
    assertEquals(5, clickedNotification?.id)
    assertEquals("Baby was fed", clickedNotification?.message)
  }

  @Test
  fun `bind load more footer loading hides button and shows progress`() {
    val list = listOf(NotificationsListItem.LoadMoreFooter(isLoading = true))
    adapter.submitList(list)
    val parent = createParent()
    val holder = adapter.onCreateViewHolder(parent, 1)
    adapter.onBindViewHolder(holder, 0)
    val button = holder.itemView.findViewById<View>(net.poopyfeed.pf.R.id.button_load_more)
    val progress = holder.itemView.findViewById<View>(net.poopyfeed.pf.R.id.progress_load_more)
    assertEquals(View.GONE, button.visibility)
    assertEquals(View.VISIBLE, progress.visibility)
  }

  @Test
  fun `bind load more footer not loading shows button and click fires callback`() {
    val list = listOf(NotificationsListItem.LoadMoreFooter(isLoading = false))
    adapter.submitList(list)
    val parent = createParent()
    val holder = adapter.onCreateViewHolder(parent, 1)
    adapter.onBindViewHolder(holder, 0)
    val button = holder.itemView.findViewById<View>(net.poopyfeed.pf.R.id.button_load_more)
    assertEquals(View.VISIBLE, button.visibility)
    button.performClick()
    assertTrue(loadMoreClicked)
  }

  @Test
  fun `item count matches submitted list`() {
    val list =
        listOf(
            NotificationsListItem.NotificationItem(TestFixtures.mockNotification(id = 1)),
            NotificationsListItem.NotificationItem(TestFixtures.mockNotification(id = 2)),
        )
    adapter.submitList(list)
    assertEquals(2, adapter.itemCount)
  }
}
