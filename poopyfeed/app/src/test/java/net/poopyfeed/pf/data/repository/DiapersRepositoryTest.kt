package net.poopyfeed.pf.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateDiaperRequest
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.models.PaginatedResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiapersRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var repository: DiapersRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = mockk()
    repository = DiapersRepository(apiService, ioDispatcher = testDispatcher)
  }

  @Test
  fun `listDiapers emits Loading then Success`() = runTest {
    val listItem = TestFixtures.mockDiaperListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))

    coEvery { apiService.listDiapers(childId = 1, page = 1) } returns response

    val results = repository.listDiapers(childId = 1, page = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<PaginatedResponse<Diaper>>>(results[1])
    val success = results[1] as ApiResult.Success
    assertEquals(1, success.data.count)
    assertEquals(listItem.id, success.data.results.first().id)
  }

  @Test
  fun `listDiapers emits Loading then Error on network failure`() = runTest {
    coEvery { apiService.listDiapers(any(), any()) } throws
        java.io.IOException("Network unavailable")

    val results = repository.listDiapers(childId = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Error<*>>(results[1])
    val error = (results[1] as ApiResult.Error).error
    assertIs<ApiError.NetworkError>(error)
  }

  @Test
  fun `createDiaper success returns Success`() = runTest {
    val request =
        CreateDiaperRequest(
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
        )
    val diaper =
        TestFixtures.mockDiaper(
            change_type = "wet",
            timestamp = "2024-01-15T10:00:00Z",
        )
    coEvery { apiService.createDiaper(childId = 1, request = request) } returns diaper

    val result = repository.createDiaper(childId = 1, request = request)

    assertIs<ApiResult.Success<Diaper>>(result)
    assertEquals("wet", result.data.change_type)
  }

  @Test
  fun `createDiaper http error returns Error`() = runTest {
    val request =
        CreateDiaperRequest(
            change_type = "dirty",
            timestamp = "2024-01-15T10:00:00Z",
        )
    val errorResponse = retrofit2.Response.error<Diaper>(400, "Bad Request".toResponseBody(null))
    coEvery { apiService.createDiaper(any(), any()) } throws retrofit2.HttpException(errorResponse)

    val result = repository.createDiaper(childId = 1, request = request)

    assertIs<ApiResult.Error<Diaper>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `deleteDiaper success returns Success Unit`() = runTest {
    coEvery { apiService.deleteDiaper(childId = 1, diaperId = 2) } returns Unit

    val result = repository.deleteDiaper(childId = 1, diaperId = 2)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `deleteDiaper http error returns Error`() = runTest {
    val errorResponse = retrofit2.Response.error<Unit>(404, "Not Found".toResponseBody(null))
    coEvery { apiService.deleteDiaper(childId = 1, diaperId = 999) } throws
        retrofit2.HttpException(errorResponse)

    val result = repository.deleteDiaper(childId = 1, diaperId = 999)

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(404, result.error.statusCode)
  }
}
