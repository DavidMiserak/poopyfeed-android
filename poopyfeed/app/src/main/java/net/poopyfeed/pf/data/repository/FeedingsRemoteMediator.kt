package net.poopyfeed.pf.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import java.io.IOException
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.FeedingDao
import net.poopyfeed.pf.data.db.FeedingEntity
import retrofit2.HttpException

private const val FEEDINGS_PAGE_SIZE = 20
private const val FEEDINGS_STARTING_PAGE = 1

/**
 * RemoteMediator for paginated feedings list with Paging 3.
 *
 * Handles loading from the API and syncing to Room database:
 * - REFRESH: Clears existing feedings and loads first page
 * - APPEND: Loads next page (pagination)
 * - PREPEND: Not used (feedings are reverse-chronological, new items don't prepend)
 *
 * @param childId The child ID to fetch feedings for
 * @param apiService Retrofit service for API calls
 * @param dao FeedingDao for Room database access
 */
@OptIn(ExperimentalPagingApi::class)
class FeedingsRemoteMediator(
    private val childId: Int,
    private val apiService: PoopyFeedApiService,
    private val dao: FeedingDao,
) : RemoteMediator<Int, FeedingEntity>() {

  override suspend fun load(
      loadType: LoadType,
      state: PagingState<Int, FeedingEntity>,
  ): MediatorResult {
    return try {
      // Determine the page to load
      val loadKey =
          when (loadType) {
            LoadType.REFRESH -> FEEDINGS_STARTING_PAGE
            LoadType.PREPEND -> {
              // Feedings are reverse-chronological (newest first), so we don't prepend new items
              return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
              val lastItem =
                  state.lastItemOrNull()
                      ?: return MediatorResult.Success(endOfPaginationReached = true)
              // Calculate next page: anchorPosition / pageSize + 2
              (state.anchorPosition ?: 0) / FEEDINGS_PAGE_SIZE + 2
            }
          }

      // Fetch from API
      val response =
          apiService.listFeedings(childId = childId, page = loadKey, pageSize = FEEDINGS_PAGE_SIZE)

      // Clear database on REFRESH to avoid duplicates
      if (loadType == LoadType.REFRESH) {
        dao.clearChildFeedings(childId)
      }

      // Convert API response to entities
      val entities =
          response.results.map { it.toFeeding(childId) }.map { FeedingEntity.fromApiModel(it) }

      // Upsert into Room
      dao.upsertFeedings(entities)

      // Return success with endOfPaginationReached flag
      MediatorResult.Success(endOfPaginationReached = response.next == null)
    } catch (e: IOException) {
      // Network error (no internet, connection timeout, etc.)
      MediatorResult.Error(e)
    } catch (e: HttpException) {
      // HTTP error (4xx, 5xx)
      MediatorResult.Error(e)
    }
  }
}
