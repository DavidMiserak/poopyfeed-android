package net.poopyfeed.pf.data.repository

import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
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
import net.poopyfeed.pf.di.IoDispatcher

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
class CachedChildrenRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    private val childDao: ChildDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  private val _hasSynced = MutableStateFlow(false)
  /**
   * True after at least one successful [refreshChildren]. Use to distinguish NotSynced vs
   * Synced(empty).
   */
  val hasSyncedFlow: Flow<Boolean> = _hasSynced.asStateFlow()

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
      childDao
          .getAllChildrenFlow()
          .map { entities ->
            if (entities.isEmpty()) {
              ApiResult.Success(emptyList())
            } else {
              ApiResult.Success(entities.map { it.toApiModel() })
            }
          }
          .flowOn(ioDispatcher)

  /** Manually refresh children from API and update cache. Useful for pull-to-refresh patterns. */
  suspend fun refreshChildren(): ApiResult<List<Child>> =
      withContext(ioDispatcher) {
        try {
          coroutineScope {
            val allResults = mutableListOf<Child>()
            var page = 1
            var response = apiService.listChildren(page = page)
            allResults.addAll(response.results)
            while (response.next != null) {
              page += 1
              response = apiService.listChildren(page = page)
              allResults.addAll(response.results)
            }
            val entities = allResults.map { ChildEntity.fromApiModel(it) }
            childDao.upsertChildren(entities)
            _hasSynced.value = true
            ApiResult.Success(allResults)
          }
        } catch (e: CancellationException) {
          throw e // preserve structured concurrency
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Get single child from cache as Flow. Emits updates whenever the child data changes in Room. */
  fun getChildCached(childId: Int): Flow<Child?> =
      childDao.getChildFlow(childId).map { it?.toApiModel() }.flowOn(ioDispatcher)

  /** Create child: API-first, then cache. */
  suspend fun createChild(request: CreateChildRequest): ApiResult<Child> =
      withContext(ioDispatcher) {
        try {
          val child = apiService.createChild(request)
          val entity = ChildEntity.fromApiModel(child)
          childDao.upsertChild(entity)
          ApiResult.Success(child)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Update child: API-first, then cache. */
  suspend fun updateChild(childId: Int, request: CreateChildRequest): ApiResult<Child> =
      withContext(ioDispatcher) {
        try {
          val child = apiService.updateChild(childId, request)
          val entity = ChildEntity.fromApiModel(child)
          childDao.upsertChild(entity)
          ApiResult.Success(child)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Delete child: API-first, then remove from cache. */
  suspend fun deleteChild(childId: Int): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.deleteChild(childId)
          childDao.deleteChild(childId)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /**
   * Clear all local cache (e.g., on logout). Resets sync status so UI can show NotSynced until next
   * refresh.
   */
  suspend fun clearCache() {
    withContext(ioDispatcher) { childDao.clearAll() }
    _hasSynced.value = false
  }
}

/** Cached Feedings Repository with local Room support. */
class CachedFeedingsRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    private val feedingDao: FeedingDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  private val _syncedChildIds = MutableStateFlow<Set<Int>>(emptySet())
  /** True for [childId] after at least one successful [refreshFeedings]. */
  fun hasSyncedFlow(childId: Int): Flow<Boolean> =
      _syncedChildIds.asStateFlow().map { childId in it }

  /** Get all feedings for a child from cache as Flow. Empty cache emits Success(emptyList()). */
  fun listFeedingsCached(childId: Int): Flow<ApiResult<List<Feeding>>> =
      feedingDao
          .getFeedingsFlow(childId)
          .map { entities ->
            if (entities.isEmpty()) {
              ApiResult.Success(emptyList())
            } else {
              ApiResult.Success(entities.map { it.toApiModel() })
            }
          }
          .flowOn(ioDispatcher)

  /** Refresh feedings from API (all pages). */
  suspend fun refreshFeedings(childId: Int): ApiResult<List<Feeding>> =
      withContext(ioDispatcher) {
        try {
          coroutineScope {
            val allResults = mutableListOf<Feeding>()
            var page = 1
            var response = apiService.listFeedings(childId, page = page)
            allResults.addAll(response.results)
            while (response.next != null) {
              page += 1
              response = apiService.listFeedings(childId, page = page)
              allResults.addAll(response.results)
            }
            val entities = allResults.map { FeedingEntity.fromApiModel(it) }
            feedingDao.upsertFeedings(entities)
            _syncedChildIds.value = _syncedChildIds.value + childId
            ApiResult.Success(allResults)
          }
        } catch (e: CancellationException) {
          throw e // preserve structured concurrency
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Create feeding: API-first, then cache. */
  suspend fun createFeeding(childId: Int, request: CreateFeedingRequest): ApiResult<Feeding> =
      withContext(ioDispatcher) {
        try {
          val feeding = apiService.createFeeding(childId, request)
          val entity = FeedingEntity.fromApiModel(feeding)
          feedingDao.upsertFeeding(entity)
          ApiResult.Success(feeding)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Delete feeding: API-first, then cache. */
  suspend fun deleteFeeding(childId: Int, feedingId: Int): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.deleteFeeding(childId, feedingId)
          feedingDao.deleteFeeding(feedingId)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  /** Clear feedings cache for a child. Resets sync status for this child. */
  suspend fun clearChildCache(childId: Int) {
    withContext(ioDispatcher) { feedingDao.clearChildFeedings(childId) }
    _syncedChildIds.value = _syncedChildIds.value - childId
  }
}

/** Cached Diapers Repository with local Room support. */
class CachedDiapersRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    private val diaperDao: DiaperDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  private val _syncedChildIds = MutableStateFlow<Set<Int>>(emptySet())
  /** True for [childId] after at least one successful [refreshDiapers]. */
  fun hasSyncedFlow(childId: Int): Flow<Boolean> =
      _syncedChildIds.asStateFlow().map { childId in it }

  /** Empty cache emits Success(emptyList()). */
  fun listDiapersCached(childId: Int): Flow<ApiResult<List<Diaper>>> =
      diaperDao
          .getDiapersFlow(childId)
          .map { entities ->
            if (entities.isEmpty()) {
              ApiResult.Success(emptyList())
            } else {
              ApiResult.Success(entities.map { it.toApiModel() })
            }
          }
          .flowOn(ioDispatcher)

  /** Refresh diapers from API (all pages). */
  suspend fun refreshDiapers(childId: Int): ApiResult<List<Diaper>> =
      withContext(ioDispatcher) {
        try {
          coroutineScope {
            val allResults = mutableListOf<Diaper>()
            var page = 1
            var response = apiService.listDiapers(childId, page = page)
            allResults.addAll(response.results)
            while (response.next != null) {
              page += 1
              response = apiService.listDiapers(childId, page = page)
              allResults.addAll(response.results)
            }
            val entities = allResults.map { DiaperEntity.fromApiModel(it) }
            diaperDao.upsertDiapers(entities)
            _syncedChildIds.value = _syncedChildIds.value + childId
            ApiResult.Success(allResults)
          }
        } catch (e: CancellationException) {
          throw e // preserve structured concurrency
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun createDiaper(childId: Int, request: CreateDiaperRequest): ApiResult<Diaper> =
      withContext(ioDispatcher) {
        try {
          val diaper = apiService.createDiaper(childId, request)
          val entity = DiaperEntity.fromApiModel(diaper)
          diaperDao.upsertDiaper(entity)
          ApiResult.Success(diaper)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun deleteDiaper(childId: Int, diaperId: Int): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.deleteDiaper(childId, diaperId)
          diaperDao.deleteDiaper(diaperId)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun clearChildCache(childId: Int) {
    withContext(ioDispatcher) { diaperDao.clearChildDiapers(childId) }
    _syncedChildIds.value = _syncedChildIds.value - childId
  }
}

/** Cached Naps Repository with local Room support. */
class CachedNapsRepository
@Inject
constructor(
    private val apiService: PoopyFeedApiService,
    private val napDao: NapDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

  private val _syncedChildIds = MutableStateFlow<Set<Int>>(emptySet())
  /** True for [childId] after at least one successful [refreshNaps]. */
  fun hasSyncedFlow(childId: Int): Flow<Boolean> =
      _syncedChildIds.asStateFlow().map { childId in it }

  /** Empty cache emits Success(emptyList()). */
  fun listNapsCached(childId: Int): Flow<ApiResult<List<Nap>>> =
      napDao
          .getNapsFlow(childId)
          .map { entities ->
            if (entities.isEmpty()) {
              ApiResult.Success(emptyList())
            } else {
              ApiResult.Success(entities.map { it.toApiModel() })
            }
          }
          .flowOn(ioDispatcher)

  /** Refresh naps from API (all pages). */
  suspend fun refreshNaps(childId: Int): ApiResult<List<Nap>> =
      withContext(ioDispatcher) {
        try {
          coroutineScope {
            val allResults = mutableListOf<Nap>()
            var page = 1
            var response = apiService.listNaps(childId, page = page)
            allResults.addAll(response.results)
            while (response.next != null) {
              page += 1
              response = apiService.listNaps(childId, page = page)
              allResults.addAll(response.results)
            }
            val entities = allResults.map { NapEntity.fromApiModel(it) }
            napDao.upsertNaps(entities)
            _syncedChildIds.value = _syncedChildIds.value + childId
            ApiResult.Success(allResults)
          }
        } catch (e: CancellationException) {
          throw e // preserve structured concurrency
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun createNap(childId: Int, request: CreateNapRequest): ApiResult<Nap> =
      withContext(ioDispatcher) {
        try {
          val nap = apiService.createNap(childId, request)
          val entity = NapEntity.fromApiModel(nap)
          napDao.upsertNap(entity)
          ApiResult.Success(nap)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun updateNap(childId: Int, napId: Int, request: UpdateNapRequest): ApiResult<Nap> =
      withContext(ioDispatcher) {
        try {
          val nap = apiService.updateNap(childId, napId, request)
          val entity = NapEntity.fromApiModel(nap)
          napDao.upsertNap(entity)
          ApiResult.Success(nap)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun deleteNap(childId: Int, napId: Int): ApiResult<Unit> =
      withContext(ioDispatcher) {
        try {
          apiService.deleteNap(childId, napId)
          napDao.deleteNap(napId)
          ApiResult.Success(Unit)
        } catch (e: Exception) {
          ApiResult.Error(e.toApiError())
        }
      }

  suspend fun clearChildCache(childId: Int) {
    withContext(ioDispatcher) { napDao.clearChildNaps(childId) }
    _syncedChildIds.value = _syncedChildIds.value - childId
  }
}
