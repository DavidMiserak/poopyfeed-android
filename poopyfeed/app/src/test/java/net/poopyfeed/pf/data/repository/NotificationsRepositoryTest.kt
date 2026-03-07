package net.poopyfeed.pf.data.repository

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.MarkAllReadResponse
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.models.UnreadCountResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var repository: NotificationsRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = io.mockk.mockk()
    repository = NotificationsRepository(apiService, ioDispatcher = testDispatcher)
  }

  @Test
  fun `getUnreadCount success returns Success with count`() = runTest {
    io.mockk.coEvery { apiService.getUnreadCount() } returns UnreadCountResponse(count = 7)

    val result = repository.getUnreadCount()

    assertIs<ApiResult.Success<Int>>(result)
    assertEquals(7, result.data)
  }

  @Test
  fun `getUnreadCount network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.getUnreadCount() } throws IOException("Network down")

    val result = repository.getUnreadCount()

    assertIs<ApiResult.Error<Int>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `listNotifications success returns Success with paginated results`() = runTest {
    val notifications = listOf(TestFixtures.mockNotification())
    val response =
        PaginatedResponse(count = 1, next = null, previous = null, results = notifications)
    io.mockk.coEvery { apiService.listNotifications(1) } returns response

    val result = repository.listNotifications(page = 1)

    assertIs<ApiResult.Success<PaginatedResponse<Notification>>>(result)
    assertEquals(1, result.data.results.size)
    assertEquals(notifications[0].id, result.data.results[0].id)
  }

  @Test
  fun `listNotifications with page 2 calls api with page 2`() = runTest {
    val response =
        PaginatedResponse<Notification>(
            count = 0, next = null, previous = null, results = emptyList())
    io.mockk.coEvery { apiService.listNotifications(2) } returns response

    repository.listNotifications(page = 2)

    io.mockk.coVerify { apiService.listNotifications(2) }
  }

  @Test
  fun `listNotifications network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.listNotifications(1) } throws IOException("Timeout")

    val result = repository.listNotifications(page = 1)

    assertIs<ApiResult.Error<PaginatedResponse<Notification>>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `markAllRead success returns Success with updated count`() = runTest {
    io.mockk.coEvery { apiService.markAllNotificationsRead() } returns
        MarkAllReadResponse(updated = 3)

    val result = repository.markAllRead()

    assertIs<ApiResult.Success<Int>>(result)
    assertEquals(3, result.data)
  }

  @Test
  fun `markAllRead network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.markAllNotificationsRead() } throws IOException("Failed")

    val result = repository.markAllRead()

    assertIs<ApiResult.Error<Int>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `markAsRead success returns Success with notification`() = runTest {
    val notification = TestFixtures.mockNotification(id = 5, isRead = true)
    io.mockk.coEvery {
      apiService.markNotificationRead(5, net.poopyfeed.pf.data.models.MarkReadRequest())
    } returns notification

    val result = repository.markAsRead(5)

    assertIs<ApiResult.Success<Notification>>(result)
    assertEquals(5, result.data.id)
    assertEquals(true, result.data.isRead)
  }

  @Test
  fun `markAsRead network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.markNotificationRead(10, any()) } throws
        IOException("Network error")

    val result = repository.markAsRead(10)

    assertIs<ApiResult.Error<Notification>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `markAsRead http error returns HttpError`() = runTest {
    val errorResponse =
        retrofit2.Response.error<Notification>(404, "Not Found".toResponseBody(null))
    io.mockk.coEvery { apiService.markNotificationRead(10, any()) } throws
        retrofit2.HttpException(errorResponse)

    val result = repository.markAsRead(10)

    assertIs<ApiResult.Error<Notification>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(404, result.error.statusCode)
  }
}
