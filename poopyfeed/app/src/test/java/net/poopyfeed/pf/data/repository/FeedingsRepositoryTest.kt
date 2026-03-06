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
import net.poopyfeed.pf.data.models.CreateFeedingRequest
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.models.PaginatedResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedingsRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var repository: FeedingsRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = mockk()
    repository = FeedingsRepository(apiService, ioDispatcher = testDispatcher)
  }

  @Test
  fun `listFeedings emits Loading then Success`() = runTest {
    val listItem = TestFixtures.mockFeedingListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))

    coEvery { apiService.listFeedings(childId = 1, page = 1) } returns response

    val results = repository.listFeedings(childId = 1, page = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<PaginatedResponse<Feeding>>>(results[1])
    val success = results[1] as ApiResult.Success
    assertEquals(1, success.data.count)
    assertEquals(listItem.id, success.data.results.first().id)
  }

  @Test
  fun `listFeedings emits Loading then Error on network failure`() = runTest {
    coEvery { apiService.listFeedings(any(), any()) } throws
        java.io.IOException("Network unavailable")

    val results = repository.listFeedings(childId = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Error<*>>(results[1])
    val error = (results[1] as ApiResult.Error).error
    assertIs<ApiError.NetworkError>(error)
  }

  @Test
  fun `createFeeding success returns Success`() = runTest {
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
            feeding_type = "bottle",
            amount_oz = 4.0,
            timestamp = "2024-01-15T10:00:00Z",
        )
    coEvery { apiService.createFeeding(childId = 1, request = request) } returns feeding

    val result = repository.createFeeding(childId = 1, request = request)

    assertIs<ApiResult.Success<Feeding>>(result)
    assertEquals(4.0, result.data.amount_oz)
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
    coEvery { apiService.createFeeding(any(), any()) } throws retrofit2.HttpException(errorResponse)

    val result = repository.createFeeding(childId = 1, request = request)

    assertIs<ApiResult.Error<Feeding>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `updateFeeding success returns Success`() = runTest {
    val request =
        CreateFeedingRequest(
            feeding_type = "breast",
            amount_oz = null,
            durationMinutes = 15,
            side = "left",
            timestamp = "2024-01-15T11:00:00Z",
        )
    val feeding =
        TestFixtures.mockFeeding(
            id = 2,
            feeding_type = "breast",
            amount_oz = null,
            timestamp = "2024-01-15T11:00:00Z",
        )
    coEvery { apiService.updateFeeding(1, 2, request) } returns feeding

    val result = repository.updateFeeding(childId = 1, feedingId = 2, request = request)

    assertIs<ApiResult.Success<Feeding>>(result)
    assertEquals("breast", result.data.feeding_type)
  }

  @Test
  fun `deleteFeeding success returns Success Unit`() = runTest {
    coEvery { apiService.deleteFeeding(childId = 1, feedingId = 2) } returns Unit

    val result = repository.deleteFeeding(childId = 1, feedingId = 2)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `deleteFeeding http error returns Error`() = runTest {
    val errorResponse = retrofit2.Response.error<Unit>(404, "Not Found".toResponseBody(null))
    coEvery { apiService.deleteFeeding(childId = 1, feedingId = 999) } throws
        retrofit2.HttpException(errorResponse)

    val result = repository.deleteFeeding(childId = 1, feedingId = 999)

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(404, result.error.statusCode)
  }
}
