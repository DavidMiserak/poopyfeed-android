package net.poopyfeed.pf.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Objects (DAOs) for Room database. Each DAO provides suspend functions and Flow
 * queries for local data access.
 */

/** DAO for Child entities. Provides CRUD operations and queries for children. */
@Dao
interface ChildDao {

  /**
   * Get all children as a Flow (updates automatically when data changes). Useful for UI that should
   * update in real-time.
   */
  @Query("SELECT * FROM children ORDER BY name ASC")
  fun getAllChildrenFlow(): Flow<List<ChildEntity>>

  /** Get all children as a one-time suspend query. Useful when you only need data once. */
  @Query("SELECT * FROM children ORDER BY name ASC") suspend fun getAllChildren(): List<ChildEntity>

  /** Get a specific child by ID as Flow. */
  @Query("SELECT * FROM children WHERE id = :childId")
  fun getChildFlow(childId: Int): Flow<ChildEntity?>

  /** Get a specific child by ID (one-time). */
  @Query("SELECT * FROM children WHERE id = :childId")
  suspend fun getChild(childId: Int): ChildEntity?

  /** Insert or update a child (replaces if exists). */
  @Upsert suspend fun upsertChild(child: ChildEntity)

  /** Batch upsert multiple children. */
  @Upsert suspend fun upsertChildren(children: List<ChildEntity>)

  /** Delete a child by ID. */
  @Query("DELETE FROM children WHERE id = :childId") suspend fun deleteChild(childId: Int)

  /** Clear all children (useful for logout). */
  @Query("DELETE FROM children") suspend fun clearAll()
}

/** DAO for Feeding entities. */
@Dao
interface FeedingDao {

  /** Get all feedings for a child as Flow. */
  @Query("SELECT * FROM feedings WHERE child = :childId ORDER BY timestamp DESC")
  fun getFeedingsFlow(childId: Int): Flow<List<FeedingEntity>>

  /** Get all feedings for a child (one-time). */
  @Query("SELECT * FROM feedings WHERE child = :childId ORDER BY timestamp DESC")
  suspend fun getFeedings(childId: Int): List<FeedingEntity>

  /** Get a specific feeding by ID. */
  @Query("SELECT * FROM feedings WHERE id = :feedingId")
  suspend fun getFeeding(feedingId: Int): FeedingEntity?

  /** Get the most recent feeding for a child (useful for display). */
  @Query("SELECT * FROM feedings WHERE child = :childId ORDER BY timestamp DESC LIMIT 1")
  suspend fun getLatestFeeding(childId: Int): FeedingEntity?

  /** Insert or update a feeding. */
  @Upsert suspend fun upsertFeeding(feeding: FeedingEntity)

  /** Batch upsert feedings. */
  @Upsert suspend fun upsertFeedings(feedings: List<FeedingEntity>)

  /** Delete a feeding by ID. */
  @Query("DELETE FROM feedings WHERE id = :feedingId") suspend fun deleteFeeding(feedingId: Int)

  /** Clear all feedings for a child. */
  @Query("DELETE FROM feedings WHERE child = :childId") suspend fun clearChildFeedings(childId: Int)
}

/** DAO for Diaper entities. */
@Dao
interface DiaperDao {

  /** Get all diapers for a child as Flow. */
  @Query("SELECT * FROM diapers WHERE child = :childId ORDER BY timestamp DESC")
  fun getDiapersFlow(childId: Int): Flow<List<DiaperEntity>>

  /** Get all diapers for a child (one-time). */
  @Query("SELECT * FROM diapers WHERE child = :childId ORDER BY timestamp DESC")
  suspend fun getDiapers(childId: Int): List<DiaperEntity>

  /** Get a specific diaper by ID. */
  @Query("SELECT * FROM diapers WHERE id = :diaperId")
  suspend fun getDiaper(diaperId: Int): DiaperEntity?

  /** Get the most recent diaper change. */
  @Query("SELECT * FROM diapers WHERE child = :childId ORDER BY timestamp DESC LIMIT 1")
  suspend fun getLatestDiaper(childId: Int): DiaperEntity?

  /** Insert or update a diaper. */
  @Upsert suspend fun upsertDiaper(diaper: DiaperEntity)

  /** Batch upsert diapers. */
  @Upsert suspend fun upsertDiapers(diapers: List<DiaperEntity>)

  /** Delete a diaper by ID. */
  @Query("DELETE FROM diapers WHERE id = :diaperId") suspend fun deleteDiaper(diaperId: Int)

  /** Clear all diapers for a child. */
  @Query("DELETE FROM diapers WHERE child = :childId") suspend fun clearChildDiapers(childId: Int)
}

/** DAO for Nap entities. */
@Dao
interface NapDao {

  /** Get all naps for a child as Flow. */
  @Query("SELECT * FROM naps WHERE child = :childId ORDER BY start_time DESC")
  fun getNapsFlow(childId: Int): Flow<List<NapEntity>>

  /** Get all naps for a child (one-time). */
  @Query("SELECT * FROM naps WHERE child = :childId ORDER BY start_time DESC")
  suspend fun getNaps(childId: Int): List<NapEntity>

  /** Get a specific nap by ID. */
  @Query("SELECT * FROM naps WHERE id = :napId")
  suspend fun getNap(napId: Int): NapEntity?

  /** Get the most recent nap. */
  @Query("SELECT * FROM naps WHERE child = :childId ORDER BY start_time DESC LIMIT 1")
  suspend fun getLatestNap(childId: Int): NapEntity?

  /** Get ongoing naps (end_time is NULL). */
  @Query("SELECT * FROM naps WHERE child = :childId AND end_time IS NULL")
  suspend fun getOngoingNaps(childId: Int): List<NapEntity>

  /** Insert or update a nap. */
  @Upsert suspend fun upsertNap(nap: NapEntity)

  /** Batch upsert naps. */
  @Upsert suspend fun upsertNaps(naps: List<NapEntity>)

  /** Delete a nap by ID. */
  @Query("DELETE FROM naps WHERE id = :napId") suspend fun deleteNap(napId: Int)

  /** Clear all naps for a child. */
  @Query("DELETE FROM naps WHERE child = :childId") suspend fun clearChildNaps(childId: Int)
}
