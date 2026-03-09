package net.poopyfeed.pf.feedings

import net.poopyfeed.pf.TestFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedingAdapterTest {

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
