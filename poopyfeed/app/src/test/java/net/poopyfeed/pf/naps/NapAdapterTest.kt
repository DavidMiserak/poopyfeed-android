package net.poopyfeed.pf.naps

import net.poopyfeed.pf.TestFixtures
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NapAdapterTest {

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
