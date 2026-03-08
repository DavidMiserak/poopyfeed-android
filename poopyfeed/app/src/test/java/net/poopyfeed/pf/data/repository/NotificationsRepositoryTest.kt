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
import net.poopyfeed.pf.data.models.DeviceTokenDeleteRequest
import net.poopyfeed.pf.data.models.DeviceTokenRequest
import net.poopyfeed.pf.data.models.DeviceTokenResponse
import net.poopyfeed.pf.data.models.MarkAllReadResponse
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.models.QuietHours
import net.poopyfeed.pf.data.models.QuietHoursUpdate
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
  fun `listNotifications with no args uses default page 1`() = runTest {
    val response =
        PaginatedResponse<Notification>(
            count = 0, next = null, previous = null, results = emptyList())
    io.mockk.coEvery { apiService.listNotifications(1) } returns response

    val result = repository.listNotifications()

    assertIs<ApiResult.Success<PaginatedResponse<Notification>>>(result)
    io.mockk.coVerify { apiService.listNotifications(1) }
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

  @Test
  fun `getQuietHours success returns Success with quiet hours`() = runTest {
    val quietHours = QuietHours(enabled = true, startTime = "22:00:00", endTime = "07:00:00")
    io.mockk.coEvery { apiService.getQuietHours() } returns quietHours

    val result = repository.getQuietHours()

    assertIs<ApiResult.Success<QuietHours>>(result)
    assertEquals(true, result.data.enabled)
  }

  @Test
  fun `getQuietHours network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.getQuietHours() } throws IOException("Network down")

    val result = repository.getQuietHours()

    assertIs<ApiResult.Error<QuietHours>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `updateQuietHours success returns Success with updated quiet hours`() = runTest {
    val request = QuietHoursUpdate(enabled = false, startTime = "22:00:00", endTime = "07:00:00")
    val updatedQuietHours =
        QuietHours(enabled = false, startTime = "22:00:00", endTime = "07:00:00")
    io.mockk.coEvery { apiService.updateQuietHours(request) } returns updatedQuietHours

    val result = repository.updateQuietHours(request)

    assertIs<ApiResult.Success<QuietHours>>(result)
    assertEquals(false, result.data.enabled)
  }

  @Test
  fun `updateQuietHours network error returns Error`() = runTest {
    val request = QuietHoursUpdate(enabled = true, startTime = "22:00:00", endTime = "07:00:00")
    io.mockk.coEvery { apiService.updateQuietHours(request) } throws IOException("Network down")

    val result = repository.updateQuietHours(request)

    assertIs<ApiResult.Error<QuietHours>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `registerDeviceToken success returns Success`() = runTest {
    val token = "device-token-123"
    val response = DeviceTokenResponse(status = "registered")
    io.mockk.coEvery { apiService.registerDeviceToken(DeviceTokenRequest(token)) } returns response

    val result = repository.registerDeviceToken(token)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `registerDeviceToken network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.registerDeviceToken(any()) } throws IOException("Network down")

    val result = repository.registerDeviceToken("token")

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `unregisterDeviceToken success returns Success`() = runTest {
    val token = "device-token-456"
    val response = DeviceTokenResponse(status = "unregistered")
    io.mockk.coEvery { apiService.unregisterDeviceToken(DeviceTokenDeleteRequest(token)) } returns
        response

    val result = repository.unregisterDeviceToken(token)

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `unregisterDeviceToken network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.unregisterDeviceToken(any()) } throws IOException("Network down")

    val result = repository.unregisterDeviceToken("token")

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }
}
