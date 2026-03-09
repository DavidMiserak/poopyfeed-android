package net.poopyfeed.pf.diapers

import net.poopyfeed.pf.TestFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaperAdapterTest {

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
