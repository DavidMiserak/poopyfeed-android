package net.poopyfeed.pf.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.DeviceTokenDeleteRequest
import net.poopyfeed.pf.data.models.DeviceTokenRequest
import net.poopyfeed.pf.data.models.MarkReadRequest
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.models.QuietHours
import net.poopyfeed.pf.data.models.QuietHoursUpdate
import net.poopyfeed.pf.data.models.toApiError
import net.poopyfeed.pf.di.IoDispatcher

/**
 * Network-only repository for in-app notifications. No local cache; all operations call the API.
 */
class NotificationsRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  /** Get unread notification count. */
  suspend fun getUnreadCount(): ApiResult<Int> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.getUnreadCount()
          ApiResult.Success(response.count)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Get paginated list of notifications. */
  suspend fun listNotifications(page: Int = 1): ApiResult<PaginatedResponse<Notification>> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.listNotifications(page)
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Get paginated notifications using Paging 3 library. */
  fun pagedNotifications(): Flow<PagingData<Notification>> {
    return Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { NotificationsPagingSource(apiService) }
    ).flow
  }

  /** Mark all notifications as read. Returns number of updated items. */
  suspend fun markAllRead(): ApiResult<Int> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.markAllNotificationsRead()
          ApiResult.Success(response.updated)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Mark a single notification as read. */
  suspend fun markAsRead(id: Int): ApiResult<Notification> =
      withContext(ioDispatcher) {
        try {
          val notification = apiService.markNotificationRead(id, MarkReadRequest())
          ApiResult.Success(notification)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Get quiet hours (created with defaults if missing). */
  suspend fun getQuietHours(): ApiResult<QuietHours> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.getQuietHours()
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Update quiet hours. */
  suspend fun updateQuietHours(request: QuietHoursUpdate): ApiResult<QuietHours> =
      withContext(ioDispatcher) {
        try {
          val response = apiService.updateQuietHours(request)
          ApiResult.Success(response)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Register an FCM device token with the backend. */
  suspend fun registerDeviceToken(token: String): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.registerDeviceToken(DeviceTokenRequest(token = token))
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Unregister an FCM device token from the backend. */
  suspend fun unregisterDeviceToken(token: String): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.unregisterDeviceToken(DeviceTokenDeleteRequest(token = token))
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }
}
