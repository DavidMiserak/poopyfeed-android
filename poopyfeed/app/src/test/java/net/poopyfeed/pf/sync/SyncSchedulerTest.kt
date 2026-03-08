package net.poopyfeed.pf.sync

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.db.PendingSyncDao
import net.poopyfeed.pf.data.db.PendingSyncEntity
import org.junit.Test

class SyncSchedulerTest {

  private val workManager: WorkManager = mockk(relaxed = true)
  private val pendingSyncDao: PendingSyncDao = mockk(relaxed = true)
  private val scheduler = SyncScheduler(workManager, pendingSyncDao)

  @Test
  fun `enqueue schedules unique work with replace policy`() {
    scheduler.enqueue()

    verify {
      workManager.enqueueUniqueWork(
          SyncScheduler.WORK_NAME, ExistingWorkPolicy.REPLACE, any<OneTimeWorkRequest>())
    }
  }

  @Test
  fun `cancel cancels unique work by name`() {
    scheduler.cancel()

    verify { workManager.cancelUniqueWork(SyncScheduler.WORK_NAME) }
  }

  @Test
  fun `multiple enqueue calls replace previous work`() {
    scheduler.enqueue()
    scheduler.enqueue()

    verify(exactly = 2) {
      workManager.enqueueUniqueWork(
          SyncScheduler.WORK_NAME, ExistingWorkPolicy.REPLACE, any<OneTimeWorkRequest>())
    }
  }

  @Test
  fun `enqueueIfPending when empty does not enqueue`() = runTest {
    coEvery { pendingSyncDao.getAllPending() } returns emptyList()

    scheduler.enqueueIfPending()

    verify(exactly = 0) { workManager.enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) }
  }

  @Test
  fun `enqueueIfPending when pending items enqueues work`() = runTest {
    val pending =
        listOf(
            PendingSyncEntity(
                entityType = "feeding",
                childId = 1,
                requestJson = "{}",
                tempLocalId = -1,
            ))
    coEvery { pendingSyncDao.getAllPending() } returns pending

    scheduler.enqueueIfPending()

    verify {
      workManager.enqueueUniqueWork(
          SyncScheduler.WORK_NAME, ExistingWorkPolicy.REPLACE, any<OneTimeWorkRequest>())
    }
  }
}
