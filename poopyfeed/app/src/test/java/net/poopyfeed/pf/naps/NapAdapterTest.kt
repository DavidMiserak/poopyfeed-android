package net.poopyfeed.pf.naps

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.Nap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NapAdapterTest {

  private lateinit var adapter: NapAdapter
  private lateinit var activity: Activity
  private var clickedNap: Nap? = null
  private var deleteClickedNap: Nap? = null
  private var endNapClicked: Nap? = null

  @Before
  fun setup() {
    clickedNap = null
    deleteClickedNap = null
    endNapClicked = null
    adapter =
        NapAdapter(
            onItemClick = { clickedNap = it },
            onDeleteClick = { deleteClickedNap = it },
            onEndNapClick = { endNapClicked = it },
        )
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    activity.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight)
  }

  private fun createAndBind(nap: Nap): NapAdapter.NapViewHolder {
    val parent =
        FrameLayout(activity).apply {
          layoutParams =
              FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.MATCH_PARENT,
                  FrameLayout.LayoutParams.WRAP_CONTENT,
              )
        }
    adapter.submitList(listOf(nap))
    val holder = adapter.onCreateViewHolder(parent, 0)
    adapter.onBindViewHolder(holder, 0)
    return holder
  }

  @Test
  fun `bind with end_time shows duration`() {
    val nap = TestFixtures.mockNap(id = 1, end_time = "2024-01-15T14:00:00Z")
    val holder = createAndBind(nap)

    val durationView = holder.itemView.findViewById<TextView>(R.id.text_duration)
    val inProgressView = holder.itemView.findViewById<View>(R.id.label_in_progress)
    val endButton = holder.itemView.findViewById<View>(R.id.button_end_nap)
    assertEquals(View.VISIBLE, durationView.visibility)
    assertEquals(View.GONE, inProgressView.visibility)
    assertEquals(View.GONE, endButton.visibility)
  }

  @Test
  fun `bind without end_time shows in progress and End Nap button`() {
    val nap = TestFixtures.mockNap(id = 1, end_time = null)
    val holder = createAndBind(nap)

    val durationView = holder.itemView.findViewById<TextView>(R.id.text_duration)
    val inProgressView = holder.itemView.findViewById<View>(R.id.label_in_progress)
    val endButton = holder.itemView.findViewById<View>(R.id.button_end_nap)
    assertEquals(View.GONE, durationView.visibility)
    assertEquals(View.VISIBLE, inProgressView.visibility)
    assertEquals(View.VISIBLE, endButton.visibility)
  }

  @Test
  fun `End Nap button click invokes onEndNapClick`() {
    val nap = TestFixtures.mockNap(id = 5, end_time = null)
    val holder = createAndBind(nap)
    holder.itemView.findViewById<View>(R.id.button_end_nap).performClick()
    assertEquals(5, endNapClicked?.id)
  }

  @Test
  fun `click invokes onItemClick`() {
    val nap = TestFixtures.mockNap(id = 42)
    val holder = createAndBind(nap)
    holder.itemView.performClick()
    assertEquals(42, clickedNap?.id)
  }

  @Test
  fun `long click invokes onDeleteClick`() {
    val nap = TestFixtures.mockNap(id = 10)
    val holder = createAndBind(nap)
    holder.itemView.performLongClick()
    assertEquals(10, deleteClickedNap?.id)
  }

  @Test
  fun `DiffCallback areItemsTheSame by id`() {
    val diff = NapAdapter.NapDiffCallback()
    val n1 = TestFixtures.mockNap(id = 1)
    val n2 = TestFixtures.mockNap(id = 1)
    val n3 = TestFixtures.mockNap(id = 2)
    assertTrue(diff.areItemsTheSame(n1, n2))
    assertFalse(diff.areItemsTheSame(n1, n3))
  }

  @Test
  fun `DiffCallback areContentsTheSame`() {
    val diff = NapAdapter.NapDiffCallback()
    val n1 = TestFixtures.mockNap(id = 1)
    val n2 = TestFixtures.mockNap(id = 1)
    val n3 = TestFixtures.mockNap(id = 1, end_time = null)
    assertTrue(diff.areContentsTheSame(n1, n2))
    assertFalse(diff.areContentsTheSame(n1, n3))
  }
}
