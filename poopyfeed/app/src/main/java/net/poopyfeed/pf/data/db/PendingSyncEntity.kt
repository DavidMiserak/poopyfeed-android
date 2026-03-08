package net.poopyfeed.pf.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** Queued offline create operations waiting to sync with the server. */
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "feeding", "diaper", or "nap" */
    @ColumnInfo(name = "entity_type") val entityType: String,
    @ColumnInfo(name = "child_id") val childId: Int,
    /** Serialized CreateXxxRequest JSON */
    @ColumnInfo(name = "request_json") val requestJson: String,
    /** Negative Int inserted into the main table so UI shows the entry immediately */
    @ColumnInfo(name = "temp_local_id") val tempLocalId: Int,
    /** "pending" or "failed" */
    @ColumnInfo(name = "sync_status") val syncStatus: String = "pending",
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "retry_count") val retryCount: Int = 0,
)

@Dao
interface PendingSyncDao {

  @Query("SELECT * FROM pending_sync WHERE sync_status = 'pending' ORDER BY created_at ASC")
  suspend fun getAllPending(): List<PendingSyncEntity>

  @Query("SELECT * FROM pending_sync ORDER BY created_at ASC")
  fun getAllFlow(): Flow<List<PendingSyncEntity>>

  @Upsert suspend fun upsert(entity: PendingSyncEntity)

  @Query("DELETE FROM pending_sync WHERE id = :id") suspend fun delete(id: Long)

  @Query("DELETE FROM pending_sync") suspend fun clearAll()
}
