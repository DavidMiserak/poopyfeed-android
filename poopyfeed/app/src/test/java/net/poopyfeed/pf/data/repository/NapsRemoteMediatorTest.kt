package net.poopyfeed.pf.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator.MediatorResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.NapDao
import net.poopyfeed.pf.data.db.NapEntity
import net.poopyfeed.pf.data.db.RemoteKeyDao
import net.poopyfeed.pf.data.db.RemoteKeyEntity
import net.poopyfeed.pf.data.models.NapListResponse
import net.poopyfeed.pf.data.models.PaginatedResponse
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Unit tests for NapsRemoteMediator.
 *
 * Tests the RemoteMediator's ability to:
 * - Load initial data (REFRESH)
 * - Append next pages (APPEND)
 * - Handle prepend (PREPEND) as no-op
 * - Parse pagination tokens (next/previous)
 * - Handle network errors gracefully
 * - Clear database on refresh
 */
@OptIn(ExperimentalPagingApi::class)
class NapsRemoteMediatorTest {

  private val mockApiService = mockk<PoopyFeedApiService>()
  private val mockDao = mockk<NapDao>(relaxed = true)
  private val mockRemoteKeyDao = mockk<RemoteKeyDao>(relaxed = true)
  private val childId = 1
  private lateinit var mediator: NapsRemoteMediator

  @Before
  fun setup() {
    mediator = NapsRemoteMediator(childId, mockApiService, mockDao, mockRemoteKeyDao)
  }

