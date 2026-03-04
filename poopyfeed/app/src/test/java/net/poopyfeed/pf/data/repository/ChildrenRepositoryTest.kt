package net.poopyfeed.pf.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

/** Unit tests for ChildrenRepository. */
class ChildrenRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var repository: ChildrenRepository

  @Before
  fun setup() {
    apiService = mockk()
    repository = ChildrenRepository(apiService)
  }

  @Test
  fun `listChildren emits Loading then Success`() = runTest {
    val mockChild =
        Child(
            id = 1,
            name = "Baby Alice",
            date_of_birth = "2024-01-15",
            gender = "F",
            user_role = "owner",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z",
            last_feeding = "2024-01-15T12:00:00Z",
            last_diaper_change = "2024-01-15T14:30:00Z",
            last_nap = "2024-01-15T13:00:00Z")

    val mockResponse = PaginatedResponse(count = 1, results = listOf(mockChild))

    coEvery { apiService.listChildren(page = 1) } returns mockResponse

    val results = repository.listChildren(page = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<*>>(results[1])

    val successData = (results[1] as ApiResult.Success).data
    assertEquals(1, successData.count)
    assertEquals("Baby Alice", successData.results.first().name)
  }

  @Test
  fun `listChildren emits Loading then Error on network failure`() = runTest {
    coEvery { apiService.listChildren(any()) } throws java.io.IOException("Network unavailable")

    val results = repository.listChildren().toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Error<*>>(results[1])

    val error = (results[1] as ApiResult.Error).error
    assertIs<ApiError.NetworkError>(error)
  }

  @Test
  fun `createChild returns Success`() = runTest {
    val request = CreateChildRequest(name = "Baby Bob", date_of_birth = "2024-06-20", gender = "M")

    val mockResponse =
        Child(
            id = 2,
            name = "Baby Bob",
            date_of_birth = "2024-06-20",
            gender = "M",
            user_role = "owner",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z")

    coEvery { apiService.createChild(request) } returns mockResponse

    val result = repository.createChild(request)

    assertIs<ApiResult.Success<Child>>(result)
    assertEquals("Baby Bob", result.data.name)
  }

  @Test
  fun `deleteChild returns Error on 404`() = runTest {
    val errorResponse = retrofit2.Response.error<Unit>(404, "Not Found".toResponseBody(null))

    coEvery { apiService.deleteChild(999) } throws retrofit2.HttpException(errorResponse)

    val result = repository.deleteChild(999)

    assertIs<ApiResult.Error<Unit>>(result)
    val error = result.error
    assertIs<ApiError.HttpError>(error)
    assertEquals(404, error.statusCode)
  }

  @Test
  fun `getChild emits Loading then Success`() = runTest {
    val mockChild =
        Child(
            id = 1,
            name = "Baby Alice",
            date_of_birth = "2024-01-15",
            gender = "F",
            user_role = "owner",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z",
            last_feeding = null,
            last_diaper_change = null,
            last_nap = null)
    coEvery { apiService.getChild(1) } returns mockChild

    val results = repository.getChild(1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<Child>>(results[1])
    assertEquals("Baby Alice", (results[1] as ApiResult.Success).data.name)
  }

  @Test
  fun `updateChild returns Success`() = runTest {
    val request = CreateChildRequest("Baby Alice", "2024-01-15", "F")
    val updated =
        Child(
            id = 1,
            name = "Baby Alice",
            date_of_birth = "2024-01-15",
            gender = "F",
            user_role = "owner",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T12:00:00Z")
    coEvery { apiService.updateChild(1, request) } returns updated

    val result = repository.updateChild(1, request)

    assertIs<ApiResult.Success<Child>>(result)
    assertEquals(updated.updated_at, result.data.updated_at)
  }
}
