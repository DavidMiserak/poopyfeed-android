package net.poopyfeed.pf.sync

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.IOException
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.FeedingDao
import net.poopyfeed.pf.data.db.NapDao
import net.poopyfeed.pf.data.db.PendingSyncDao
import net.poopyfeed.pf.data.db.PendingSyncEntity
import net.poopyfeed.pf.data.models.CreateDiaperRequest
import net.poopyfeed.pf.data.models.CreateFeedingRequest
import net.poopyfeed.pf.data.models.CreateNapRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SyncWorkerTest {

  private val pendingSyncDao: PendingSyncDao = mockk(relaxed = true)
  private val apiService: PoopyFeedApiService = mockk()
  private val feedingDao: FeedingDao = mockk(relaxed = true)
  private val diaperDao: DiaperDao = mockk(relaxed = true)
  private val napDao: NapDao = mockk(relaxed = true)
  private val json = Json { ignoreUnknownKeys = true }
  private val analyticsTracker: AnalyticsTracker = mockk(relaxed = true)

  private lateinit var context: Context

  @Before
  fun setup() {
    context = RuntimeEnvironment.getApplication()
    mockkStatic(Log::class)
    every { Log.w(any<String>(), any<String>(), any()) } returns 0
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun buildWorker(): SyncWorker {
    return TestListenableWorkerBuilder<SyncWorker>(context)
        .setWorkerFactory(
            TestSyncWorkerFactory(
                pendingSyncDao, apiService, feedingDao, diaperDao, napDao, json, analyticsTracker))
        .build()
  }

  @Test
  fun `doWork returns success when no pending items`() = runTest {
    coEvery { pendingSyncDao.getAllPending() } returns emptyList()

    val result = buildWorker().doWork()

    assertEquals(ListenableWorker.Result.success(), result)
  }

  @Test
  fun `doWork syncs feeding and deletes from pending queue`() = runTest {
    val request =
        CreateFeedingRequest(
            feeding_type = "bottle", amount_oz = 4.0, timestamp = "2024-01-15T10:00:00Z")
    val pendingItem =
        PendingSyncEntity(
            id = 1,
            entityType = "feeding",
            childId = 1,
            requestJson = json.encodeToString(CreateFeedingRequest.serializer(), request),
            tempLocalId = -1,
        )
    coEvery { pendingSyncDao.getAllPending() } returns listOf(pendingItem)
    coEvery { apiService.createFeeding(1, any()) } returns
        TestFixtures.mockFeedingListResponse(id = 42)

    val result = buildWorker().doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    coVerify { feedingDao.deleteFeeding(-1) }
    coVerify { feedingDao.upsertFeeding(match { it.id == 42 }) }
    coVerify { pendingSyncDao.delete(1) }
  }

  @Test
  fun `doWork syncs diaper and deletes from pending queue`() = runTest {
    val request = CreateDiaperRequest(change_type = "wet", timestamp = "2024-01-15T10:00:00Z")
    val pendingItem =
        PendingSyncEntity(
            id = 2,
            entityType = "diaper",
            childId = 1,
            requestJson = json.encodeToString(CreateDiaperRequest.serializer(), request),
            tempLocalId = -2,
        )
    coEvery { pendingSyncDao.getAllPending() } returns listOf(pendingItem)
    coEvery { apiService.createDiaper(1, any()) } returns
        TestFixtures.mockDiaperListResponse(id = 43)

    val result = buildWorker().doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    coVerify { diaperDao.deleteDiaper(-2) }
    coVerify { diaperDao.upsertDiaper(match { it.id == 43 }) }
    coVerify { pendingSyncDao.delete(2) }
  }

  @Test
  fun `doWork syncs nap and deletes from pending queue`() = runTest {
    val request = CreateNapRequest(start_time = "2024-01-15T10:00:00Z")
    val pendingItem =
        PendingSyncEntity(
            id = 3,
            entityType = "nap",
            childId = 1,
            requestJson = json.encodeToString(CreateNapRequest.serializer(), request),
            tempLocalId = -3,
        )
    coEvery { pendingSyncDao.getAllPending() } returns listOf(pendingItem)
    coEvery { apiService.createNap(1, any()) } returns TestFixtures.mockNapListResponse(id = 44)

    val result = buildWorker().doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    coVerify { napDao.deleteNap(-3) }
    coVerify { napDao.upsertNap(match { it.id == 44 }) }
    coVerify { pendingSyncDao.delete(3) }
  }

  @Test
  fun `doWork retries on IOException`() = runTest {
    val request =
        CreateFeedingRequest(
            feeding_type = "bottle", amount_oz = 4.0, timestamp = "2024-01-15T10:00:00Z")
    val pendingItem =
        PendingSyncEntity(
            id = 1,
            entityType = "feeding",
            childId = 1,
            requestJson = json.encodeToString(CreateFeedingRequest.serializer(), request),
            tempLocalId = -1,
        )
    coEvery { pendingSyncDao.getAllPending() } returns listOf(pendingItem)
    coEvery { apiService.createFeeding(any(), any()) } throws IOException("Network down")

    val result = buildWorker().doWork()

    assertEquals(ListenableWorker.Result.retry(), result)
  }

  @Test
  fun `doWork marks item as failed on non-IOException`() = runTest {
    val request =
        CreateFeedingRequest(
            feeding_type = "bottle", amount_oz = 4.0, timestamp = "2024-01-15T10:00:00Z")
    val pendingItem =
        PendingSyncEntity(
            id = 1,
            entityType = "feeding",
            childId = 1,
            requestJson = json.encodeToString(CreateFeedingRequest.serializer(), request),
            tempLocalId = -1,
        )
    coEvery { pendingSyncDao.getAllPending() } returns listOf(pendingItem)
    coEvery { apiService.createFeeding(any(), any()) } throws RuntimeException("Server error")

    val result = buildWorker().doWork()

    assertEquals(ListenableWorker.Result.success(), result)
    coVerify { pendingSyncDao.upsert(match { it.syncStatus == "failed" && it.retryCount == 1 }) }
  }
}

/** Simple WorkerFactory that creates SyncWorker with injected dependencies for testing. */
private class TestSyncWorkerFactory(
    private val pendingSyncDao: PendingSyncDao,
    private val apiService: PoopyFeedApiService,
    private val feedingDao: FeedingDao,
    private val diaperDao: DiaperDao,
    private val napDao: NapDao,
    private val json: Json,
    private val analyticsTracker: AnalyticsTracker,
) : androidx.work.WorkerFactory() {
  override fun createWorker(
      appContext: Context,
      workerClassName: String,
      workerParameters: androidx.work.WorkerParameters
  ): ListenableWorker {
    return SyncWorker(
        appContext,
        workerParameters,
        pendingSyncDao,
        apiService,
        feedingDao,
        diaperDao,
        napDao,
        json,
        analyticsTracker)
  }
}
