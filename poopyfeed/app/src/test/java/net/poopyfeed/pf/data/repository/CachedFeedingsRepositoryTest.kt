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
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.FeedingDao
import net.poopyfeed.pf.data.db.FeedingEntity
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateFeedingRequest
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.models.PaginatedResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedFeedingsRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var feedingDao: FeedingDao
  private lateinit var repository: CachedFeedingsRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = io.mockk.mockk()
    feedingDao = io.mockk.mockk()
    repository = CachedFeedingsRepository(apiService, feedingDao, ioDispatcher = testDispatcher)
  }

  @Test
  fun `listFeedingsCached emits Success empty list when cache empty`() = runTest {
    io.mockk.coEvery { feedingDao.getFeedingsFlow(1) } returns flowOf(emptyList())

    val results = repository.listFeedingsCached(childId = 1).toList()

    assertEquals(1, results.size)
    assertIs<ApiResult.Success<List<Feeding>>>(results[0])
    assertEquals(0, (results[0] as ApiResult.Success).data.size)
  }

  @Test
  fun `listFeedingsCached emits Success when cache has data`() = runTest {
    val entities =
        listOf(
            FeedingEntity(
                id = 1,
                child = 1,
                feeding_type = "bottle",
                amount_oz = 4.0,
                timestamp = "2024-01-15T10:00:00Z",
                created_at = "2024-01-15T10:00:00Z",
                updated_at = "2024-01-15T10:00:00Z",
            ))
    io.mockk.coEvery { feedingDao.getFeedingsFlow(1) } returns flowOf(entities)

    val results = repository.listFeedingsCached(childId = 1).toList()

    assertEquals(1, results.size)
    assertIs<ApiResult.Success<List<Feeding>>>(results[0])
    assertEquals("bottle", (results[0] as ApiResult.Success).data.first().feeding_type)
  }

  @Test
  fun `refreshFeedings success upserts and returns Success`() = runTest {
    val listItem = TestFixtures.mockFeedingListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))
    io.mockk.coEvery { apiService.listFeedings(childId = 1, page = 1) } returns response
    io.mockk.coEvery { feedingDao.upsertFeedings(any()) } returns Unit

    val result = repository.refreshFeedings(childId = 1)

    assertIs<ApiResult.Success<List<Feeding>>>(result)
    assertEquals(1, result.data.size)
    assertEquals(listItem.id, result.data.first().id)
  }

  @Test
  fun `refreshFeedings fetches all pages when next non-null`() = runTest {
    val list1 = TestFixtures.mockFeedingListResponse(id = 1)
    val list2 = TestFixtures.mockFeedingListResponse(id = 2, fed_at = "2024-01-15T11:00:00Z")

    io.mockk.coEvery { apiService.listFeedings(childId = 1, page = 1) } returns
        PaginatedResponse(
            count = 2,
            next = "http://api/children/1/feedings/?page=2",
            results = listOf(list1),
        )
    io.mockk.coEvery { apiService.listFeedings(childId = 1, page = 2) } returns
        PaginatedResponse(count = 2, next = null, results = listOf(list2))
    io.mockk.coEvery { feedingDao.upsertFeedings(any()) } returns Unit

    val result = repository.refreshFeedings(childId = 1)

    assertIs<ApiResult.Success<List<Feeding>>>(result)
    assertEquals(2, result.data.size)
    assertEquals(listOf(1, 2), result.data.map { it.id })
  }

  @Test
  fun `refreshFeedings network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.listFeedings(any(), any()) } throws IOException("Network down")

    val result = repository.refreshFeedings(childId = 1)

    assertIs<ApiResult.Error<List<Feeding>>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `refreshFeedings rethrows CancellationException`() = runTest {
    io.mockk.coEvery { apiService.listFeedings(childId = 1, page = 1) } throws
        CancellationException("cancel")

    kotlin.test.assertFailsWith<CancellationException> { repository.refreshFeedings(childId = 1) }
  }

  @Test
  fun `createFeeding success upserts and returns Success`() = runTest {
    val request =
        CreateFeedingRequest(
            feeding_type = "bottle",
            amount_oz = 4.0,
            durationMinutes = null,
            side = null,
            timestamp = "2024-01-15T10:00:00Z",
        )
    val feeding =
        TestFixtures.mockFeeding(
            id = 2,
            feeding_type = "bottle",
            amount_oz = 4.0,
            timestamp = "2024-01-15T10:00:00Z",
        )
    io.mockk.coEvery { apiService.createFeeding(1, request) } returns feeding
    io.mockk.coEvery { feedingDao.upsertFeeding(any()) } returns Unit

    val result = repository.createFeeding(childId = 1, request = request)

    assertIs<ApiResult.Success<Feeding>>(result)
    assertEquals(2, result.data.id)
  }

  @Test
  fun `createFeeding http error returns Error`() = runTest {
    val request =
        CreateFeedingRequest(
            feeding_type = "bottle",
            amount_oz = 4.0,
            durationMinutes = null,
            side = null,
            timestamp = "2024-01-15T10:00:00Z",
        )
    val errorResponse = retrofit2.Response.error<Feeding>(400, "Bad Request".toResponseBody(null))
    io.mockk.coEvery { apiService.createFeeding(any(), any()) } throws
        retrofit2.HttpException(errorResponse)

    val result = repository.createFeeding(childId = 1, request = request)

    assertIs<ApiResult.Error<Feeding>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `deleteFeeding success clears from cache and returns Success`() = runTest {
    io.mockk.coEvery { apiService.deleteFeeding(1, 2) } returns Unit
    io.mockk.coEvery { feedingDao.deleteFeeding(2) } returns Unit

    val result = repository.deleteFeeding(childId = 1, feedingId = 2)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `deleteFeeding network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.deleteFeeding(any(), any()) } throws IOException("Network down")

    val result = repository.deleteFeeding(childId = 1, feedingId = 2)

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `clearSessionCache resets sync state so hasSyncedFlow emits false`() = runTest {
    val listItem = TestFixtures.mockFeedingListResponse()
    io.mockk.coEvery { apiService.listFeedings(childId = 1, page = 1) } returns
        PaginatedResponse(count = 1, results = listOf(listItem))
    io.mockk.coEvery { feedingDao.upsertFeedings(any()) } returns Unit

    repository.refreshFeedings(childId = 1)
    val syncedAfterRefresh = repository.hasSyncedFlow(1).take(1).toList().single()
    kotlin.test.assertTrue(syncedAfterRefresh)

    repository.clearSessionCache()
    val syncedAfterClear = repository.hasSyncedFlow(1).take(1).toList().single()
    kotlin.test.assertFalse(syncedAfterClear)
  }
}
