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

/** Schedules and cancels background sync work via WorkManager. */
@Singleton
class SyncScheduler @Inject constructor(private val workManager: WorkManager) {

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

  /** Cancel any pending sync work (e.g. on logout). */
  fun cancel() {
    workManager.cancelUniqueWork(WORK_NAME)
  }

  companion object {
    const val WORK_NAME = "poopyfeed_background_sync"
  }
}
