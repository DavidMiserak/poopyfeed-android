package net.poopyfeed.pf.data.repository

import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.MarkReadRequest
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.data.models.PaginatedResponse
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
}
