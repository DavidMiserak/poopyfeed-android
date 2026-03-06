package net.poopyfeed.pf.data.repository

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.NapDao
import net.poopyfeed.pf.data.db.NapEntity
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateNapRequest
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.models.UpdateNapRequest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedNapsRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var napDao: NapDao
  private lateinit var repository: CachedNapsRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = io.mockk.mockk()
    napDao = io.mockk.mockk()
    repository = CachedNapsRepository(apiService, napDao, ioDispatcher = testDispatcher)
  }

  @Test
  fun `listNapsCached emits Success empty list when cache empty`() = runTest {
    io.mockk.coEvery { napDao.getNapsFlow(1) } returns flowOf(emptyList())

    val results = repository.listNapsCached(childId = 1).toList()

    assertEquals(1, results.size)
    assertIs<ApiResult.Success<List<Nap>>>(results[0])
    assertEquals(0, (results[0] as ApiResult.Success).data.size)
  }

  @Test
  fun `listNapsCached emits Success when cache has data`() = runTest {
    val entities =
        listOf(
            NapEntity(
                id = 1,
                child = 1,
                start_time = "2024-01-15T10:00:00Z",
                end_time = "2024-01-15T11:00:00Z",
                created_at = "2024-01-15T10:00:00Z",
                updated_at = "2024-01-15T11:00:00Z",
            ))
    io.mockk.coEvery { napDao.getNapsFlow(1) } returns flowOf(entities)

    val results = repository.listNapsCached(childId = 1).toList()

    assertEquals(1, results.size)
    assertIs<ApiResult.Success<List<Nap>>>(results[0])
    assertEquals("2024-01-15T11:00:00Z", (results[0] as ApiResult.Success).data.first().end_time)
  }

  @Test
  fun `refreshNaps success upserts and returns Success`() = runTest {
    val listItem = TestFixtures.mockNapListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))
    io.mockk.coEvery { apiService.listNaps(childId = 1, page = 1) } returns response
    io.mockk.coEvery { napDao.upsertNaps(any()) } returns Unit

    val result = repository.refreshNaps(childId = 1)

    assertIs<ApiResult.Success<List<Nap>>>(result)
    assertEquals(1, result.data.size)
    assertEquals(listItem.id, result.data.first().id)
  }

  @Test
  fun `refreshNaps fetches all pages when next non-null`() = runTest {
    val list1 = TestFixtures.mockNapListResponse(id = 1)
    val list2 =
        TestFixtures.mockNapListResponse(
            id = 2,
            napped_at = "2024-01-15T11:00:00Z",
            ended_at = "2024-01-15T12:00:00Z",
        )

    io.mockk.coEvery { apiService.listNaps(childId = 1, page = 1) } returns
        PaginatedResponse(
            count = 2,
            next = "http://api/children/1/naps/?page=2",
            results = listOf(list1),
        )
    io.mockk.coEvery { apiService.listNaps(childId = 1, page = 2) } returns
        PaginatedResponse(count = 2, next = null, results = listOf(list2))
    io.mockk.coEvery { napDao.upsertNaps(any()) } returns Unit

    val result = repository.refreshNaps(childId = 1)

    assertIs<ApiResult.Success<List<Nap>>>(result)
    assertEquals(2, result.data.size)
    assertEquals(listOf(1, 2), result.data.map { it.id })
  }

  @Test
  fun `refreshNaps network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.listNaps(any(), any()) } throws IOException("Network down")

    val result = repository.refreshNaps(childId = 1)

    assertIs<ApiResult.Error<List<Nap>>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `refreshNaps rethrows CancellationException`() = runTest {
    io.mockk.coEvery { apiService.listNaps(childId = 1, page = 1) } throws
        CancellationException("cancel")

    kotlin.test.assertFailsWith<CancellationException> { repository.refreshNaps(childId = 1) }
  }

  @Test
  fun `createNap success upserts and returns Success`() = runTest {
    val request =
        CreateNapRequest(
            start_time = "2024-01-15T10:00:00Z",
            end_time = null,
        )
    val napResponse =
        TestFixtures.mockNapListResponse(
            id = 2,
            napped_at = "2024-01-15T10:00:00Z",
            ended_at = null,
        )
    io.mockk.coEvery { apiService.createNap(1, request) } returns napResponse
    io.mockk.coEvery { napDao.upsertNap(any()) } returns Unit

    val result = repository.createNap(childId = 1, request = request)

    assertIs<ApiResult.Success<Nap>>(result)
    assertEquals(2, result.data.id)
  }

  @Test
  fun `createNap http error returns Error`() = runTest {
    val request =
        CreateNapRequest(
            start_time = "2024-01-15T10:00:00Z",
            end_time = null,
        )
    val errorResponse = retrofit2.Response.error<Nap>(400, "Bad Request".toResponseBody(null))
    io.mockk.coEvery { apiService.createNap(any(), any()) } throws
        retrofit2.HttpException(errorResponse)

    val result = repository.createNap(childId = 1, request = request)

    assertIs<ApiResult.Error<Nap>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `updateNap success upserts and returns Success`() = runTest {
    val request = UpdateNapRequest(end_time = "2024-01-15T11:00:00Z")
    val napResponse =
        TestFixtures.mockNapListResponse(
            id = 2,
            napped_at = "2024-01-15T10:00:00Z",
            ended_at = "2024-01-15T11:00:00Z",
        )
    io.mockk.coEvery { apiService.updateNap(1, 2, request) } returns napResponse
    io.mockk.coEvery { napDao.upsertNap(any()) } returns Unit

    val result = repository.updateNap(childId = 1, napId = 2, request = request)

    assertIs<ApiResult.Success<Nap>>(result)
    assertEquals("2024-01-15T11:00:00Z", result.data.end_time)
  }

  @Test
  fun `deleteNap success clears from cache and returns Success`() = runTest {
    io.mockk.coEvery { apiService.deleteNap(1, 2) } returns Unit
    io.mockk.coEvery { napDao.deleteNap(2) } returns Unit

    val result = repository.deleteNap(childId = 1, napId = 2)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `deleteNap network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.deleteNap(any(), any()) } throws IOException("Network down")

    val result = repository.deleteNap(childId = 1, napId = 2)

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }
}
