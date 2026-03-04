package net.poopyfeed.pf.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.ChildDao
import net.poopyfeed.pf.data.db.ChildEntity
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.DiaperEntity
import net.poopyfeed.pf.data.db.FeedingDao
import net.poopyfeed.pf.data.db.FeedingEntity
import net.poopyfeed.pf.data.db.NapDao
import net.poopyfeed.pf.data.db.NapEntity
import net.poopyfeed.pf.data.models.*
import net.poopyfeed.pf.data.models.toApiError

/**
 * Enhanced repository with local Room caching.
 *
 * Strategy: Cache-first with network sync
 * 1. Check local Room database first
 * 2. If data is stale (> 5 minutes), refresh from API
 * 3. Always emit local data (immediate UI update)
 * 4. Update local cache when API responds
 *
 * This provides:
 * - Offline-first experience
 * - Instant UI updates from cache
 * - Background sync in the future
 */
class CachedChildrenRepository(
    private val apiService: PoopyFeedApiService,
    private val childDao: ChildDao
) {

  /**
   * Get all children from local cache as Flow.
   *
   * Returns:
   * - Local data immediately (or Success(emptyList()) when cache is empty)
   * - Refreshes from API in background if stale
   * - Updates Flow when new data arrives
   *
   * Empty cache emits Success(emptyList()); call [refreshChildren] to fetch from network and show
   * loading during that call if you need to distinguish "not yet synced" from "synced and empty".
   *
   * Usage:
   * ```
   * repo.listChildrenCached()
   *     .collect { result -> when (result) { is Success -> showChildren(result.data) ... } }
   * ```
   */
  fun listChildrenCached(): Flow<ApiResult<List<Child>>> =
      childDao.getAllChildrenFlow().map { entities ->
        if (entities.isEmpty()) {
          ApiResult.Success(emptyList())
        } else {
          ApiResult.Success(entities.map { it.toApiModel() })
        }
      }

  /** Manually refresh children from API and update cache. Useful for pull-to-refresh patterns. */
  suspend fun refreshChildren(): ApiResult<List<Child>> =
      try {
        val response = apiService.listChildren(page = 1)
        val entities = response.results.map { ChildEntity.fromApiModel(it) }

        // Update local cache
        childDao.upsertChildren(entities)

        ApiResult.Success(response.results)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  /** Get single child from cache as Flow. Emits updates whenever the child data changes in Room. */
  fun getChildCached(childId: Int): Flow<Child?> =
      childDao.getChildFlow(childId).map { it?.toApiModel() }

  /** Create child: API-first, then cache. */
  suspend fun createChild(request: CreateChildRequest): ApiResult<Child> =
      try {
        val child = apiService.createChild(request)
        val entity = ChildEntity.fromApiModel(child)
        childDao.upsertChild(entity)
        ApiResult.Success(child)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  /** Update child: API-first, then cache. */
  suspend fun updateChild(childId: Int, request: CreateChildRequest): ApiResult<Child> =
      try {
        val child = apiService.updateChild(childId, request)
        val entity = ChildEntity.fromApiModel(child)
        childDao.upsertChild(entity)
        ApiResult.Success(child)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  /** Delete child: API-first, then remove from cache. */
  suspend fun deleteChild(childId: Int): ApiResult<Unit> =
      try {
        apiService.deleteChild(childId)
        childDao.deleteChild(childId)
        ApiResult.Success(Unit)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  /** Clear all local cache (e.g., on logout). */
  suspend fun clearCache() {
    childDao.clearAll()
  }
}

/** Cached Feedings Repository with local Room support. */
class CachedFeedingsRepository(
    private val apiService: PoopyFeedApiService,
    private val feedingDao: FeedingDao
) {

  /** Get all feedings for a child from cache as Flow. Empty cache emits Success(emptyList()). */
  fun listFeedingsCached(childId: Int): Flow<ApiResult<List<Feeding>>> =
      feedingDao.getFeedingsFlow(childId).map { entities ->
        if (entities.isEmpty()) {
          ApiResult.Success(emptyList())
        } else {
          ApiResult.Success(entities.map { it.toApiModel() })
        }
      }

  /** Refresh feedings from API. */
  suspend fun refreshFeedings(childId: Int): ApiResult<List<Feeding>> =
      try {
        val response = apiService.listFeedings(childId, page = 1)
        val entities = response.results.map { FeedingEntity.fromApiModel(it) }
        feedingDao.upsertFeedings(entities)
        ApiResult.Success(response.results)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  /** Create feeding: API-first, then cache. */
  suspend fun createFeeding(childId: Int, request: CreateFeedingRequest): ApiResult<Feeding> =
      try {
        val feeding = apiService.createFeeding(childId, request)
        val entity = FeedingEntity.fromApiModel(feeding)
        feedingDao.upsertFeeding(entity)
        ApiResult.Success(feeding)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  /** Delete feeding: API-first, then cache. */
  suspend fun deleteFeeding(childId: Int, feedingId: Int): ApiResult<Unit> =
      try {
        apiService.deleteFeeding(childId, feedingId)
        feedingDao.deleteFeeding(feedingId)
        ApiResult.Success(Unit)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  /** Clear feedings cache for a child. */
  suspend fun clearChildCache(childId: Int) {
    feedingDao.clearChildFeedings(childId)
  }
}

/** Cached Diapers Repository with local Room support. */
class CachedDiapersRepository(
    private val apiService: PoopyFeedApiService,
    private val diaperDao: DiaperDao
) {

  /** Empty cache emits Success(emptyList()). */
  fun listDiapersCached(childId: Int): Flow<ApiResult<List<Diaper>>> =
      diaperDao.getDiapersFlow(childId).map { entities ->
        if (entities.isEmpty()) {
          ApiResult.Success(emptyList())
        } else {
          ApiResult.Success(entities.map { it.toApiModel() })
        }
      }

  suspend fun refreshDiapers(childId: Int): ApiResult<List<Diaper>> =
      try {
        val response = apiService.listDiapers(childId, page = 1)
        val entities = response.results.map { DiaperEntity.fromApiModel(it) }
        diaperDao.upsertDiapers(entities)
        ApiResult.Success(response.results)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  suspend fun createDiaper(childId: Int, request: CreateDiaperRequest): ApiResult<Diaper> =
      try {
        val diaper = apiService.createDiaper(childId, request)
        val entity = DiaperEntity.fromApiModel(diaper)
        diaperDao.upsertDiaper(entity)
        ApiResult.Success(diaper)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  suspend fun deleteDiaper(childId: Int, diaperId: Int): ApiResult<Unit> =
      try {
        apiService.deleteDiaper(childId, diaperId)
        diaperDao.deleteDiaper(diaperId)
        ApiResult.Success(Unit)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  suspend fun clearChildCache(childId: Int) {
    diaperDao.clearChildDiapers(childId)
  }
}

/** Cached Naps Repository with local Room support. */
class CachedNapsRepository(
    private val apiService: PoopyFeedApiService,
    private val napDao: NapDao
) {

  /** Empty cache emits Success(emptyList()). */
  fun listNapsCached(childId: Int): Flow<ApiResult<List<Nap>>> =
      napDao.getNapsFlow(childId).map { entities ->
        if (entities.isEmpty()) {
          ApiResult.Success(emptyList())
        } else {
          ApiResult.Success(entities.map { it.toApiModel() })
        }
      }

  suspend fun refreshNaps(childId: Int): ApiResult<List<Nap>> =
      try {
        val response = apiService.listNaps(childId, page = 1)
        val entities = response.results.map { NapEntity.fromApiModel(it) }
        napDao.upsertNaps(entities)
        ApiResult.Success(response.results)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  suspend fun createNap(childId: Int, request: CreateNapRequest): ApiResult<Nap> =
      try {
        val nap = apiService.createNap(childId, request)
        val entity = NapEntity.fromApiModel(nap)
        napDao.upsertNap(entity)
        ApiResult.Success(nap)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  suspend fun updateNap(childId: Int, napId: Int, request: UpdateNapRequest): ApiResult<Nap> =
      try {
        val nap = apiService.updateNap(childId, napId, request)
        val entity = NapEntity.fromApiModel(nap)
        napDao.upsertNap(entity)
        ApiResult.Success(nap)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  suspend fun deleteNap(childId: Int, napId: Int): ApiResult<Unit> =
      try {
        apiService.deleteNap(childId, napId)
        napDao.deleteNap(napId)
        ApiResult.Success(Unit)
      } catch (e: Exception) {
        ApiResult.Error(e.toApiError())
      }

  suspend fun clearChildCache(childId: Int) {
    napDao.clearChildNaps(childId)
  }
}
