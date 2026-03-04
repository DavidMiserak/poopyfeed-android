package net.poopyfeed.pf.data.db

import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.models.Nap
import org.junit.Test
import kotlin.test.assertEquals

class RoomEntitiesTest {

    @Test
    fun `ChildEntity toApiModel and fromApiModel roundtrip`() {
        val child = Child(
            id = 1,
            name = "Baby Alice",
            date_of_birth = "2024-01-15",
            gender = "F",
            user_role = "owner",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z",
            last_feeding = "2024-01-15T12:00:00Z",
            last_diaper_change = "2024-01-15T14:30:00Z",
            last_nap = "2024-01-15T13:00:00Z"
        )
        val entity = ChildEntity.fromApiModel(child)
        assertEquals(1, entity.id)
        assertEquals("Baby Alice", entity.name)
        assertEquals(child, entity.toApiModel())
    }

    @Test
    fun `FeedingEntity toApiModel and fromApiModel roundtrip`() {
        val feeding = Feeding(
            id = 1,
            child = 1,
            feeding_type = "bottle",
            amount_oz = 4.0,
            timestamp = "2024-01-15T12:00:00Z",
            created_at = "2024-01-15T12:00:00Z",
            updated_at = "2024-01-15T12:00:00Z"
        )
        val entity = FeedingEntity.fromApiModel(feeding)
        assertEquals(1, entity.id)
        assertEquals("bottle", entity.feeding_type)
        assertEquals(4.0, entity.amount_oz)
        assertEquals(feeding, entity.toApiModel())
    }

    @Test
    fun `DiaperEntity toApiModel and fromApiModel roundtrip`() {
        val diaper = Diaper(
            id = 1,
            child = 1,
            change_type = "both",
            timestamp = "2024-01-15T14:00:00Z",
            created_at = "2024-01-15T14:00:00Z",
            updated_at = "2024-01-15T14:00:00Z"
        )
        val entity = DiaperEntity.fromApiModel(diaper)
        assertEquals(1, entity.id)
        assertEquals("both", entity.change_type)
        assertEquals(diaper, entity.toApiModel())
    }

    @Test
    fun `NapEntity toApiModel and fromApiModel roundtrip`() {
        val nap = Nap(
            id = 1,
            child = 1,
            start_time = "2024-01-15T13:00:00Z",
            end_time = "2024-01-15T14:00:00Z",
            created_at = "2024-01-15T13:00:00Z",
            updated_at = "2024-01-15T14:00:00Z"
        )
        val entity = NapEntity.fromApiModel(nap)
        assertEquals(1, entity.id)
        assertEquals("2024-01-15T13:00:00Z", entity.start_time)
        assertEquals("2024-01-15T14:00:00Z", entity.end_time)
        assertEquals(nap, entity.toApiModel())
    }

    @Test
    fun `NapEntity with null end_time`() {
        val nap = Nap(
            id = 1,
            child = 1,
            start_time = "2024-01-15T13:00:00Z",
            end_time = null,
            created_at = "2024-01-15T13:00:00Z",
            updated_at = "2024-01-15T13:00:00Z"
        )
        val entity = NapEntity.fromApiModel(nap)
        assertEquals(null, entity.end_time)
        assertEquals(nap, entity.toApiModel())
    }
}
