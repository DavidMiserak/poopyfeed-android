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
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.DiaperEntity
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateDiaperRequest
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.models.PaginatedResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedDiapersRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var diaperDao: DiaperDao
  private lateinit var repository: CachedDiapersRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = io.mockk.mockk()
    diaperDao = io.mockk.mockk()
    repository = CachedDiapersRepository(apiService, diaperDao, ioDispatcher = testDispatcher)
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
  fun `refreshDiapers success upserts and returns Success`() = runTest {
    val diaper = TestFixtures.mockDiaper()
    val response = PaginatedResponse(count = 1, results = listOf(diaper))
    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 1) } returns response
    io.mockk.coEvery { diaperDao.upsertDiapers(any()) } returns Unit

    val result = repository.refreshDiapers(childId = 1)

    assertIs<ApiResult.Success<List<Diaper>>>(result)
    assertEquals(1, result.data.size)
    assertEquals(diaper.id, result.data.first().id)
  }

  @Test
  fun `refreshDiapers fetches all pages when next non-null`() = runTest {
    val diaper1 = TestFixtures.mockDiaper(id = 1)
    val diaper2 = TestFixtures.mockDiaper(id = 2, timestamp = "2024-01-15T11:00:00Z")

    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 1) } returns
        PaginatedResponse(
            count = 2,
            next = "http://api/children/1/diapers/?page=2",
            results = listOf(diaper1),
        )
    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 2) } returns
        PaginatedResponse(count = 2, next = null, results = listOf(diaper2))
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
    val diaper =
        TestFixtures.mockDiaper(
            id = 2,
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
        )
    io.mockk.coEvery { apiService.createDiaper(1, request) } returns diaper
    io.mockk.coEvery { diaperDao.upsertDiaper(any()) } returns Unit

    val result = repository.createDiaper(childId = 1, request = request)

    assertIs<ApiResult.Success<Diaper>>(result)
    assertEquals(2, result.data.id)
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
}
