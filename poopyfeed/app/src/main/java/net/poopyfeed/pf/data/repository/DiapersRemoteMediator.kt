package net.poopyfeed.pf.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import java.io.IOException
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.DiaperEntity
import net.poopyfeed.pf.data.db.RemoteKeyDao
import net.poopyfeed.pf.data.db.RemoteKeyEntity
import retrofit2.HttpException

private const val DIAPERS_PAGE_SIZE = 20
private const val DIAPERS_STARTING_PAGE = 1
private const val ENTITY_TYPE = "diapers"

/**
 * RemoteMediator for paginated diapers list with Paging 3.
 *
 * Handles loading from the API and syncing to Room database:
 * - REFRESH: Clears existing diapers and loads first page
 * - APPEND: Loads next page (pagination)
 * - PREPEND: Not used (diapers are reverse-chronological, new items don't prepend)
 *
 * @param childId The child ID to fetch diapers for
 * @param apiService Retrofit service for API calls
 * @param dao DiaperDao for Room database access
 * @param remoteKeyDao RemoteKeyDao for tracking pagination state
 */
@OptIn(ExperimentalPagingApi::class)
class DiapersRemoteMediator(
    private val childId: Int,
    private val apiService: PoopyFeedApiService,
    private val dao: DiaperDao,
    private val remoteKeyDao: RemoteKeyDao,
) : RemoteMediator<Int, DiaperEntity>() {

  override suspend fun load(
      loadType: LoadType,
      state: PagingState<Int, DiaperEntity>,
  ): MediatorResult {
    return try {
      // Determine the page to load
      val loadKey =
          when (loadType) {
            LoadType.REFRESH -> {
              // Clear remote key on REFRESH to reset pagination
              remoteKeyDao.clearKey(childId, ENTITY_TYPE)
              DIAPERS_STARTING_PAGE
            }
            LoadType.PREPEND -> {
              // Diapers are reverse-chronological (newest first), so we don't prepend new items
              return MediatorResult.Success(endOfPaginationReached = true)
            }
            LoadType.APPEND -> {
              // Get next page from RemoteKey, or end pagination if not found
              val key = remoteKeyDao.getKey(childId, ENTITY_TYPE)
              key?.nextPage ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
          }

      // Fetch from API
      val response =
          apiService.listDiapers(childId = childId, page = loadKey, pageSize = DIAPERS_PAGE_SIZE)

      // Clear database on REFRESH to avoid duplicates
      if (loadType == LoadType.REFRESH) {
        dao.clearChildDiapers(childId)
      }

      // Convert API response to entities
      val entities =
          response.results.map { it.toDiaper(childId) }.map { DiaperEntity.fromApiModel(it) }

      // Upsert into Room
      dao.upsertDiapers(entities)

      // Save next page to RemoteKey (null if no more pages)
      val nextPage = if (response.next == null) null else loadKey + 1
      remoteKeyDao.upsert(RemoteKeyEntity(childId, ENTITY_TYPE, nextPage))

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
