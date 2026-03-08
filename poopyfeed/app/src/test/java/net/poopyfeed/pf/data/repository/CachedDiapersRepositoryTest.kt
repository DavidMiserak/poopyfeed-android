package net.poopyfeed.pf.data.repository

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.DiaperEntity
import net.poopyfeed.pf.data.db.PendingSyncDao
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateDiaperRequest
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.sync.SyncScheduler
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedDiapersRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var diaperDao: DiaperDao
  private lateinit var pendingSyncDao: PendingSyncDao
  private lateinit var syncScheduler: SyncScheduler
  private lateinit var repository: CachedDiapersRepository
  private val testDispatcher = UnconfinedTestDispatcher()
  private val json = Json { ignoreUnknownKeys = true }

  @Before
  fun setup() {
    apiService = io.mockk.mockk()
    diaperDao = io.mockk.mockk()
    pendingSyncDao = io.mockk.mockk(relaxed = true)
    syncScheduler = io.mockk.mockk(relaxed = true)
    repository =
        CachedDiapersRepository(
            apiService,
            diaperDao,
            pendingSyncDao,
            syncScheduler,
            json,
            ioDispatcher = testDispatcher)
  }

  @Test
  fun `listDiapersCached emits Success empty list when cache empty`() = runTest {
    io.mockk.coEvery { diaperDao.getDiapersFlow(1) } returns flowOf(emptyList())

    val results = repository.listDiapersCached(childId = 1).toList()

    assertEquals(1, results.size)
    assertIs<ApiResult.Success<List<Diaper>>>(results[0])
    assertEquals(0, (results[0] as ApiResult.Success).data.size)
  }

  @Test
  fun `listDiapersCached emits Success when cache has data`() = runTest {
    val entities =
        listOf(
            DiaperEntity(
                id = 1,
                child = 1,
                change_type = "wet",
                timestamp = "2024-01-15T10:00:00Z",
                created_at = "2024-01-15T10:00:00Z",
                updated_at = "2024-01-15T10:00:00Z",
            ))
    io.mockk.coEvery { diaperDao.getDiapersFlow(1) } returns flowOf(entities)

    val results = repository.listDiapersCached(childId = 1).toList()

    assertEquals(1, results.size)
    assertIs<ApiResult.Success<List<Diaper>>>(results[0])
    assertEquals("wet", (results[0] as ApiResult.Success).data.first().change_type)
  }

  @Test
  fun `refreshDiapers success clears then upserts and returns Success`() = runTest {
    val listItem = TestFixtures.mockDiaperListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))
    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 1) } returns response
    io.mockk.coEvery { diaperDao.clearChildDiapers(1) } returns Unit
    io.mockk.coEvery { diaperDao.upsertDiapers(any()) } returns Unit

    val result = repository.refreshDiapers(childId = 1)

    assertIs<ApiResult.Success<List<Diaper>>>(result)
    assertEquals(1, result.data.size)
    assertEquals(listItem.id, result.data.first().id)
  }

  @Test
  fun `refreshDiapers fetches all pages when next non-null`() = runTest {
    val list1 = TestFixtures.mockDiaperListResponse(id = 1)
    val list2 = TestFixtures.mockDiaperListResponse(id = 2, changed_at = "2024-01-15T11:00:00Z")

    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 1) } returns
        PaginatedResponse(
            count = 2,
            next = "http://api/children/1/diapers/?page=2",
            results = listOf(list1),
        )
    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 2) } returns
        PaginatedResponse(count = 2, next = null, results = listOf(list2))
    io.mockk.coEvery { diaperDao.clearChildDiapers(1) } returns Unit
    io.mockk.coEvery { diaperDao.upsertDiapers(any()) } returns Unit

    val result = repository.refreshDiapers(childId = 1)

    assertIs<ApiResult.Success<List<Diaper>>>(result)
    assertEquals(2, result.data.size)
    assertEquals(listOf(1, 2), result.data.map { it.id })
  }

  @Test
  fun `refreshDiapers network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.listDiapers(any(), any()) } throws IOException("Network down")

    val result = repository.refreshDiapers(childId = 1)

    assertIs<ApiResult.Error<List<Diaper>>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `refreshDiapers rethrows CancellationException`() = runTest {
    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 1) } throws
        CancellationException("cancel")

    kotlin.test.assertFailsWith<CancellationException> { repository.refreshDiapers(childId = 1) }
  }

  @Test
  fun `createDiaper success upserts and returns Success`() = runTest {
    val request =
        CreateDiaperRequest(
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
        )
    val diaperResponse =
        TestFixtures.mockDiaperListResponse(
            id = 2,
            change_type = "wet",
            changed_at = "2024-01-15T10:00:00Z",
        )
    io.mockk.coEvery { apiService.createDiaper(1, request) } returns diaperResponse
    io.mockk.coEvery { diaperDao.upsertDiaper(any()) } returns Unit

    val result = repository.createDiaper(childId = 1, request = request)

    assertIs<ApiResult.Success<Diaper>>(result)
    assertEquals(2, result.data.id)
  }

  @Test
  fun `createDiaper network error queues offline and returns Success`() = runTest {
    val request =
        CreateDiaperRequest(
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
        )
    io.mockk.coEvery { apiService.createDiaper(any(), any()) } throws IOException("Network down")
    io.mockk.coEvery { diaperDao.upsertDiaper(any()) } returns Unit

    val result = repository.createDiaper(childId = 1, request = request)

    assertIs<ApiResult.Success<Diaper>>(result)
    kotlin.test.assertTrue(result.data.id < 0)
    assertEquals("wet", result.data.change_type)
    io.mockk.coVerify { diaperDao.upsertDiaper(match { it.id < 0 }) }
    io.mockk.coVerify { pendingSyncDao.upsert(match { it.entityType == "diaper" }) }
    io.mockk.verify { syncScheduler.enqueue() }
  }

  @Test
  fun `createDiaper http error returns Error`() = runTest {
    val request =
        CreateDiaperRequest(
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
        )
    val errorResponse = retrofit2.Response.error<Diaper>(400, "Bad Request".toResponseBody(null))
    io.mockk.coEvery { apiService.createDiaper(any(), any()) } throws
        retrofit2.HttpException(errorResponse)

    val result = repository.createDiaper(childId = 1, request = request)

    assertIs<ApiResult.Error<Diaper>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `deleteDiaper success clears from cache and returns Success`() = runTest {
    io.mockk.coEvery { apiService.deleteDiaper(1, 2) } returns Unit
    io.mockk.coEvery { diaperDao.deleteDiaper(2) } returns Unit

    val result = repository.deleteDiaper(childId = 1, diaperId = 2)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `deleteDiaper network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.deleteDiaper(any(), any()) } throws IOException("Network down")

    val result = repository.deleteDiaper(childId = 1, diaperId = 2)

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `getDiaper returns diaper when in cache and child matches`() = runTest {
    val entity =
        DiaperEntity(
            id = 2,
            child = 1,
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z",
        )
    io.mockk.coEvery { diaperDao.getDiaper(2) } returns entity

    val result = repository.getDiaper(childId = 1, diaperId = 2)

    assertEquals(2, result?.id)
    assertEquals(1, result?.child)
    assertEquals("wet", result?.change_type)
  }

  @Test
  fun `getDiaper returns null when child does not match`() = runTest {
    val entity =
        DiaperEntity(
            id = 2,
            child = 99,
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z",
        )
    io.mockk.coEvery { diaperDao.getDiaper(2) } returns entity

    val result = repository.getDiaper(childId = 1, diaperId = 2)

    assertEquals(null, result)
  }

  @Test
  fun `getDiaper returns null when not in cache`() = runTest {
    io.mockk.coEvery { diaperDao.getDiaper(2) } returns null

    val result = repository.getDiaper(childId = 1, diaperId = 2)

    assertEquals(null, result)
  }

  @Test
  fun `updateDiaper success upserts and returns Success`() = runTest {
    val request =
        CreateDiaperRequest(
            change_type = "both",
            timestamp = "2024-01-15T11:00:00Z",
        )
    val diaperResponse =
        TestFixtures.mockDiaperListResponse(
            id = 2,
            change_type = "both",
            changed_at = "2024-01-15T11:00:00Z",
        )
    io.mockk.coEvery { apiService.updateDiaper(1, 2, request) } returns diaperResponse
    io.mockk.coEvery { diaperDao.upsertDiaper(any()) } returns Unit

    val result = repository.updateDiaper(childId = 1, diaperId = 2, request = request)

    assertIs<ApiResult.Success<Diaper>>(result)
    assertEquals(2, result.data.id)
    assertEquals("both", result.data.change_type)
  }

  @Test
  fun `updateDiaper network error returns Error`() = runTest {
    val request =
        CreateDiaperRequest(
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
        )
    io.mockk.coEvery { apiService.updateDiaper(any(), any(), any()) } throws
        IOException("Network down")

    val result = repository.updateDiaper(childId = 1, diaperId = 2, request = request)

    assertIs<ApiResult.Error<Diaper>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `clearChildCache clears dao and removes child from synced set`() = runTest {
    io.mockk.coEvery { apiService.listDiapers(1, page = 1) } returns
        PaginatedResponse(
            count = 1, next = null, results = listOf(TestFixtures.mockDiaperListResponse()))
    io.mockk.coEvery { diaperDao.upsertDiapers(any()) } returns Unit
    io.mockk.coEvery { diaperDao.clearChildDiapers(1) } returns Unit

    repository.refreshDiapers(childId = 1)
    kotlin.test.assertTrue(repository.hasSyncedFlow(1).take(1).toList().single())

    repository.clearChildCache(1)
    kotlin.test.assertFalse(repository.hasSyncedFlow(1).take(1).toList().single())
    io.mockk.coVerify { diaperDao.clearChildDiapers(1) }
  }

  @Test
  fun `clearSessionCache resets sync state`() = runTest {
    io.mockk.coEvery { apiService.listDiapers(1, page = 1) } returns
        PaginatedResponse(
            count = 1, next = null, results = listOf(TestFixtures.mockDiaperListResponse()))
    io.mockk.coEvery { diaperDao.upsertDiapers(any()) } returns Unit

    repository.refreshDiapers(childId = 1)
    kotlin.test.assertTrue(repository.hasSyncedFlow(1).take(1).toList().single())

    repository.clearSessionCache()
    kotlin.test.assertFalse(repository.hasSyncedFlow(1).take(1).toList().single())
  }
}
