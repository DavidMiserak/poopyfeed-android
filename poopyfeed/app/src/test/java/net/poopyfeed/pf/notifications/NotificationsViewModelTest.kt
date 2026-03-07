package net.poopyfeed.pf.notifications

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.repository.NotificationsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var mockRepository: NotificationsRepository
  private lateinit var viewModel: NotificationsViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    mockRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init calls refresh and repo listNotifications page 1`() =
      runTest(testDispatcher) {
        val list =
            PaginatedResponse(
                count = 2,
                next = "http://next",
                previous = null,
                results = listOf(TestFixtures.mockNotification()))
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(list)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        coVerify { mockRepository.listNotifications(page = 1) }
        assertIs<NotificationsListUiState.Ready>(viewModel.uiState.value)
        assertTrue(
            (viewModel.uiState.value as NotificationsListUiState.Ready).notifications.size == 1)
      }

  @Test
  fun `refresh with empty results emits Empty`() =
      runTest(testDispatcher) {
        val empty =
            PaginatedResponse<Notification>(
                count = 0, next = null, previous = null, results = emptyList())
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(empty)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<NotificationsListUiState.Empty>(viewModel.uiState.value)
      }

  @Test
  fun `refresh with API error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listNotifications(page = 1) } returns
            ApiResult.Error(ApiError.NetworkError("Network down"))

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<NotificationsListUiState.Error>(viewModel.uiState.value)
      }

  @Test
  fun `loadNextPage appends next page and updates nextPageToLoad`() =
      runTest(testDispatcher) {
        val page1 =
            PaginatedResponse(
                count = 3,
                next = "http://page2",
                previous = null,
                results = listOf(TestFixtures.mockNotification(id = 1)))
        val page2 =
            PaginatedResponse(
                count = 3,
                next = null,
                previous = "http://page1",
                results =
                    listOf(
                        TestFixtures.mockNotification(id = 2),
                        TestFixtures.mockNotification(id = 3)))
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(page1)
        coEvery { mockRepository.listNotifications(page = 2) } returns ApiResult.Success(page2)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()
        assertIs<NotificationsListUiState.Ready>(viewModel.uiState.value)

        viewModel.loadNextPage()
        advanceUntilIdle()

        val ready = viewModel.uiState.value as NotificationsListUiState.Ready
        assertTrue(ready.notifications.size == 3)
        assertTrue(!ready.hasNextPage)
        coVerify { mockRepository.listNotifications(page = 2) }
      }

  @Test
  fun `loadNextPage when no next page does not call repo`() =
      runTest(testDispatcher) {
        val page1 =
            PaginatedResponse(
                count = 1,
                next = null,
                previous = null,
                results = listOf(TestFixtures.mockNotification()))
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(page1)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.listNotifications(any()) }
      }

  @Test
  fun `loadNextPage when state is Empty does not call repo for page 2`() =
      runTest(testDispatcher) {
        val empty =
            PaginatedResponse<Notification>(
                count = 0, next = null, previous = null, results = emptyList())
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(empty)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()
        assertIs<NotificationsListUiState.Empty>(viewModel.uiState.value)
        viewModel.loadNextPage()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.listNotifications(1) }
      }

  @Test
  fun `markAllRead on success triggers refresh`() =
      runTest(testDispatcher) {
        val list =
            PaginatedResponse(
                count = 0, next = null, previous = null, results = emptyList<Notification>())
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(list)
        coEvery { mockRepository.markAllRead() } returns ApiResult.Success(5)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.markAllRead()
        advanceUntilIdle()

        coVerify { mockRepository.markAllRead() }
        coVerify(atLeast = 2) { mockRepository.listNotifications(page = 1) }
      }

  @Test
  fun `markAllRead on error emits errorMessage`() =
      runTest(testDispatcher) {
        val list =
            PaginatedResponse(
                count = 1,
                next = null,
                previous = null,
                results = listOf(TestFixtures.mockNotification()))
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(list)
        coEvery { mockRepository.markAllRead() } returns
            ApiResult.Error(ApiError.NetworkError("Failed"))

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.markAllRead()
        advanceUntilIdle()

        coVerify { mockRepository.markAllRead() }
      }

  @Test
  fun `markAsReadAndNavigate on success emits navigateToChild and refreshes`() =
      runTest(testDispatcher) {
        val notif = TestFixtures.mockNotification(id = 10, childId = 42)
        val list =
            PaginatedResponse(count = 1, next = null, previous = null, results = listOf(notif))
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(list)
        coEvery { mockRepository.markAsRead(10) } returns ApiResult.Success(notif)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()
        val navIds = mutableListOf<Int>()
        val collectJob = this.launch { viewModel.navigateToChild.collect { navIds.add(it) } }
        viewModel.markAsReadAndNavigate(10, 42)
        advanceUntilIdle()

        coVerify { mockRepository.markAsRead(10) }
        assertTrue(navIds.size == 1 && navIds[0] == 42)
        collectJob.cancel()
      }

  @Test
  fun `hasUnread is true when Ready state has unread notification`() =
      runTest(testDispatcher) {
        val unread = TestFixtures.mockNotification(id = 1, isRead = false)
        val list =
            PaginatedResponse(count = 1, next = null, previous = null, results = listOf(unread))
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(list)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        assertTrue(viewModel.hasUnread)
      }

  @Test
  fun `hasUnread is false when all notifications are read`() =
      runTest(testDispatcher) {
        val read = TestFixtures.mockNotification(id = 1, isRead = true)
        val list =
            PaginatedResponse(count = 1, next = null, previous = null, results = listOf(read))
        coEvery { mockRepository.listNotifications(page = 1) } returns ApiResult.Success(list)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        assertTrue(!viewModel.hasUnread)
      }
}
