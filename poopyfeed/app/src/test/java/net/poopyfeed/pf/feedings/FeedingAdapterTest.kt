package net.poopyfeed.pf.feedings

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.Feeding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeedingAdapterTest {

  private lateinit var adapter: FeedingAdapter
  private lateinit var activity: Activity
  private var clickedFeeding: Feeding? = null
  private var deleteClickedFeeding: Feeding? = null

  @Before
  fun setup() {
    clickedFeeding = null
    deleteClickedFeeding = null
    adapter =
        FeedingAdapter(
            onItemClick = { clickedFeeding = it },
            onDeleteClick = { deleteClickedFeeding = it },
        )
    activity = Robolectric.buildActivity(Activity::class.java).create().get()
    activity.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight)
  }

  private fun createAndBind(feeding: Feeding): FeedingAdapter.FeedingViewHolder {
    val parent =
        FrameLayout(activity).apply {
          layoutParams =
              FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.MATCH_PARENT,
                  FrameLayout.LayoutParams.WRAP_CONTENT,
              )
        }
    adapter.submitList(listOf(feeding))
    val holder = adapter.onCreateViewHolder(parent, 0)
    adapter.onBindViewHolder(holder, 0)
    return holder
  }

  @Test
  fun `bind bottle shows type and amount`() {
    val feeding = TestFixtures.mockFeeding(id = 1, feeding_type = "bottle", amount_oz = 4.0)
    val holder = createAndBind(feeding)

    val typeView = holder.itemView.findViewById<TextView>(R.id.text_feeding_type)
    val amountView = holder.itemView.findViewById<TextView>(R.id.text_amount)
    assertTrue(typeView.text.isNotEmpty())
    assertEquals(View.VISIBLE, amountView.visibility)
    assertTrue(amountView.text.toString().contains("4"))
  }

  @Test
  fun `bind bottle null amount hides amount`() {
    val feeding = TestFixtures.mockFeeding(id = 1, feeding_type = "bottle", amount_oz = null)
    val holder = createAndBind(feeding)
    val amountView = holder.itemView.findViewById<TextView>(R.id.text_amount)
    assertEquals(View.GONE, amountView.visibility)
  }

  @Test
  fun `click invokes onItemClick`() {
    val feeding = TestFixtures.mockFeeding(id = 42)
    val holder = createAndBind(feeding)
    holder.itemView.performClick()
    assertEquals(42, clickedFeeding?.id)
  }

  @Test
  fun `long click invokes onDeleteClick`() {
    val feeding = TestFixtures.mockFeeding(id = 10)
    val holder = createAndBind(feeding)
    holder.itemView.performLongClick()
    assertEquals(10, deleteClickedFeeding?.id)
  }

  @Test
  fun `DiffCallback areItemsTheSame by id`() {
    val diff = FeedingAdapter.FeedingDiffCallback()
    val f1 = TestFixtures.mockFeeding(id = 1)
    val f2 = TestFixtures.mockFeeding(id = 1)
    val f3 = TestFixtures.mockFeeding(id = 2)
    assertTrue(diff.areItemsTheSame(f1, f2))
    assertFalse(diff.areItemsTheSame(f1, f3))
  }

  @Test
  fun `DiffCallback areContentsTheSame`() {
    val diff = FeedingAdapter.FeedingDiffCallback()
    val f1 = TestFixtures.mockFeeding(id = 1)
    val f2 = TestFixtures.mockFeeding(id = 1)
    val f3 = TestFixtures.mockFeeding(id = 1, amount_oz = 6.0)
    assertTrue(diff.areContentsTheSame(f1, f2))
    assertFalse(diff.areContentsTheSame(f1, f3))
  }
}
