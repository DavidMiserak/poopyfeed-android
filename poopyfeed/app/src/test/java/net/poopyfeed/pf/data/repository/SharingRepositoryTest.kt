package net.poopyfeed.pf.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.models.ChildInvite
import net.poopyfeed.pf.data.models.ChildShare
import net.poopyfeed.pf.data.models.CreateShareRequest
import net.poopyfeed.pf.data.models.ShareInvite
import net.poopyfeed.pf.data.models.ShareInviteResponse
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
            userEmail = "co-parent@example.com",
            role = "co-parent",
            roleDisplay = "Co-parent",
            createdAt = "2024-01-15T10:00:00Z",
        )
    coEvery { apiService.listShares(childId = 1) } returns listOf(share)

    val result = repository.listShares(childId = 1)

    assertIs<ApiResult.Success<List<ChildShare>>>(result)
    assertEquals(1, result.data.size)
    assertEquals(share.id, result.data.first().id)
    assertEquals("co-parent@example.com", result.data.first().userEmail)
  }

  @Test
  fun `listInvites success returns Success with invites`() = runTest {
    val invite = TestFixtures.mockChildInvite(id = 5)
    coEvery { apiService.listInvites(childId = 1) } returns listOf(invite)

    val result = repository.listInvites(childId = 1)

    assertIs<ApiResult.Success<List<ChildInvite>>>(result)
    assertEquals(1, result.data.size)
    assertEquals(5, result.data.first().id)
    assertEquals("abc123token", result.data.first().token)
  }

  @Test
  fun `toggleInvite success returns Success with updated invite`() = runTest {
    val invite = TestFixtures.mockChildInvite(id = 5, isActive = false)
    coEvery { apiService.toggleInvite(childId = 1, invitePk = 5) } returns invite

    val result = repository.toggleInvite(childId = 1, invitePk = 5)

    assertIs<ApiResult.Success<ChildInvite>>(result)
    assertEquals(5, result.data.id)
    assertEquals(false, result.data.isActive)
  }

  @Test
  fun `deleteInvite success returns Success`() = runTest {
    coEvery { apiService.deleteInvite(childId = 1, invitePk = 5) } returns Unit

    val result = repository.deleteInvite(childId = 1, invitePk = 5)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `listShares network error returns Error`() = runTest {
    coEvery { apiService.listShares(any()) } throws java.io.IOException("Network down")

    val result = repository.listShares(childId = 1)

    assertIs<ApiResult.Error<List<ChildShare>>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `createShare success returns Success with ShareInviteResponse`() = runTest {
    val request = CreateShareRequest(role = "caregiver")
    val response =
        ShareInviteResponse(
            id = 10,
            token = "abc123",
            role = "caregiver",
            isActive = true,
            createdAt = "2024-01-15T10:00:00Z",
            inviteUrl = "https://example.com/invite/abc123",
        )
    coEvery { apiService.createShare(childId = 1, request = request) } returns response

    val result = repository.createShare(childId = 1, request = request)

    assertIs<ApiResult.Success<ShareInviteResponse>>(result)
    assertEquals("caregiver", result.data.role)
    assertEquals("abc123", result.data.token)
  }

  @Test
  fun `createShare http error returns Error`() = runTest {
    val request = CreateShareRequest(role = "caregiver")
    val errorResponse =
        retrofit2.Response.error<ShareInviteResponse>(400, "Bad Request".toResponseBody(null))
    coEvery { apiService.createShare(any(), any()) } throws retrofit2.HttpException(errorResponse)

    val result = repository.createShare(childId = 1, request = request)

    assertIs<ApiResult.Error<ShareInviteResponse>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(400, result.error.statusCode)
  }

  @Test
  fun `getPendingInvites returns empty list`() = runTest {
    val result = repository.getPendingInvites()

    assertIs<ApiResult.Success<List<ShareInvite>>>(result)
    assertEquals(0, result.data.size)
  }

  @Test
  fun `acceptInvite with token success returns Success with Child`() = runTest {
    val child = net.poopyfeed.pf.TestFixtures.mockChild(id = 3, name = "Baby")
    coEvery { apiService.acceptInvite(any()) } returns child

    val result = repository.acceptInvite("sometoken")

    assertIs<ApiResult.Success<Child>>(result)
    assertEquals(3, result.data.id)
  }

  @Test
  fun `acceptInvite network error returns Error`() = runTest {
    coEvery { apiService.acceptInvite(any()) } throws java.io.IOException("Network down")

    val result = repository.acceptInvite("badtoken")

    assertIs<ApiResult.Error<Child>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }
}
