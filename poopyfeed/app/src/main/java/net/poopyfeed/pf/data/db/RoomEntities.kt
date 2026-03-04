package net.poopyfeed.pf.data.db

import androidx.room.*
import net.poopyfeed.pf.data.models.*

/**
 * Room database entities that mirror API models. Stored in local SQLite for offline access and
 * caching.
 */

/** Child entity for Room database. Maps 1:1 to API Child model. */
@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val date_of_birth: String,
    val gender: String,
    val user_role: String,
    val created_at: String,
    val updated_at: String,
    val last_feeding: String? = null,
    val last_diaper_change: String? = null,
    val last_nap: String? = null,

    // Local metadata
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis()
) {
  fun toApiModel(): Child =
      Child(
          id = id,
          name = name,
          date_of_birth = date_of_birth,
          gender = gender,
          user_role = user_role,
          created_at = created_at,
          updated_at = updated_at,
          last_feeding = last_feeding,
          last_diaper_change = last_diaper_change,
          last_nap = last_nap)

  companion object {
    fun fromApiModel(child: Child): ChildEntity =
        ChildEntity(
            id = child.id,
            name = child.name,
            date_of_birth = child.date_of_birth,
            gender = child.gender,
            user_role = child.user_role,
            created_at = child.created_at,
            updated_at = child.updated_at,
            last_feeding = child.last_feeding,
            last_diaper_change = child.last_diaper_change,
            last_nap = child.last_nap)
  }
}

/** Feeding entity for Room database. */
@Entity(
    tableName = "feedings",
    foreignKeys =
        [
            ForeignKey(
                entity = ChildEntity::class,
                parentColumns = ["id"],
                childColumns = ["child"],
                onDelete = ForeignKey.CASCADE)],
    indices = [Index("child")] // Index for faster queries
    )
data class FeedingEntity(
    @PrimaryKey val id: Int,
    val child: Int,
    val feeding_type: String,
    val amount_oz: Double? = null,
    val timestamp: String,
    val created_at: String,
    val updated_at: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis()
) {
  fun toApiModel(): Feeding =
      Feeding(
          id = id,
          child = child,
          feeding_type = feeding_type,
          amount_oz = amount_oz,
          timestamp = timestamp,
          created_at = created_at,
          updated_at = updated_at)

  companion object {
    fun fromApiModel(feeding: Feeding): FeedingEntity =
        FeedingEntity(
            id = feeding.id,
            child = feeding.child,
            feeding_type = feeding.feeding_type,
            amount_oz = feeding.amount_oz,
            timestamp = feeding.timestamp,
            created_at = feeding.created_at,
            updated_at = feeding.updated_at)
  }
}

/** Diaper entity for Room database. */
@Entity(
    tableName = "diapers",
    foreignKeys =
        [
            ForeignKey(
                entity = ChildEntity::class,
                parentColumns = ["id"],
                childColumns = ["child"],
                onDelete = ForeignKey.CASCADE)],
    indices = [Index("child")])
data class DiaperEntity(
    @PrimaryKey val id: Int,
    val child: Int,
    val change_type: String,
    val timestamp: String,
    val created_at: String,
    val updated_at: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis()
) {
  fun toApiModel(): Diaper =
      Diaper(
          id = id,
          child = child,
          change_type = change_type,
          timestamp = timestamp,
          created_at = created_at,
          updated_at = updated_at)

  companion object {
    fun fromApiModel(diaper: Diaper): DiaperEntity =
        DiaperEntity(
            id = diaper.id,
            child = diaper.child,
            change_type = diaper.change_type,
            timestamp = diaper.timestamp,
            created_at = diaper.created_at,
            updated_at = diaper.updated_at)
  }
}

/** Nap entity for Room database. */
@Entity(
    tableName = "naps",
    foreignKeys =
        [
            ForeignKey(
                entity = ChildEntity::class,
                parentColumns = ["id"],
                childColumns = ["child"],
                onDelete = ForeignKey.CASCADE)],
    indices = [Index("child")])
data class NapEntity(
    @PrimaryKey val id: Int,
    val child: Int,
    val start_time: String,
    val end_time: String? = null,
    val created_at: String,
    val updated_at: String,
    @ColumnInfo(name = "synced_at") val syncedAt: Long = System.currentTimeMillis()
) {
  fun toApiModel(): Nap =
      Nap(
          id = id,
          child = child,
          start_time = start_time,
          end_time = end_time,
          created_at = created_at,
          updated_at = updated_at)

  companion object {
    fun fromApiModel(nap: Nap): NapEntity =
        NapEntity(
            id = nap.id,
            child = nap.child,
            start_time = nap.start_time,
            end_time = nap.end_time,
            created_at = nap.created_at,
            updated_at = nap.updated_at)
  }
}
