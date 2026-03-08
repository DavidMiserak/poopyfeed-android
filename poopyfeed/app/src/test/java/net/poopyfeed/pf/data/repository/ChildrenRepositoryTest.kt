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
import net.poopyfeed.pf.data.models.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

/** Unit tests for ChildrenRepository. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChildrenRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var repository: ChildrenRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = mockk()
    repository = ChildrenRepository(apiService, ioDispatcher = testDispatcher)
  }

  @Test
  fun `listChildren emits Loading then Success`() = runTest {
    val mockChild = TestFixtures.mockChild()
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
        TestFixtures.mockChild(
            id = 2,
            name = "Baby Bob",
            date_of_birth = "2024-06-20",
            gender = "M",
            last_feeding = null,
            last_diaper_change = null,
            last_nap = null)
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
  fun `deleteChild success returns Success Unit`() = runTest {
    coEvery { apiService.deleteChild(1) } returns Unit

    val result = repository.deleteChild(1)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `getChild emits Loading then Success`() = runTest {
    val mockChild =
        TestFixtures.mockChild(last_feeding = null, last_diaper_change = null, last_nap = null)
    coEvery { apiService.getChild(1) } returns mockChild

    val results = repository.getChild(1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<Child>>(results[1])
    assertEquals("Baby Alice", (results[1] as ApiResult.Success).data.name)
  }

  @Test
  fun `getChild emits Loading then Error on exception`() = runTest {
    coEvery { apiService.getChild(1) } throws java.io.IOException("Network down")

    val results = repository.getChild(1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Error<Child>>(results[1])
    assertIs<ApiError.NetworkError>((results[1] as ApiResult.Error).error)
  }

  @Test
  fun `updateChild returns Success`() = runTest {
    val request =
        net.poopyfeed.pf.data.models.UpdateChildRequest(
            name = "Baby Alice",
            date_of_birth = "2024-01-15",
            gender = "F",
        )
    val updated = TestFixtures.mockChild(updated_at = "2024-01-15T12:00:00Z")
    coEvery { apiService.updateChild(1, request) } returns updated

    val result = repository.updateChild(1, request)

    assertIs<ApiResult.Success<Child>>(result)
    assertEquals(updated.updated_at, result.data.updated_at)
  }
}
