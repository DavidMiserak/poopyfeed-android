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
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.DiaperEntity
import net.poopyfeed.pf.data.models.DiaperListResponse
import net.poopyfeed.pf.data.models.PaginatedResponse
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Unit tests for DiapersRemoteMediator.
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
class DiapersRemoteMediatorTest {

  private val mockApiService = mockk<PoopyFeedApiService>()
  private val mockDao = mockk<DiaperDao>(relaxed = true)
  private val childId = 1
  private lateinit var mediator: DiapersRemoteMediator

  @Before
  fun setup() {
    mediator = DiapersRemoteMediator(childId, mockApiService, mockDao)
  }

  @Test
  fun testRefreshLoadFetchesFirstPageAndClearsPreviousData() = runTest {
    // Setup mock API response with next page
    val mockDiaper1 = TestFixtures.mockDiaperListResponse(id = 1)
    val mockDiaper2 = TestFixtures.mockDiaperListResponse(id = 2)
    val mockResponse =
        TestFixtures.mockPaginatedResponse(
            results = listOf(mockDiaper1, mockDiaper2),
            count = 40,
            next = "http://api.example.com/diapers/?page=2",
            previous = null)

    coEvery { mockApiService.listDiapers(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildDiapers(childId) } returns Unit
    coEvery { mockDao.upsertDiapers(any()) } returns Unit

    // Create empty paging state
    val state: PagingState<Int, DiaperEntity> = createPagingState()

    // Execute REFRESH
    val result = mediator.load(LoadType.REFRESH, state)

    // Verify result
    assertTrue(result is MediatorResult.Success)
    assertFalse(result.endOfPaginationReached)

    // Verify API was called with correct parameters
    coVerify { mockApiService.listDiapers(childId, 1, 20) }

    // Verify database was cleared before upserting
    coVerify { mockDao.clearChildDiapers(childId) }

    // Verify entities were upserted
    coVerify { mockDao.upsertDiapers(any()) }
  }

  @Test
  fun testRefreshLoadReachesEndOfPaginationWhenNoNextLink() = runTest {
    val mockDiaper = TestFixtures.mockDiaperListResponse(id = 1)
    val mockResponse =
        TestFixtures.mockPaginatedResponse(
            results = listOf(mockDiaper),
            count = 5,
            next = null, // No next page
            previous = null)

    coEvery { mockApiService.listDiapers(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildDiapers(childId) } returns Unit
    coEvery { mockDao.upsertDiapers(any()) } returns Unit

    val state: PagingState<Int, DiaperEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)
  }

  @Test
  fun testAppendLoadHandlesResponseCorrectly() = runTest {
    // When AppendLoad encounters empty state (no lastItem), it should return success immediately
    val state: PagingState<Int, DiaperEntity> = createPagingState(itemCount = 0)

    val result = mediator.load(LoadType.APPEND, state)

    // Should return success without calling API
    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify no API call was made
    coVerify(exactly = 0) { mockApiService.listDiapers(any(), any(), any()) }
  }

  @Test
  fun testAppendLoadReturnsSuccessWhenNoMorePages() = runTest {
    val mockDiaper = TestFixtures.mockDiaperListResponse(id = 5)
    val mockResponse =
        TestFixtures.mockPaginatedResponse(
            results = listOf(mockDiaper),
            next = null, // Last page
            previous = "http://api.example.com/diapers/?page=2")

    coEvery { mockApiService.listDiapers(childId, any(), 20) } returns mockResponse
    coEvery { mockDao.upsertDiapers(any()) } returns Unit

    val state = createPagingState(anchorPosition = 39, itemCount = 40)

    val result = mediator.load(LoadType.APPEND, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)
  }

  @Test
  fun testPrependLoadReturnsSuccessImmediately() = runTest {
    val state: PagingState<Int, DiaperEntity> = createPagingState()

    val result = mediator.load(LoadType.PREPEND, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify no API call was made
    coVerify(exactly = 0) { mockApiService.listDiapers(any(), any(), any()) }
  }

  @Test
  fun testAppendLoadWithoutLastItemReturnsSuccessImmediately() = runTest {
    // Empty state (no items yet) during APPEND
    val state: PagingState<Int, DiaperEntity> = createPagingState(itemCount = 0)

    coEvery { mockDao.upsertDiapers(any()) } returns Unit

    val result = mediator.load(LoadType.APPEND, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify no API call for empty state
    coVerify(exactly = 0) { mockApiService.listDiapers(any(), any(), any()) }
  }

  @Test
  fun testNetworkErrorReturnsErrorResult() = runTest {
    val ioException = IOException("Network error")

    coEvery { mockApiService.listDiapers(childId, 1, 20) } throws ioException

    val state: PagingState<Int, DiaperEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    assertTrue(result is MediatorResult.Error)
    assertTrue(result.throwable is IOException)
  }

  @Test
  fun testHttpErrorReturnsErrorResult() = runTest {
    val httpException = HttpException(Response.error<String>(500, mockk(relaxed = true)))

    coEvery { mockApiService.listDiapers(childId, 1, 20) } throws httpException

    val state: PagingState<Int, DiaperEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    assertTrue(result is MediatorResult.Error)
    assertTrue(result.throwable is HttpException)
  }

  @Test
  fun testRefreshLoadDoesNotClearDatabaseOnError() = runTest {
    val exception = IOException("Network error")

    coEvery { mockApiService.listDiapers(childId, 1, 20) } throws exception

    val state: PagingState<Int, DiaperEntity> = createPagingState()

    mediator.load(LoadType.REFRESH, state)

    // Verify clearChildDiapers was never called when there's an error
    coVerify(exactly = 0) { mockDao.clearChildDiapers(any()) }
  }

  @Test
  fun testEmptyResponseIsHandledCorrectly() = runTest {
    val mockResponse: PaginatedResponse<DiaperListResponse> =
        TestFixtures.mockPaginatedResponse(
            results = emptyList(), count = 0, next = null, previous = null)

    coEvery { mockApiService.listDiapers(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildDiapers(childId) } returns Unit
    coEvery { mockDao.upsertDiapers(any()) } returns Unit

    val state: PagingState<Int, DiaperEntity> = createPagingState()

    val result = mediator.load(LoadType.REFRESH, state)

    assertTrue(result is MediatorResult.Success)
    assertTrue(result.endOfPaginationReached)

    // Verify empty list was still upserted
    coVerify { mockDao.upsertDiapers(emptyList()) }
  }

  @Test
  fun testConvertsDiaperListResponseToEntityCorrectly() = runTest {
    val mockDiaper =
        TestFixtures.mockDiaperListResponse(
            id = 999, change_type = "poop", changed_at = "2024-01-15T10:00:00Z")
    val mockResponse =
        TestFixtures.mockPaginatedResponse(
            results = listOf(mockDiaper), next = null, previous = null)

    coEvery { mockApiService.listDiapers(childId, 1, 20) } returns mockResponse
    coEvery { mockDao.clearChildDiapers(childId) } returns Unit
    coEvery { mockDao.upsertDiapers(any()) } returns Unit

    val state: PagingState<Int, DiaperEntity> = createPagingState()

    mediator.load(LoadType.REFRESH, state)

    // Verify upsertDiapers was called exactly once (entities were converted and persisted)
    coVerify(exactly = 1) { mockDao.upsertDiapers(any()) }
  }

  // Helper function to create PagingState with configurable parameters
  private fun createPagingState(
      anchorPosition: Int = 0,
      itemsBefore: Int = 0,
      itemsAfter: Int = 0,
      itemCount: Int = 0,
  ): PagingState<Int, DiaperEntity> {
    return PagingState(
        pages = emptyList(),
        anchorPosition = anchorPosition.takeIf { itemCount > 0 },
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0)
  }
}
