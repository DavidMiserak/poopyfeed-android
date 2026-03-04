package net.poopyfeed.pf.data.db

import kotlin.test.assertEquals
import net.poopyfeed.pf.TestFixtures
import org.junit.Test

class RoomEntitiesTest {

  @Test
  fun `ChildEntity toApiModel and fromApiModel roundtrip`() {
    val child = TestFixtures.mockChild()
    val entity = ChildEntity.fromApiModel(child)
    assertEquals(1, entity.id)
    assertEquals("Baby Alice", entity.name)
    assertEquals(child, entity.toApiModel())
  }

  @Test
  fun `FeedingEntity toApiModel and fromApiModel roundtrip`() {
    val feeding = TestFixtures.mockFeeding()
    val entity = FeedingEntity.fromApiModel(feeding)
    assertEquals(1, entity.id)
    assertEquals("bottle", entity.feeding_type)
    assertEquals(4.0, entity.amount_oz)
    assertEquals(feeding, entity.toApiModel())
  }

  @Test
  fun `DiaperEntity toApiModel and fromApiModel roundtrip`() {
    val diaper = TestFixtures.mockDiaper()
    val entity = DiaperEntity.fromApiModel(diaper)
    assertEquals(1, entity.id)
    assertEquals("both", entity.change_type)
    assertEquals(diaper, entity.toApiModel())
  }

  @Test
  fun `NapEntity toApiModel and fromApiModel roundtrip`() {
    val nap = TestFixtures.mockNap()
    val entity = NapEntity.fromApiModel(nap)
    assertEquals(1, entity.id)
    assertEquals("2024-01-15T13:00:00Z", entity.start_time)
    assertEquals("2024-01-15T14:00:00Z", entity.end_time)
    assertEquals(nap, entity.toApiModel())
  }

  @Test
  fun `NapEntity with null end_time`() {
    val nap = TestFixtures.mockNap(end_time = null, updated_at = "2024-01-15T13:00:00Z")
    val entity = NapEntity.fromApiModel(nap)
    assertEquals(null, entity.end_time)
    assertEquals(nap, entity.toApiModel())
  }
}
