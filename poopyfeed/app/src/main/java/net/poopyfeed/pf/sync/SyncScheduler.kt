package net.poopyfeed.pf.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import net.poopyfeed.pf.data.db.PendingSyncDao

/** Schedules and cancels background sync work via WorkManager. */
@Singleton
class SyncScheduler
@Inject
constructor(
    private val workManager: WorkManager,
    private val pendingSyncDao: PendingSyncDao,
) {

  /** Enqueue a one-time sync that runs when network is available. */
  fun enqueue() {
    val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    val request =
        OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
    workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
  }

  /**
   * Enqueue sync work only if there are pending items. Call when app comes to foreground so locally
   * saved items are pushed once the device is online.
   */
  suspend fun enqueueIfPending() {
    if (pendingSyncDao.getAllPending().isNotEmpty()) {
      enqueue()
    }
  }

  /** Cancel any pending sync work (e.g. on logout). */
  fun cancel() {
    workManager.cancelUniqueWork(WORK_NAME)
  }

  companion object {
    const val WORK_NAME = "poopyfeed_background_sync"
  }
}
