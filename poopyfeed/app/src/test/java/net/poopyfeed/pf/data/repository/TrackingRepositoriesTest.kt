package net.poopyfeed.pf.data.repository

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

class TrackingRepositoriesTest {

  private lateinit var apiService: PoopyFeedApiService

  @Before
  fun setup() {
    apiService = io.mockk.mockk()
  }

  @Test
  fun `FeedingsRepository listFeedings emits Loading then Success`() = runTest {
    val repository = FeedingsRepository(apiService)
    val listItem = TestFixtures.mockFeedingListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))

    io.mockk.coEvery { apiService.listFeedings(childId = 1, page = 1) } returns response

    val results = repository.listFeedings(childId = 1, page = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<*>>(results[1])
  }

  @Test
  fun `FeedingsRepository createFeeding network error returns Error`() = runTest {
    val repository = FeedingsRepository(apiService)

    val request =
        CreateFeedingRequest(
            feeding_type = "bottle",
            amount_oz = 4.0,
            durationMinutes = null,
            side = null,
            timestamp = "2024-01-15T12:00:00Z",
        )

    io.mockk.coEvery { apiService.createFeeding(1, request) } throws IOException("Network down")

    val result = repository.createFeeding(1, request)

    assertIs<ApiResult.Error<Feeding>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `FeedingsRepository createFeeding success returns Success`() = runTest {
    val repository = FeedingsRepository(apiService)

    val request =
        CreateFeedingRequest(
            feeding_type = "bottle",
            amount_oz = 4.0,
            durationMinutes = null,
            side = null,
            timestamp = "2024-01-15T12:00:00Z",
        )
    val feedingResponse = TestFixtures.mockFeedingListResponse()

    io.mockk.coEvery { apiService.createFeeding(1, request) } returns feedingResponse

    val result = repository.createFeeding(1, request)

    assertIs<ApiResult.Success<Feeding>>(result)
  }

  @Test
  fun `FeedingsRepository updateFeeding success returns updated Feeding`() = runTest {
    val repository = FeedingsRepository(apiService)

    val request =
        CreateFeedingRequest(
            feeding_type = "bottle",
            amount_oz = 5.0,
            durationMinutes = null,
            side = null,
            timestamp = "2024-01-15T12:30:00Z",
        )
    val updatedResponse =
        TestFixtures.mockFeedingListResponse(
            amount_oz = "5.0", fed_at = "2024-01-15T12:30:00Z", updated_at = "2024-01-15T12:31:00Z")

    io.mockk.coEvery { apiService.updateFeeding(1, 10, request) } returns updatedResponse

    val result = repository.updateFeeding(1, 10, request)

    assertIs<ApiResult.Success<Feeding>>(result)
  }

  @Test
  fun `FeedingsRepository deleteFeeding success returns Success Unit`() = runTest {
    val repository = FeedingsRepository(apiService)

    io.mockk.coEvery { apiService.deleteFeeding(1, 10) } returns Unit

    val result = repository.deleteFeeding(1, 10)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `DiapersRepository listDiapers emits Loading then Error on http 500`() = runTest {
    val repository = DiapersRepository(apiService)

    val errorResponse =
        retrofit2.Response.error<PaginatedResponse<Diaper>>(
            500, "Server error".toResponseBody(null))

    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 1) } throws
        retrofit2.HttpException(errorResponse)

    val results = repository.listDiapers(childId = 1, page = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Error<*>>(results[1])
    val error = (results[1] as ApiResult.Error).error
    assertIs<ApiError.HttpError>(error)
    assertEquals(500, error.statusCode)
  }

  @Test
  fun `DiapersRepository listDiapers emits Loading then Success`() = runTest {
    val repository = DiapersRepository(apiService)
    val listItem = TestFixtures.mockDiaperListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))

    io.mockk.coEvery { apiService.listDiapers(childId = 1, page = 1) } returns response

    val results = repository.listDiapers(childId = 1, page = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<*>>(results[1])
  }

  @Test
  fun `DiapersRepository createDiaper success returns Success`() = runTest {
    val repository = DiapersRepository(apiService)

    val request = CreateDiaperRequest(change_type = "wet", timestamp = "2024-01-15T14:00:00Z")
    val diaperResponse =
        TestFixtures.mockDiaperListResponse(
            change_type = "wet", changed_at = "2024-01-15T14:00:00Z")

    io.mockk.coEvery { apiService.createDiaper(1, request) } returns diaperResponse

    val result = repository.createDiaper(1, request)

    assertIs<ApiResult.Success<Diaper>>(result)
  }

  @Test
  fun `DiapersRepository deleteDiaper success returns Success Unit`() = runTest {
    val repository = DiapersRepository(apiService)

    io.mockk.coEvery { apiService.deleteDiaper(1, 5) } returns Unit

    val result = repository.deleteDiaper(1, 5)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `NapsRepository createNap success returns Success`() = runTest {
    val repository = NapsRepository(apiService)
    val request = CreateNapRequest(start_time = "2024-01-15T13:00:00Z", end_time = null)
    val napResponse =
        TestFixtures.mockNapListResponse(ended_at = null, updated_at = "2024-01-15T13:00:00Z")
    io.mockk.coEvery { apiService.createNap(1, request) } returns napResponse

    val result = repository.createNap(1, request)

    assertIs<ApiResult.Success<Nap>>(result)
  }

  @Test
  fun `NapsRepository listNaps emits Loading then Success`() = runTest {
    val repository = NapsRepository(apiService)
    val listItem = TestFixtures.mockNapListResponse()
    val response = PaginatedResponse(count = 1, results = listOf(listItem))

    io.mockk.coEvery { apiService.listNaps(childId = 1, page = 1) } returns response

    val results = repository.listNaps(childId = 1, page = 1).toList()

    assertEquals(2, results.size)
    assertIs<ApiResult.Loading<*>>(results[0])
    assertIs<ApiResult.Success<*>>(results[1])
  }

  @Test
  fun `NapsRepository updateNap success returns updated Nap`() = runTest {
    val repository = NapsRepository(apiService)

    val request = UpdateNapRequest(end_time = "2024-01-15T14:00:00Z")
    val updatedResponse =
        TestFixtures.mockNapListResponse(
            ended_at = "2024-01-15T14:00:00Z", updated_at = "2024-01-15T14:01:00Z")

    io.mockk.coEvery { apiService.updateNap(1, 3, request) } returns updatedResponse

    val result = repository.updateNap(1, 3, request)

    assertIs<ApiResult.Success<Nap>>(result)
  }

  @Test
  fun `NapsRepository deleteNap success returns Success Unit`() = runTest {
    val repository = NapsRepository(apiService)

    io.mockk.coEvery { apiService.deleteNap(1, 3) } returns Unit

    val result = repository.deleteNap(1, 3)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `SharingRepository listShares success returns list`() = runTest {
    val repository = SharingRepository(apiService)

    val share =
        ChildShare(
            id = 1,
            userEmail = "friend@example.com",
            role = "co-parent",
            roleDisplay = "Co-parent",
            createdAt = "2024-01-15T10:00:00Z")

    io.mockk.coEvery { apiService.listShares(1) } returns listOf(share)

    val result = repository.listShares(1)

    assertIs<ApiResult.Success<List<ChildShare>>>(result)
    assertEquals(1, result.data.size)
    assertEquals("friend@example.com", result.data.first().userEmail)
  }

  @Test
  fun `SharingRepository createShare success returns ShareInviteResponse`() = runTest {
    val repository = SharingRepository(apiService)

    val request = CreateShareRequest(role = "co-parent")
    val response =
        ShareInviteResponse(
            id = 10,
            token = "abc123",
            role = "co-parent",
            isActive = true,
            createdAt = "2024-01-15T10:05:00Z",
            inviteUrl = "https://example.com/invite/abc123")

    io.mockk.coEvery { apiService.createShare(1, request) } returns response

    val result = repository.createShare(1, request)

    assertIs<ApiResult.Success<ShareInviteResponse>>(result)
    assertEquals("abc123", result.data.token)
  }

  @Test
  fun `SharingRepository getPendingInvites returns empty list`() = runTest {
    val repository = SharingRepository(apiService)

    val result = repository.getPendingInvites()

    assertIs<ApiResult.Success<List<ShareInvite>>>(result)
    assertEquals(0, result.data.size)
  }

  @Test
  fun `SharingRepository acceptInvite http error returns Error`() = runTest {
    val repository = SharingRepository(apiService)

    val errorResponse = retrofit2.Response.error<Child>(400, "Bad request".toResponseBody(null))

    io.mockk.coEvery { apiService.acceptInvite(any()) } throws
        retrofit2.HttpException(errorResponse)

    val result = repository.acceptInvite("badtoken")

    assertIs<ApiResult.Error<Child>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `SharingRepository acceptInvite success returns Child`() = runTest {
    val repository = SharingRepository(apiService)

    val child = TestFixtures.mockChild(id = 1, name = "Baby")

    io.mockk.coEvery { apiService.acceptInvite(any()) } returns child

    val result = repository.acceptInvite("valid-token")

    assertIs<ApiResult.Success<Child>>(result)
    assertEquals(1, result.data.id)
  }
}