  @Test
  fun testRefreshLoadFetchesFirstPageAndClearsPreviousData() = runTest {
    // Setup mock API response with next page
    val mockNap1 = TestFixtures.mockNapListResponse(id = 1)
    val mockNap2 = TestFixtures.mockNapListResponse(id = 2)
    val mockResponse =
        TestFixtures.mockPaginatedResponse(
            results = listOf(mockNap1, mockNap2),
            count = 40,
            next = "http://api.example.com/naps/?page=2",
            previous = null)

    coEvery { mockApiService.listNaps(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildNaps(childId) } returns Unit
    coEvery { mockDao.upsertNaps(any()) } returns Unit
    coEvery { mockRemoteKeyDao.clearKey(childId, "naps") } returns Unit
    coEvery { mockRemoteKeyDao.upsert(any()) } returns Unit

    // Create empty paging state
    val state: PagingState<Int, NapEntity> = createPagingState()

    // Execute REFRESH
    val result = mediator.load(LoadType.REFRESH, state)

    // Verify result
    assertTrue(result is MediatorResult.Success)
    assertFalse(result.endOfPaginationReached)

    // Verify API was called with correct parameters
    coVerify { mockApiService.listNaps(childId, 1, 20) }

    // Verify database was cleared before upserting
    coVerify { mockDao.clearChildNaps(childId) }

    // Verify entities were upserted
    coVerify { mockDao.upsertNaps(any()) }

    // Verify RemoteKey was cleared and next page was saved
    coVerify { mockRemoteKeyDao.clearKey(childId, "naps") }
    coVerify { mockRemoteKeyDao.upsert(RemoteKeyEntity(childId, "naps", 2)) }
  }

  @Test
  fun testRefreshLoadReachesEndOfPaginationWhenNoNextLink() = runTest {
    val mockNap = TestFixtures.mockNapListResponse(id = 1)
    val mockResponse =
        TestFixtures.mockPaginatedResponse(
            results = listOf(mockNap),
            count = 5,
            next = null, // No next page
            previous = null)

    coEvery { mockApiService.listNaps(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildNaps(childId) } returns Unit
    coEvery { mockDao.upsertNaps(any()) } returns Unit

    val state: PagingState<Int, NapEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)
  }

  @Test
  fun testAppendLoadsPageFromRemoteKey() = runTest {
    val mockNap = TestFixtures.mockNapListResponse(id = 5)
    val mockResponse =
        TestFixtures.mockPaginatedResponse(
            results = listOf(mockNap),
            count = 40,
            next = "http://api.example.com/naps/?page=3",
            previous = "http://api.example.com/naps/?page=1")

    coEvery { mockApiService.listNaps(childId, 2, 20) } returns mockResponse
    coEvery { mockDao.upsertNaps(any()) } returns Unit
    coEvery { mockRemoteKeyDao.getKey(childId, "naps") } returns RemoteKeyEntity(childId, "naps", 2)
    coEvery { mockRemoteKeyDao.upsert(any()) } returns Unit

    val state = createPagingState(anchorPosition = 19, itemCount = 20)

    val result = mediator.load(LoadType.APPEND, state)

    assertTrue(result is MediatorResult.Success)
    assertFalse(result.endOfPaginationReached)

    // Verify API was called with page 2 from RemoteKey
    coVerify { mockApiService.listNaps(childId, 2, 20) }

    // Verify next page was saved (2 + 1 = 3)
    coVerify { mockRemoteKeyDao.upsert(RemoteKeyEntity(childId, "naps", 3)) }
  }

  @Test
  fun testAppendWithNullNextPageReturnsEndOfPagination() = runTest {
    coEvery { mockRemoteKeyDao.getKey(childId, "naps") } returns
        RemoteKeyEntity(childId, "naps", null)

    val state = createPagingState(anchorPosition = 39, itemCount = 40)

    val result = mediator.load(LoadType.APPEND, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify no API call was made
    coVerify(exactly = 0) { mockApiService.listNaps(any(), any(), any()) }
  }

  @Test
  fun testAppendLoadHandlesResponseCorrectly() = runTest {
    // When AppendLoad encounters no RemoteKey, it should return success immediately
    coEvery { mockRemoteKeyDao.getKey(childId, "naps") } returns null

    val state: PagingState<Int, NapEntity> = createPagingState(itemCount = 0)

    val result = mediator.load(LoadType.APPEND, state)

    // Should return success without calling API
    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify no API call was made
    coVerify(exactly = 0) { mockApiService.listNaps(any(), any(), any()) }
  }

  @Test
  fun testAppendLoadReturnsSuccessWhenNoMorePages() = runTest {
    val mockNap = TestFixtures.mockNapListResponse(id = 5)
    val mockResponse =
        TestFixtures.mockPaginatedResponse(
            results = listOf(mockNap),
            next = null, // Last page
            previous = "http://api.example.com/naps/?page=2")

    coEvery { mockApiService.listNaps(childId, any(), 20) } returns mockResponse
    coEvery { mockDao.upsertNaps(any()) } returns Unit

    val state = createPagingState(anchorPosition = 39, itemCount = 40)

    val result = mediator.load(LoadType.APPEND, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)
  }

  @Test
  fun testPrependLoadReturnsSuccessImmediately() = runTest {
    val state: PagingState<Int, NapEntity> = createPagingState()

    val result = mediator.load(LoadType.PREPEND, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify no API call was made
    coVerify(exactly = 0) { mockApiService.listNaps(any(), any(), any()) }
  }

  @Test
  fun testAppendLoadWithoutLastItemReturnsSuccessImmediately() = runTest {
    // Empty state (no items yet) during APPEND
    val state: PagingState<Int, NapEntity> = createPagingState(itemCount = 0)

    coEvery { mockRemoteKeyDao.getKey(childId, "naps") } returns null
    coEvery { mockDao.upsertNaps(any()) } returns Unit

    val result = mediator.load(LoadType.APPEND, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify no API call for empty state
    coVerify(exactly = 0) { mockApiService.listNaps(any(), any(), any()) }
  }

  @Test
  fun testNetworkErrorReturnsErrorResult() = runTest {
    val ioException = IOException("Network error")

    coEvery { mockApiService.listNaps(childId, 1, 20) } throws ioException

    val state: PagingState<Int, NapEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    assertTrue(result is MediatorResult.Error)
    assertTrue(result.throwable is IOException)
  }

  @Test
  fun testHttpErrorReturnsErrorResult() = runTest {
    val httpException = HttpException(Response.error<String>(500, mockk(relaxed = true)))

    coEvery { mockApiService.listNaps(childId, 1, 20) } throws httpException

    val state: PagingState<Int, NapEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    assertTrue(result is MediatorResult.Error)
    assertTrue(result.throwable is HttpException)
  }

  @Test
  fun testRefreshLoadDoesNotClearDatabaseOnError() = runTest {
    val exception = IOException("Network error")

    coEvery { mockApiService.listNaps(childId, 1, 20) } throws exception

    val state: PagingState<Int, NapEntity> = createPagingState()

    mediator.load(LoadType.REFRESH, state)

    // Verify clearChildNaps was never called when there's an error
    coVerify(exactly = 0) { mockDao.clearChildNaps(any()) }
  }

  @Test
  fun testEmptyResponseIsHandledCorrectly() = runTest {
    val mockResponse: PaginatedResponse<NapListResponse> =
        TestFixtures.mockPaginatedResponse(
            results = emptyList(), count = 0, next = null, previous = null)

    coEvery { mockApiService.listNaps(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildNaps(childId) } returns Unit
    coEvery { mockDao.upsertNaps(any()) } returns Unit

    val state: PagingState<Int, NapEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify empty list was still upserted
    coVerify { mockDao.upsertNaps(emptyList()) }
  }

  @Test
  fun testConvertsNapListResponseToEntityCorrectly() = runTest {
    val mockNap =
        TestFixtures.mockNapListResponse(
            id = 999, napped_at = "2024-01-15T10:00:00Z", ended_at = "2024-01-15T11:00:00Z")
    val mockResponse =
        TestFixtures.mockPaginatedResponse(results = listOf(mockNap), next = null, previous = null)

    coEvery { mockApiService.listNaps(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildNaps(childId) } returns Unit
    coEvery { mockDao.upsertNaps(any()) } returns Unit

    val state: PagingState<Int, NapEntity> = createPagingState()

    mediator.load(LoadType.REFRESH, state)

    // Verify upsertNaps was called exactly once (entities were converted and persisted)
    coVerify(exactly = 1) { mockDao.upsertNaps(any()) }
  }

  // Helper function to create PagingState with configurable parameters
  private fun createPagingState(
      anchorPosition: Int = 0,
      itemsBefore: Int = 0,
      itemsAfter: Int = 0,
      itemCount: Int = 0,
  ): PagingState<Int, NapEntity> {
    return PagingState(
        pages = emptyList(),
        anchorPosition = anchorPosition.takeIf { itemCount > 0 },
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0)
  }
}
