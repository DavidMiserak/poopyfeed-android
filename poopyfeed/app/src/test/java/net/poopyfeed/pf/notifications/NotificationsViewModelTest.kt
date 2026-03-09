package net.poopyfeed.pf.notifications

import android.content.Context
import androidx.paging.PagingData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    mockRepository = mockk(relaxed = true)
    every { mockContext.getString(any()) } returns "Error message"
    // Mock pagingData flow
    coEvery { mockRepository.pagedNotifications() } returns
        flowOf(
            PagingData.from(
                listOf(TestFixtures.mockNotification())
            )
        )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init exposes pagingData flow from repository`() =
      runTest(testDispatcher) {
        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        coVerify { mockRepository.pagedNotifications() }
        // Verify pagingData flow exists
        assertTrue(viewModel.pagingData != null)
      }

  @Test
  fun `markAllRead on success emits unreadCountInvalidated`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.markAllRead() } returns ApiResult.Success(5)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        val invalidations = mutableListOf<Unit>()
        val collectJob = this.launch { viewModel.unreadCountInvalidated.collect { invalidations.add(it) } }
        viewModel.markAllRead()
        advanceUntilIdle()
        collectJob.cancel()

        coVerify { mockRepository.markAllRead() }
        assertTrue(invalidations.size == 1)
      }

  @Test
  fun `markAllRead on error emits errorMessage`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.markAllRead() } returns
            ApiResult.Error(ApiError.NetworkError("Failed"))

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        val errors = mutableListOf<String>()
        val collectJob = this.launch { viewModel.errorMessage.collect { errors.add(it) } }
        viewModel.markAllRead()
        advanceUntilIdle()
        collectJob.cancel()

        coVerify { mockRepository.markAllRead() }
        assertTrue(errors.size == 1)
      }

  @Test
  fun `markAsReadAndNavigate on success emits navigateToChild and unreadCountInvalidated`() =
      runTest(testDispatcher) {
        val notif = TestFixtures.mockNotification(id = 10, childId = 42)
        coEvery { mockRepository.markAsRead(10) } returns ApiResult.Success(notif)

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        val navIds = mutableListOf<Int>()
        val navJob = this.launch { viewModel.navigateToChild.collect { navIds.add(it) } }
        val invalidations = mutableListOf<Unit>()
        val invalidJob = this.launch { viewModel.unreadCountInvalidated.collect { invalidations.add(it) } }

        viewModel.markAsReadAndNavigate(10, 42)
        advanceUntilIdle()

        navJob.cancel()
        invalidJob.cancel()

        coVerify { mockRepository.markAsRead(10) }
        assertTrue(navIds.size == 1 && navIds[0] == 42)
        assertTrue(invalidations.size == 1)
      }

  @Test
  fun `markAsReadAndNavigate when markAsRead returns Error emits errorMessage`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.markAsRead(10) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        val errors = mutableListOf<String>()
        val collectJob = this.launch { viewModel.errorMessage.collect { errors.add(it) } }
        viewModel.markAsReadAndNavigate(10, 42)
        advanceUntilIdle()
        collectJob.cancel()

        coVerify { mockRepository.markAsRead(10) }
        assertTrue(errors.size == 1)
      }

  @Test
  fun `markAllRead when API returns Loading does not emit error`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.markAllRead() } returns ApiResult.Loading()

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        val errors = mutableListOf<String>()
        val collectJob = this.launch { viewModel.errorMessage.collect { errors.add(it) } }
        viewModel.markAllRead()
        advanceUntilIdle()
        collectJob.cancel()

        assertTrue(errors.isEmpty())
      }

  @Test
  fun `markAsReadAndNavigate when markAsRead returns Loading does not navigate`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.markAsRead(10) } returns ApiResult.Loading()

        viewModel = NotificationsViewModel(mockRepository, mockContext)
        advanceUntilIdle()

        val navIds = mutableListOf<Int>()
        val collectJob = this.launch { viewModel.navigateToChild.collect { navIds.add(it) } }
        viewModel.markAsReadAndNavigate(10, 42)
        advanceUntilIdle()
        collectJob.cancel()

        assertTrue(navIds.isEmpty())
      }
}
