package net.poopyfeed.pf.diapers

import android.app.Activity
import android.widget.FrameLayout
import android.widget.TextView
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.Diaper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DiaperAdapterTest {

  private lateinit var adapter: DiaperAdapter
  private lateinit var activity: Activity
  private var clickedDiaper: Diaper? = null
  private var deleteClickedDiaper: Diaper? = null

  @Before
  fun setup() {
    clickedDiaper = null
    deleteClickedDiaper = null
    adapter =
        DiaperAdapter(
            onItemClick = { clickedDiaper = it },
            onDeleteClick = { deleteClickedDiaper = it },
        )
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    activity.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight)
  }

  private fun createAndBind(diaper: Diaper): DiaperAdapter.DiaperViewHolder {
    val parent =
        FrameLayout(activity).apply {
          layoutParams =
              FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.MATCH_PARENT,
                  FrameLayout.LayoutParams.WRAP_CONTENT,
              )
        }
    adapter.submitList(listOf(diaper))
    val holder = adapter.onCreateViewHolder(parent, 0)
    adapter.onBindViewHolder(holder, 0)
    return holder
  }

  @Test
  fun `bind shows change type and time`() {
    val diaper =
        TestFixtures.mockDiaper(id = 1, change_type = "both", timestamp = "2024-01-15T14:00:00Z")
    val holder = createAndBind(diaper)

    val typeView = holder.itemView.findViewById<TextView>(R.id.text_change_type)
    val timeView = holder.itemView.findViewById<TextView>(R.id.text_time)
    assertTrue(typeView.text.isNotEmpty())
    assertTrue(timeView.text.isNotEmpty())
  }

  @Test
  fun `click invokes onItemClick`() {
    val diaper = TestFixtures.mockDiaper(id = 42)
    val holder = createAndBind(diaper)
    holder.itemView.performClick()
    assertEquals(42, clickedDiaper?.id)
  }

  @Test
  fun `long click invokes onDeleteClick`() {
    val diaper = TestFixtures.mockDiaper(id = 10)
    val holder = createAndBind(diaper)
    holder.itemView.performLongClick()
    assertEquals(10, deleteClickedDiaper?.id)
  }

  @Test
  fun `DiffCallback areItemsTheSame by id`() {
    val diff = DiaperAdapter.DiaperDiffCallback()
    val d1 = TestFixtures.mockDiaper(id = 1)
    val d2 = TestFixtures.mockDiaper(id = 1)
    val d3 = TestFixtures.mockDiaper(id = 2)
    assertTrue(diff.areItemsTheSame(d1, d2))
    assertFalse(diff.areItemsTheSame(d1, d3))
  }

  @Test
  fun `DiffCallback areContentsTheSame`() {
    val diff = DiaperAdapter.DiaperDiffCallback()
    val d1 = TestFixtures.mockDiaper(id = 1, change_type = "wet")
    val d2 = TestFixtures.mockDiaper(id = 1, change_type = "wet")
    val d3 = TestFixtures.mockDiaper(id = 1, change_type = "dirty")
    assertTrue(diff.areContentsTheSame(d1, d2))
    assertFalse(diff.areContentsTheSame(d1, d3))
  }
}
