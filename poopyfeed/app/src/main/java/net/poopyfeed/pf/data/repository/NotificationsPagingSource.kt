package net.poopyfeed.pf.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.io.IOException
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.Notification
import retrofit2.HttpException

private const val NOTIFICATIONS_PAGE_SIZE = 20
private const val NOTIFICATIONS_STARTING_PAGE = 1

/**
 * Network-only PagingSource for notifications pagination.
 *
 * Notifications use simple network-only pagination (no Room caching) unlike activity lists
 * (feedings/diapers/naps) which use RemoteMediator + Room. This is simpler since notification
 * history doesn't require offline-first behavior.
 */
class NotificationsPagingSource(
    private val apiService: PoopyFeedApiService,
) : PagingSource<Int, Notification>() {

  override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Notification> {
    return try {
      val page = params.key ?: NOTIFICATIONS_STARTING_PAGE
      val response = apiService.listNotifications(page = page, pageSize = NOTIFICATIONS_PAGE_SIZE)

      LoadResult.Page(
          data = response.results,
          prevKey = if (page == NOTIFICATIONS_STARTING_PAGE) null else page - 1,
          nextKey = if (response.next == null) null else page + 1)
    } catch (e: IOException) {
      LoadResult.Error(e)
    } catch (e: HttpException) {
      LoadResult.Error(e)
    }
  }

  override fun getRefreshKey(state: PagingState<Int, Notification>): Int? {
    return state.anchorPosition?.let { anchorPosition ->
      state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
          ?: state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
    }
  }
}
