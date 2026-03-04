package net.poopyfeed.pf.data.repository

import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.DiaperEntity
import net.poopyfeed.pf.data.db.FeedingDao
import net.poopyfeed.pf.data.db.FeedingEntity
import net.poopyfeed.pf.data.db.NapDao
import net.poopyfeed.pf.data.db.NapEntity
import net.poopyfeed.pf.data.models.*
import org.junit.Test

class CachedTrackingRepositoriesTest {

  // --- CachedFeedingsRepository ---

  @Test
  fun `CachedFeedingsRepository listFeedingsCached emits Success with empty list when empty`() =
      runTest {
        val feedingDao = io.mockk.mockk<FeedingDao>()
        io.mockk.every { feedingDao.getFeedingsFlow(1) } returns flowOf(emptyList())
        val apiService = io.mockk.mockk<PoopyFeedApiService>()
        val repo = CachedFeedingsRepository(apiService, feedingDao)

        val results = repo.listFeedingsCached(1).toList()

        assertIs<ApiResult.Success<List<Feeding>>>(results[0])
        assertEquals(0, (results[0] as ApiResult.Success).data.size)
      }

  @Test
  fun `CachedFeedingsRepository listFeedingsCached emits Success when has data`() = runTest {
    val feedingDao = io.mockk.mockk<FeedingDao>()
    val entity =
        FeedingEntity(
            id = 1,
            child = 1,
            feeding_type = "bottle",
            amount_oz = 4.0,
            timestamp = "2024-01-15T12:00:00Z",
            created_at = "2024-01-15T12:00:00Z",
            updated_at = "2024-01-15T12:00:00Z")
    io.mockk.every { feedingDao.getFeedingsFlow(1) } returns flowOf(listOf(entity))
    val apiService = io.mockk.mockk<PoopyFeedApiService>()
    val repo = CachedFeedingsRepository(apiService, feedingDao)

    val results = repo.listFeedingsCached(1).toList()

    assertIs<ApiResult.Success<List<Feeding>>>(results[0])
    assertEquals("bottle", (results[0] as ApiResult.Success).data.first().feeding_type)
  }

  @Test
  fun `CachedFeedingsRepository refreshFeedings success`() = runTest {
    val feedingDao = io.mockk.mockk<FeedingDao>()
    io.mockk.coEvery { feedingDao.upsertFeedings(any()) } returns Unit
    val apiService = io.mockk.mockk<PoopyFeedApiService>()
    val feeding =
        Feeding(
            id = 1,
            child = 1,
            feeding_type = "bottle",
            amount_oz = 4.0,
            timestamp = "2024-01-15T12:00:00Z",
            created_at = "2024-01-15T12:00:00Z",
            updated_at = "2024-01-15T12:00:00Z")
    io.mockk.coEvery { apiService.listFeedings(1, 1) } returns
        PaginatedResponse(1, results = listOf(feeding))
    val repo = CachedFeedingsRepository(apiService, feedingDao)

    val result = repo.refreshFeedings(1)

    assertIs<ApiResult.Success<List<Feeding>>>(result)
    assertEquals(1, result.data.size)
  }

  @Test
  fun `CachedFeedingsRepository createFeeding and deleteFeeding`() = runTest {
    val feedingDao = io.mockk.mockk<FeedingDao>()
    io.mockk.coEvery { feedingDao.upsertFeeding(any()) } returns Unit
    io.mockk.coEvery { feedingDao.deleteFeeding(1) } returns Unit
    val apiService = io.mockk.mockk<PoopyFeedApiService>()
    val feeding =
        Feeding(
            id = 1,
            child = 1,
            feeding_type = "bottle",
            amount_oz = 4.0,
            timestamp = "2024-01-15T12:00:00Z",
            created_at = "2024-01-15T12:00:00Z",
            updated_at = "2024-01-15T12:00:00Z")
    val request = CreateFeedingRequest("bottle", 4.0, "2024-01-15T12:00:00Z")
    io.mockk.coEvery { apiService.createFeeding(1, request) } returns feeding
    io.mockk.coEvery { apiService.deleteFeeding(1, 1) } returns Unit
    val repo = CachedFeedingsRepository(apiService, feedingDao)

    val createResult = repo.createFeeding(1, request)
    assertIs<ApiResult.Success<Feeding>>(createResult)

    val deleteResult = repo.deleteFeeding(1, 1)
    assertIs<ApiResult.Success<Unit>>(deleteResult)
  }

  @Test
  fun `CachedFeedingsRepository refreshFeedings network error`() = runTest {
    val feedingDao = io.mockk.mockk<FeedingDao>()
    val apiService = io.mockk.mockk<PoopyFeedApiService>()
    io.mockk.coEvery { apiService.listFeedings(1, 1) } throws IOException("Network down")
    val repo = CachedFeedingsRepository(apiService, feedingDao)

    val result = repo.refreshFeedings(1)

    assertIs<ApiResult.Error<List<Feeding>>>(result)
  }

  // --- CachedDiapersRepository ---

