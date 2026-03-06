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
import net.poopyfeed.pf.data.models.CreateNapRequest
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.models.UpdateNapRequest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NapsRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var repository: NapsRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = mockk()
    repository = NapsRepository(apiService, ioDispatcher = testDispatcher)
  }

  @Test
  fun `listNaps emits Loading then Success`() = runTest {
    val listItem = TestFixtures.mockNapListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))

    coEvery { apiService.listNaps(childId = 1, page = 1) } returns response

    val results = repository.listNaps(childId = 1, page = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<PaginatedResponse<Nap>>>(results[1])
    val success = results[1] as ApiResult.Success
    assertEquals(1, success.data.count)
    assertEquals(listItem.id, success.data.results.first().id)
  }

  @Test
  fun `listNaps emits Loading then Error on network failure`() = runTest {
    coEvery { apiService.listNaps(any(), any()) } throws java.io.IOException("Network unavailable")

    val results = repository.listNaps(childId = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Error<*>>(results[1])
    val error = (results[1] as ApiResult.Error).error
    assertIs<ApiError.NetworkError>(error)
  }

  @Test
  fun `createNap success returns Success`() = runTest {
    val request =
        CreateNapRequest(
            start_time = "2024-01-15T10:00:00Z",
            end_time = null,
        )
    val napResponse =
        TestFixtures.mockNapListResponse(
            napped_at = "2024-01-15T10:00:00Z",
            ended_at = null,
        )
    coEvery { apiService.createNap(childId = 1, request = request) } returns napResponse

    val result = repository.createNap(childId = 1, request = request)

    assertIs<ApiResult.Success<Nap>>(result)
    assertEquals("2024-01-15T10:00:00Z", result.data.start_time)
  }

  @Test
  fun `createNap http error returns Error`() = runTest {
    val request =
        CreateNapRequest(
            start_time = "2024-01-15T10:00:00Z",
            end_time = null,
        )
    val errorResponse = retrofit2.Response.error<Nap>(400, "Bad Request".toResponseBody(null))
    coEvery { apiService.createNap(any(), any()) } throws retrofit2.HttpException(errorResponse)

    val result = repository.createNap(childId = 1, request = request)

    assertIs<ApiResult.Error<Nap>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `updateNap success returns Success`() = runTest {
    val request = UpdateNapRequest(end_time = "2024-01-15T11:00:00Z")
    val napResponse =
        TestFixtures.mockNapListResponse(
            id = 2,
            napped_at = "2024-01-15T10:00:00Z",
            ended_at = "2024-01-15T11:00:00Z",
        )
    coEvery { apiService.updateNap(childId = 1, napId = 2, request = request) } returns napResponse

    val result = repository.updateNap(childId = 1, napId = 2, request = request)

    assertIs<ApiResult.Success<Nap>>(result)
    assertEquals("2024-01-15T11:00:00Z", result.data.end_time)
  }

  @Test
  fun `deleteNap success returns Success Unit`() = runTest {
    coEvery { apiService.deleteNap(childId = 1, napId = 2) } returns Unit

    val result = repository.deleteNap(childId = 1, napId = 2)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `deleteNap http error returns Error`() = runTest {
    val errorResponse = retrofit2.Response.error<Unit>(404, "Not Found".toResponseBody(null))
    coEvery { apiService.deleteNap(childId = 1, napId = 999) } throws
        retrofit2.HttpException(errorResponse)

    val result = repository.deleteNap(childId = 1, napId = 999)

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(404, result.error.statusCode)
  }
}
