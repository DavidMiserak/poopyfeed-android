package net.poopyfeed.pf.sync

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class SyncSchedulerTest {

  private val workManager: WorkManager = mockk(relaxed = true)
  private val scheduler = SyncScheduler(workManager)

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
}
