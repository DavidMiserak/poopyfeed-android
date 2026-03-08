package net.poopyfeed.pf.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException
import kotlinx.serialization.json.Json
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.DiaperEntity
import net.poopyfeed.pf.data.db.FeedingDao
import net.poopyfeed.pf.data.db.FeedingEntity
import net.poopyfeed.pf.data.db.NapDao
import net.poopyfeed.pf.data.db.NapEntity
import net.poopyfeed.pf.data.db.PendingSyncDao
import net.poopyfeed.pf.data.db.PendingSyncEntity
import net.poopyfeed.pf.data.models.CreateDiaperRequest
import net.poopyfeed.pf.data.models.CreateFeedingRequest
import net.poopyfeed.pf.data.models.CreateNapRequest

/**
 * Background worker that syncs pending offline-created entries with the server. Iterates all
 * pending items, calls the appropriate API, replaces the temp local entity with the server entity,
 * and removes from the pending queue.
 */
@HiltWorker
class SyncWorker
@AssistedInject
constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val pendingSyncDao: PendingSyncDao,
    private val apiService: PoopyFeedApiService,
    private val feedingDao: FeedingDao,
    private val diaperDao: DiaperDao,
    private val napDao: NapDao,
    private val json: Json,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val pendingItems = pendingSyncDao.getAllPending()
    if (pendingItems.isEmpty()) return Result.success()

    var hasNetworkFailure = false

    for (item in pendingItems) {
      try {
        syncItem(item)
        pendingSyncDao.delete(item.id)
      } catch (e: IOException) {
        // Network still down — retry the whole batch later
        hasNetworkFailure = true
        break
      } catch (e: Exception) {
        Log.w(TAG, "Sync failed for ${item.entityType} id=${item.id}", e)
        pendingSyncDao.upsert(
            item.copy(
                syncStatus = "failed",
                errorMessage = e.message,
                retryCount = item.retryCount + 1,
            ))
      }
    }

    return if (hasNetworkFailure) Result.retry() else Result.success()
  }

  private suspend fun syncItem(item: PendingSyncEntity) {
    when (item.entityType) {
      "feeding" -> syncFeeding(item)
      "diaper" -> syncDiaper(item)
      "nap" -> syncNap(item)
      else -> Log.w(TAG, "Unknown entity type: ${item.entityType}")
    }
  }

  private suspend fun syncFeeding(item: PendingSyncEntity) {
    val request = json.decodeFromString<CreateFeedingRequest>(item.requestJson)
    val response = apiService.createFeeding(item.childId, request)
    val feeding = response.toFeeding(item.childId)
    feedingDao.deleteFeeding(item.tempLocalId)
    feedingDao.upsertFeeding(FeedingEntity.fromApiModel(feeding))
  }

  private suspend fun syncDiaper(item: PendingSyncEntity) {
    val request = json.decodeFromString<CreateDiaperRequest>(item.requestJson)
    val response = apiService.createDiaper(item.childId, request)
    val diaper = response.toDiaper(item.childId)
    diaperDao.deleteDiaper(item.tempLocalId)
    diaperDao.upsertDiaper(DiaperEntity.fromApiModel(diaper))
  }

  private suspend fun syncNap(item: PendingSyncEntity) {
    val request = json.decodeFromString<CreateNapRequest>(item.requestJson)
    val response = apiService.createNap(item.childId, request)
    val nap = response.toNap(item.childId)
    napDao.deleteNap(item.tempLocalId)
    napDao.upsertNap(NapEntity.fromApiModel(nap))
  }

  companion object {
    private const val TAG = "SyncWorker"
  }
}
