package net.poopyfeed.pf.children

import android.app.Activity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.chip.Chip
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.Child
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/** Unit tests for [ChildAdapter] and its ViewHolder/DiffUtil logic. */
@RunWith(RobolectricTestRunner::class)
class ChildAdapterTest {

  private lateinit var adapter: ChildAdapter
  private lateinit var activity: Activity
  private var clickedChild: Child? = null

  @Before
  fun setup() {
    clickedChild = null
    adapter = ChildAdapter { child -> clickedChild = child }
    activity =
        Robolectric.buildActivity(
                Activity::class.java,
            )
            .create()
            .get()
    activity.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_DayNight)
  }

  private fun createAndBind(child: Child): ChildAdapter.ChildViewHolder {
    val parent =
        FrameLayout(activity).apply {
          layoutParams =
              FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        }
    adapter.submitList(listOf(child))
    val holder = adapter.onCreateViewHolder(parent, 0)
    adapter.onBindViewHolder(holder, 0)
    return holder
  }

  @Test
  fun `bind populates child name`() {
    val child = TestFixtures.mockChild(name = "Baby Bob")
    val holder = createAndBind(child)

    val nameView = holder.itemView.findViewById<TextView>(R.id.text_child_name)
    assertEquals("Baby Bob", nameView.text.toString())
  }

  @Test
  fun `bind shows age and gender`() {
    val child = TestFixtures.mockChild(date_of_birth = "2024-01-15", gender = "F")
    val holder = createAndBind(child)

    val ageGenderView = holder.itemView.findViewById<TextView>(R.id.text_age_gender)
    val text = ageGenderView.text.toString()
    assertTrue("Expected 'Girl' in text: $text", text.contains("Girl"))
  }

  @Test
  fun `bind shows male gender`() {
    val child = TestFixtures.mockChild(gender = "M")
    val holder = createAndBind(child)

    val ageGenderView = holder.itemView.findViewById<TextView>(R.id.text_age_gender)
    val text = ageGenderView.text.toString()
    assertTrue("Expected 'Boy' in text: $text", text.contains("Boy"))
  }

  @Test
  fun `click callback fires with correct child`() {
    val child = TestFixtures.mockChild(id = 42, name = "Tapped Child")
    val holder = createAndBind(child)

    holder.itemView.performClick()

    assertEquals(42, clickedChild?.id)
    assertEquals("Tapped Child", clickedChild?.name)
  }

  @Test
  fun `role chip hidden for owner`() {
    val child = TestFixtures.mockChild(user_role = "owner")
    val holder = createAndBind(child)

    val chip = holder.itemView.findViewById<Chip>(R.id.chip_role)
    assertEquals(View.GONE, chip.visibility)
  }

  @Test
  fun `role chip visible for co-parent`() {
    val child = TestFixtures.mockChild(user_role = "co-parent")
    val holder = createAndBind(child)

    val chip = holder.itemView.findViewById<Chip>(R.id.chip_role)
    assertEquals(View.VISIBLE, chip.visibility)
    assertEquals("Co-parent", chip.text.toString())
  }

  @Test
  fun `role chip visible for caregiver`() {
    val child = TestFixtures.mockChild(user_role = "caregiver")
    val holder = createAndBind(child)

    val chip = holder.itemView.findViewById<Chip>(R.id.chip_role)
    assertEquals(View.VISIBLE, chip.visibility)
    assertEquals("Caregiver", chip.text.toString())
  }

  @Test
  fun `activity time shows dash for null timestamps`() {
    val child =
        TestFixtures.mockChild(last_feeding = null, last_diaper_change = null, last_nap = null)
    val holder = createAndBind(child)

    val feedingText = holder.itemView.findViewById<TextView>(R.id.text_last_feeding).text.toString()
    val diaperText = holder.itemView.findViewById<TextView>(R.id.text_last_diaper).text.toString()
    val napText = holder.itemView.findViewById<TextView>(R.id.text_last_nap).text.toString()

    assertEquals("Expected dash for null feeding", "—", feedingText)
    assertEquals("Expected dash for null diaper", "—", diaperText)
    assertEquals("Expected dash for null nap", "—", napText)
  }

  @Test
  fun `activity time shows abbreviated time for non-null timestamps`() {
    val child =
        TestFixtures.mockChild(
            last_feeding = "2024-01-15T12:00:00Z",
            last_diaper_change = "2024-01-15T14:30:00Z",
            last_nap = "2024-01-15T13:00:00Z")
    val holder = createAndBind(child)

    val feedingText = holder.itemView.findViewById<TextView>(R.id.text_last_feeding).text.toString()
    val diaperText = holder.itemView.findViewById<TextView>(R.id.text_last_diaper).text.toString()
    val napText = holder.itemView.findViewById<TextView>(R.id.text_last_nap).text.toString()

    // Should show abbreviated time (e.g., "2d" for old timestamps from 2024)
    assertTrue("Expected time abbreviation for feeding: $feedingText", feedingText.matches(Regex("now|\\d+[mhd]")))
    assertTrue("Expected time abbreviation for diaper: $diaperText", diaperText.matches(Regex("now|\\d+[mhd]")))
    assertTrue("Expected time abbreviation for nap: $napText", napText.matches(Regex("now|\\d+[mhd]")))
  }

  @Test
  fun `DiffUtil areItemsTheSame compares by id`() {
    val diff = ChildAdapter.ChildDiffCallback()
    val child1 = TestFixtures.mockChild(id = 1, name = "Alice")
    val child2 = TestFixtures.mockChild(id = 1, name = "Bob")
    val child3 = TestFixtures.mockChild(id = 2, name = "Alice")

    assertTrue(diff.areItemsTheSame(child1, child2))
    assertFalse(diff.areItemsTheSame(child1, child3))
  }

  @Test
  fun `DiffUtil areContentsTheSame compares full equality`() {
    val diff = ChildAdapter.ChildDiffCallback()
    val child1 = TestFixtures.mockChild(id = 1, name = "Alice")
    val child2 = TestFixtures.mockChild(id = 1, name = "Alice")
    val child3 = TestFixtures.mockChild(id = 1, name = "Bob")

    assertTrue(diff.areContentsTheSame(child1, child2))
    assertFalse(diff.areContentsTheSame(child1, child3))
  }

  @Test
  fun `adapter item count matches submitted list`() {
    val children =
        listOf(
            TestFixtures.mockChild(id = 1, name = "Alice"),
            TestFixtures.mockChild(id = 2, name = "Bob"),
            TestFixtures.mockChild(id = 3, name = "Charlie"))
    adapter.submitList(children)

    assertEquals(3, adapter.itemCount)
  }

  @Test
  fun `submitList with null clears adapter`() {
    adapter.submitList(listOf(TestFixtures.mockChild()))
    assertEquals(1, adapter.itemCount)

    adapter.submitList(null)
    assertEquals(0, adapter.itemCount)
  }
}