  @Test
  fun `CachedDiapersRepository listDiapersCached and refreshDiapers`() = runTest {
    val diaperDao = io.mockk.mockk<DiaperDao>()
    val entity =
        DiaperEntity(
            id = 1,
            child = 1,
            change_type = "wet",
            timestamp = "2024-01-15T14:00:00Z",
            created_at = "2024-01-15T14:00:00Z",
            updated_at = "2024-01-15T14:00:00Z")
    io.mockk.every { diaperDao.getDiapersFlow(1) } returns flowOf(listOf(entity))
    io.mockk.coEvery { diaperDao.upsertDiapers(any()) } returns Unit
    val apiService = io.mockk.mockk<PoopyFeedApiService>()
    val diaper =
        Diaper(
            id = 1,
            child = 1,
            change_type = "wet",
            timestamp = "2024-01-15T14:00:00Z",
            created_at = "2024-01-15T14:00:00Z",
            updated_at = "2024-01-15T14:00:00Z")
    io.mockk.coEvery { apiService.listDiapers(1, 1) } returns
        PaginatedResponse(1, results = listOf(diaper))
    val repo = CachedDiapersRepository(apiService, diaperDao)

    val listResults = repo.listDiapersCached(1).toList()
    assertIs<ApiResult.Success<List<Diaper>>>(listResults[0])

    val refreshResult = repo.refreshDiapers(1)
    assertIs<ApiResult.Success<List<Diaper>>>(refreshResult)
  }

  @Test
  fun `CachedDiapersRepository createDiaper deleteDiaper`() = runTest {
    val diaperDao = io.mockk.mockk<DiaperDao>()
    io.mockk.coEvery { diaperDao.upsertDiaper(any()) } returns Unit
    io.mockk.coEvery { diaperDao.deleteDiaper(1) } returns Unit
    val apiService = io.mockk.mockk<PoopyFeedApiService>()
    val diaper =
        Diaper(
            id = 1,
            child = 1,
            change_type = "dirty",
            timestamp = "2024-01-15T14:00:00Z",
            created_at = "2024-01-15T14:00:00Z",
            updated_at = "2024-01-15T14:00:00Z")
    val request = CreateDiaperRequest("dirty", "2024-01-15T14:00:00Z")
    io.mockk.coEvery { apiService.createDiaper(1, request) } returns diaper
    io.mockk.coEvery { apiService.deleteDiaper(1, 1) } returns Unit
    val repo = CachedDiapersRepository(apiService, diaperDao)

    assertIs<ApiResult.Success<Diaper>>(repo.createDiaper(1, request))
    assertIs<ApiResult.Success<Unit>>(repo.deleteDiaper(1, 1))
  }

  // --- CachedNapsRepository ---

  @Test
  fun `CachedNapsRepository listNapsCached and refreshNaps`() = runTest {
    val napDao = io.mockk.mockk<NapDao>()
    val entity =
        NapEntity(
            id = 1,
            child = 1,
            start_time = "2024-01-15T13:00:00Z",
            end_time = null,
            created_at = "2024-01-15T13:00:00Z",
            updated_at = "2024-01-15T13:00:00Z")
    io.mockk.every { napDao.getNapsFlow(1) } returns flowOf(listOf(entity))
    io.mockk.coEvery { napDao.upsertNaps(any()) } returns Unit
    val apiService = io.mockk.mockk<PoopyFeedApiService>()
    val nap =
        Nap(
            id = 1,
            child = 1,
            start_time = "2024-01-15T13:00:00Z",
            end_time = null,
            created_at = "2024-01-15T13:00:00Z",
            updated_at = "2024-01-15T13:00:00Z")
    io.mockk.coEvery { apiService.listNaps(1, 1) } returns
        PaginatedResponse(1, results = listOf(nap))
    val repo = CachedNapsRepository(apiService, napDao)

    val listResults = repo.listNapsCached(1).toList()
    assertIs<ApiResult.Success<List<Nap>>>(listResults[0])
    val refreshResult = repo.refreshNaps(1)
    assertIs<ApiResult.Success<List<Nap>>>(refreshResult)
  }

  @Test
  fun `CachedNapsRepository createNap updateNap deleteNap`() = runTest {
    val napDao = io.mockk.mockk<NapDao>()
    io.mockk.coEvery { napDao.upsertNap(any()) } returns Unit
    io.mockk.coEvery { napDao.deleteNap(1) } returns Unit
    val apiService = io.mockk.mockk<PoopyFeedApiService>()
    val nap =
        Nap(
            id = 1,
            child = 1,
            start_time = "2024-01-15T13:00:00Z",
            end_time = "2024-01-15T14:00:00Z",
            created_at = "2024-01-15T13:00:00Z",
            updated_at = "2024-01-15T14:00:00Z")
    val createRequest = CreateNapRequest("2024-01-15T13:00:00Z", null)
    val updateRequest = UpdateNapRequest("2024-01-15T14:00:00Z")
    io.mockk.coEvery { apiService.createNap(1, createRequest) } returns nap
    io.mockk.coEvery { apiService.updateNap(1, 1, updateRequest) } returns nap
    io.mockk.coEvery { apiService.deleteNap(1, 1) } returns Unit
    val repo = CachedNapsRepository(apiService, napDao)

    assertIs<ApiResult.Success<Nap>>(repo.createNap(1, createRequest))
    assertIs<ApiResult.Success<Nap>>(repo.updateNap(1, 1, updateRequest))
    assertIs<ApiResult.Success<Unit>>(repo.deleteNap(1, 1))
  }
}
