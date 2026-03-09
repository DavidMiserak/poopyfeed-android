package net.poopyfeed.pf.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import java.io.IOException
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.NapDao
import net.poopyfeed.pf.data.db.NapEntity
import retrofit2.HttpException

private const val NAPS_PAGE_SIZE = 20
private const val NAPS_STARTING_PAGE = 1

/**
 * RemoteMediator for paginated naps list with Paging 3.
 *
 * Handles loading from the API and syncing to Room database:
 * - REFRESH: Clears existing naps and loads first page
 * - APPEND: Loads next page (pagination)
 * - PREPEND: Not used (naps are reverse-chronological, new items don't prepend)
 *
 * @param childId The child ID to fetch naps for
 * @param apiService Retrofit service for API calls
 * @param dao NapDao for Room database access
 */
@OptIn(ExperimentalPagingApi::class)
class NapsRemoteMediator(
    private val childId: Int,
    private val apiService: PoopyFeedApiService,
    private val dao: NapDao,
) : RemoteMediator<Int, NapEntity>() {

  override suspend fun load(
      loadType: LoadType,
      state: PagingState<Int, NapEntity>,
  ): MediatorResult {
    return try {
      // Determine the page to load
      val loadKey =
          when (loadType) {
            LoadType.REFRESH -> NAPS_STARTING_PAGE
            LoadType.PREPEND -> {
              // Naps are reverse-chronological (newest first), so we don't prepend new items
              return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
              val lastItem =
                  state.lastItemOrNull()
                      ?: return MediatorResult.Success(endOfPaginationReached = true)
              // Calculate next page: anchorPosition / pageSize + 2
              (state.anchorPosition ?: 0) / NAPS_PAGE_SIZE + 2
            }
          }

      // Fetch from API
      val response =
          apiService.listNaps(childId = childId, page = loadKey, pageSize = NAPS_PAGE_SIZE)

      // Clear database on REFRESH to avoid duplicates
      if (loadType == LoadType.REFRESH) {
        dao.clearChildNaps(childId)
      }

      // Convert API response to entities
      val entities = response.results.map { it.toNap(childId) }.map { NapEntity.fromApiModel(it) }

      // Upsert into Room
      dao.upsertNaps(entities)

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
