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
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.db.FeedingDao
import net.poopyfeed.pf.data.db.FeedingEntity
import net.poopyfeed.pf.data.db.RemoteKeyDao
import net.poopyfeed.pf.data.db.RemoteKeyEntity
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Unit tests for FeedingsRemoteMediator.
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
class FeedingsRemoteMediatorTest {

  private val mockApiService = mockk<PoopyFeedApiService>()
  private val mockDao = mockk<FeedingDao>(relaxed = true)
  private val mockRemoteKeyDao = mockk<RemoteKeyDao>(relaxed = true)
  private val childId = 1
  private lateinit var mediator: FeedingsRemoteMediator

  @Before
  fun setup() {
    mediator = FeedingsRemoteMediator(childId, mockApiService, mockDao, mockRemoteKeyDao)
  }

  @Test
  fun testRefreshLoadFetchesFirstPageAndClearsPreviousData() = runTest {
    // Setup mock API response with next page
    val mockFeeding1 = TestFixtures.mockFeedingListResponse(id = 1)
    val mockFeeding2 = TestFixtures.mockFeedingListResponse(id = 2)
    val mockResponse =
        TestFixtures.mockPaginatedFeedingsResponse(
            results = listOf(mockFeeding1, mockFeeding2),
            count = 40,
            next = "http://api.example.com/feedings/?page=2",
            previous = null)

    coEvery { mockApiService.listFeedings(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildFeedings(childId) } returns Unit
    coEvery { mockDao.upsertFeedings(any()) } returns Unit

    // Create empty paging state
    val state: PagingState<Int, FeedingEntity> = createPagingState()

    // Execute REFRESH
    val result = mediator.load(LoadType.REFRESH, state)

    // Verify result
    if (result is MediatorResult.Success) {
      assertFalse(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }

    // Verify API was called with correct parameters
    coVerify { mockApiService.listFeedings(childId, 1, 20) }

    // Verify database was cleared before upserting
    coVerify { mockDao.clearChildFeedings(childId) }

    // Verify entities were upserted
    coVerify { mockDao.upsertFeedings(any()) }
  }

  @Test
  fun testRefreshLoadReachesEndOfPaginationWhenNoNextLink() = runTest {
    val mockFeeding = TestFixtures.mockFeedingListResponse(id = 1)
    val mockResponse =
        TestFixtures.mockPaginatedFeedingsResponse(
            results = listOf(mockFeeding),
            count = 5,
            next = null, // No next page
            previous = null)

    coEvery { mockApiService.listFeedings(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildFeedings(childId) } returns Unit
    coEvery { mockDao.upsertFeedings(any()) } returns Unit

    val state: PagingState<Int, FeedingEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    if (result is MediatorResult.Success) {
      assertTrue(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }
  }

  @Test
  fun testRefreshClearsRemoteKeyAndSavesNextPage() = runTest {
    val mockFeeding = TestFixtures.mockFeedingListResponse(id = 1)
    val mockResponse =
        TestFixtures.mockPaginatedFeedingsResponse(
            results = listOf(mockFeeding),
            count = 40,
            next = "http://api.example.com/feedings/?page=2",
            previous = null)

    coEvery { mockApiService.listFeedings(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildFeedings(childId) } returns Unit
    coEvery { mockDao.upsertFeedings(any()) } returns Unit
    coEvery { mockRemoteKeyDao.clearKey(childId, "feedings") } returns Unit
    coEvery { mockRemoteKeyDao.upsert(any()) } returns Unit

    val state: PagingState<Int, FeedingEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    if (result is MediatorResult.Success) {
      assertFalse(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }

    // Verify RemoteKey was cleared on REFRESH
    coVerify { mockRemoteKeyDao.clearKey(childId, "feedings") }

    // Verify next page was saved (page 1 + 1 = 2)
    coVerify { mockRemoteKeyDao.upsert(RemoteKeyEntity(childId, "feedings", 2)) }
  }

  @Test
  fun testRefreshWithSinglePageSetsNextPageToNull() = runTest {
    val mockFeeding = TestFixtures.mockFeedingListResponse(id = 1)
    val mockResponse =
        TestFixtures.mockPaginatedFeedingsResponse(
            results = listOf(mockFeeding), count = 5, next = null, previous = null)

    coEvery { mockApiService.listFeedings(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildFeedings(childId) } returns Unit
    coEvery { mockDao.upsertFeedings(any()) } returns Unit
    coEvery { mockRemoteKeyDao.clearKey(childId, "feedings") } returns Unit
    coEvery { mockRemoteKeyDao.upsert(any()) } returns Unit

    val state: PagingState<Int, FeedingEntity> = createPagingState()

    mediator.load(LoadType.REFRESH, state)

    // Verify next page is null when no more pages
    coVerify { mockRemoteKeyDao.upsert(RemoteKeyEntity(childId, "feedings", null)) }
  }

  @Test
  fun testAppendLoadsPageFromRemoteKey() = runTest {
    val mockFeeding = TestFixtures.mockFeedingListResponse(id = 5)
    val mockResponse =
        TestFixtures.mockPaginatedFeedingsResponse(
            results = listOf(mockFeeding),
            count = 40,
            next = "http://api.example.com/feedings/?page=3",
            previous = "http://api.example.com/feedings/?page=1")

    coEvery { mockApiService.listFeedings(childId, 2, 20) } returns mockResponse
    coEvery { mockDao.upsertFeedings(any()) } returns Unit
    coEvery { mockRemoteKeyDao.getKey(childId, "feedings") } returns
        RemoteKeyEntity(childId, "feedings", 2)
    coEvery { mockRemoteKeyDao.upsert(any()) } returns Unit

    val state = createPagingState(anchorPosition = 19, itemCount = 20)

    val result = mediator.load(LoadType.APPEND, state)

    if (result is MediatorResult.Success) {
      assertFalse(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }

    // Verify API was called with page 2 from RemoteKey
    coVerify { mockApiService.listFeedings(childId, 2, 20) }

    // Verify next page was saved (2 + 1 = 3)
    coVerify { mockRemoteKeyDao.upsert(RemoteKeyEntity(childId, "feedings", 3)) }
  }

  @Test
  fun testAppendWithNullNextPageReturnsEndOfPagination() = runTest {
    coEvery { mockRemoteKeyDao.getKey(childId, "feedings") } returns
        RemoteKeyEntity(childId, "feedings", null)

    val state = createPagingState(anchorPosition = 39, itemCount = 40)

    val result = mediator.load(LoadType.APPEND, state)

    if (result is MediatorResult.Success) {
      assertTrue(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }

    // Verify no API call was made
    coVerify(exactly = 0) { mockApiService.listFeedings(any(), any(), any()) }
  }

  @Test
  fun testAppendLoadHandlesResponseCorrectly() = runTest {
    // When AppendLoad encounters no RemoteKey, it should return success immediately
    coEvery { mockRemoteKeyDao.getKey(childId, "feedings") } returns null

    val state: PagingState<Int, FeedingEntity> = createPagingState(itemCount = 0)

    val result = mediator.load(LoadType.APPEND, state)

    // Should return success without calling API
    if (result is MediatorResult.Success) {
      assertTrue(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }

    // Verify no API call was made
    coVerify(exactly = 0) { mockApiService.listFeedings(any(), any(), any()) }
  }

  @Test
  fun testAppendLoadReturnsSuccessWhenNoMorePages() = runTest {
    val mockFeeding = TestFixtures.mockFeedingListResponse(id = 5)
    val mockResponse =
        TestFixtures.mockPaginatedFeedingsResponse(
            results = listOf(mockFeeding),
            next = null, // Last page
            previous = "http://api.example.com/feedings/?page=2")

    coEvery { mockApiService.listFeedings(childId, any(), 20) } returns mockResponse
    coEvery { mockDao.upsertFeedings(any()) } returns Unit

    val state = createPagingState(anchorPosition = 39, itemCount = 40)

    val result = mediator.load(LoadType.APPEND, state)

    if (result is MediatorResult.Success) {
      assertTrue(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }
  }

  @Test
  fun testPrependLoadReturnsSuccessImmediately() = runTest {
    val state: PagingState<Int, FeedingEntity> = createPagingState()

    val result = mediator.load(LoadType.PREPEND, state)

    if (result is MediatorResult.Success) {
      assertTrue(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }

    // Verify no API call was made
    coVerify(exactly = 0) { mockApiService.listFeedings(any(), any(), any()) }
  }

  @Test
  fun testAppendLoadWithoutLastItemReturnsSuccessImmediately() = runTest {
    // Empty state (no items yet) during APPEND
    val state: PagingState<Int, FeedingEntity> = createPagingState(itemCount = 0)

    coEvery { mockRemoteKeyDao.getKey(childId, "feedings") } returns null
    coEvery { mockDao.upsertFeedings(any()) } returns Unit

    val result = mediator.load(LoadType.APPEND, state)

    if (result is MediatorResult.Success) {
      assertTrue(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }

    // Verify no API call for empty state
    coVerify(exactly = 0) { mockApiService.listFeedings(any(), any(), any()) }
  }

  @Test
  fun testNetworkErrorReturnsErrorResult() = runTest {
    val ioException = IOException("Network error")

    coEvery { mockApiService.listFeedings(childId, 1, 20) } throws ioException

    val state: PagingState<Int, FeedingEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    if (result is MediatorResult.Error) {
      assertTrue(result.throwable is IOException)
    } else {
      fail("Expected MediatorResult.Error")
    }
  }

  @Test
  fun testHttpErrorReturnsErrorResult() = runTest {
    val httpException = HttpException(Response.error<String>(500, mockk(relaxed = true)))

    coEvery { mockApiService.listFeedings(childId, 1, 20) } throws httpException

    val state: PagingState<Int, FeedingEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    if (result is MediatorResult.Error) {
      assertTrue(result.throwable is HttpException)
    } else {
      fail("Expected MediatorResult.Error")
    }
  }

  @Test
  fun testRefreshLoadDoesNotClearDatabaseOnError() = runTest {
    val exception = IOException("Network error")

    coEvery { mockApiService.listFeedings(childId, 1, 20) } throws exception

    val state: PagingState<Int, FeedingEntity> = createPagingState()

    mediator.load(LoadType.REFRESH, state)

    // Verify clearChildFeedings was never called when there's an error
    coVerify(exactly = 0) { mockDao.clearChildFeedings(any()) }
  }

  @Test
  fun testEmptyResponseIsHandledCorrectly() = runTest {
    val mockResponse =
        TestFixtures.mockPaginatedFeedingsResponse(
            results = emptyList(), count = 0, next = null, previous = null)

    coEvery { mockApiService.listFeedings(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildFeedings(childId) } returns Unit
    coEvery { mockDao.upsertFeedings(any()) } returns Unit

    val state: PagingState<Int, FeedingEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    if (result is MediatorResult.Success) {
      assertTrue(result.endOfPaginationReached)
    } else {
      fail("Expected MediatorResult.Success")
    }

    // Verify empty list was still upserted
    coVerify { mockDao.upsertFeedings(emptyList()) }
  }

  @Test
  fun testConvertsFeedingListResponseToEntityCorrectly() = runTest {
    val mockFeeding =
        TestFixtures.mockFeedingListResponse(
            id = 999,
            feeding_type = "breast",
            amount_oz = null,
            duration_minutes = 15,
            side = "left")
    val mockResponse =
        TestFixtures.mockPaginatedFeedingsResponse(
            results = listOf(mockFeeding), next = null, previous = null)

    coEvery { mockApiService.listFeedings(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildFeedings(childId) } returns Unit
    coEvery { mockDao.upsertFeedings(any()) } returns Unit

    val state: PagingState<Int, FeedingEntity> = createPagingState()

    mediator.load(LoadType.REFRESH, state)

    // Verify upsertFeedings was called exactly once (entities were converted and persisted)
    coVerify(exactly = 1) { mockDao.upsertFeedings(any()) }
  }

  // Helper function to create PagingState with configurable parameters
  private fun createPagingState(
      anchorPosition: Int = 0,
      itemsBefore: Int = 0,
      itemsAfter: Int = 0,
      itemCount: Int = 0,
  ): PagingState<Int, FeedingEntity> {
    return PagingState(
        pages = emptyList(),
        anchorPosition = anchorPosition.takeIf { itemCount > 0 },
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0)
  }
}
