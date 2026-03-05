package net.poopyfeed.pf.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.ChildShare
import net.poopyfeed.pf.data.models.CreateShareRequest
import net.poopyfeed.pf.data.models.ShareInvite
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharingRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var repository: SharingRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = mockk()
    repository = SharingRepository(apiService, ioDispatcher = testDispatcher)
  }

  @Test
  fun `listShares success returns Success with shares`() = runTest {
    val share =
        ChildShare(
            id = 1,
            child = 1,
            shared_with_user = "co-parent@example.com",
            role = "co-parent",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z",
        )
    coEvery { apiService.listShares(childId = 1) } returns listOf(share)

    val result = repository.listShares(childId = 1)

    assertIs<ApiResult.Success<List<ChildShare>>>(result)
    assertEquals(1, result.data.size)
    assertEquals(share.id, result.data.first().id)
  }

  @Test
  fun `listShares network error returns Error`() = runTest {
    coEvery { apiService.listShares(any()) } throws java.io.IOException("Network down")

    val result = repository.listShares(childId = 1)

    assertIs<ApiResult.Error<List<ChildShare>>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `createShare success returns Success with invite`() = runTest {
    val request =
        CreateShareRequest(
            email = "caregiver@example.com",
            role = "caregiver",
        )
    val invite =
        ShareInvite(
            id = 10,
            child = 1,
            invited_email = "caregiver@example.com",
            role = "caregiver",
            status = "pending",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z",
        )
    coEvery { apiService.createShare(childId = 1, request = request) } returns invite

    val result = repository.createShare(childId = 1, request = request)

    assertIs<ApiResult.Success<ShareInvite>>(result)
    assertEquals("caregiver@example.com", result.data.invited_email)
  }

  @Test
  fun `createShare http error returns Error`() = runTest {
    val request =
        CreateShareRequest(
            email = "caregiver@example.com",
            role = "caregiver",
        )
    val errorResponse =
        retrofit2.Response.error<ShareInvite>(400, "Bad Request".toResponseBody(null))
    coEvery { apiService.createShare(any(), any()) } throws retrofit2.HttpException(errorResponse)

    val result = repository.createShare(childId = 1, request = request)

    assertIs<ApiResult.Error<ShareInvite>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `getPendingInvites success returns Success with invites`() = runTest {
    val invite =
        ShareInvite(
            id = 20,
            child = 1,
            invited_email = "user@example.com",
            role = "co-parent",
            status = "pending",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T10:00:00Z",
        )
    coEvery { apiService.getPendingInvites() } returns listOf(invite)

    val result = repository.getPendingInvites()

    assertIs<ApiResult.Success<List<ShareInvite>>>(result)
    assertEquals(1, result.data.size)
    assertEquals("user@example.com", result.data.first().invited_email)
  }

  @Test
  fun `getPendingInvites http error returns Error`() = runTest {
    val errorResponse =
        retrofit2.Response.error<List<ShareInvite>>(500, "Server Error".toResponseBody(null))
    coEvery { apiService.getPendingInvites() } throws retrofit2.HttpException(errorResponse)

    val result = repository.getPendingInvites()

    assertIs<ApiResult.Error<List<ShareInvite>>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(500, result.error.statusCode)
  }

  @Test
  fun `acceptInvite success returns Success with invite`() = runTest {
    val invite =
        ShareInvite(
            id = 42,
            child = 1,
            invited_email = "friend@example.com",
            role = "co-parent",
            status = "accepted",
            created_at = "2024-01-15T10:00:00Z",
            updated_at = "2024-01-15T11:00:00Z",
        )
    coEvery { apiService.acceptInvite(inviteId = 42) } returns invite

    val result = repository.acceptInvite(inviteId = 42)

    assertIs<ApiResult.Success<ShareInvite>>(result)
    assertEquals(42, result.data.id)
  }

  @Test
  fun `acceptInvite network error returns Error`() = runTest {
    coEvery { apiService.acceptInvite(any()) } throws java.io.IOException("Network down")

    val result = repository.acceptInvite(inviteId = 99)

    assertIs<ApiResult.Error<ShareInvite>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }
}
