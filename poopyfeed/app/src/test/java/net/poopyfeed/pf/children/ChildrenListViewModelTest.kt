package net.poopyfeed.pf.children

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.ui.toast.ToastManager
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Unit tests for ChildrenListViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChildrenListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var mockRepository: CachedChildrenRepository
  private lateinit var mockAnalyticsTracker: net.poopyfeed.pf.analytics.AnalyticsTracker
  private lateinit var mockToastManager: ToastManager
  private lateinit var viewModel: ChildrenListViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    mockRepository = mockk()
    mockAnalyticsTracker = mockk()
    mockToastManager = mockk()
    every { mockContext.getString(any()) } returns "Error message"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init calls refresh on start`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listChildrenCached() } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow } returns flowOf(true)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        every { mockToastManager.showSuccess(any()) } returns Unit

        viewModel =
            ChildrenListViewModel(
                mockRepository, mockContext, mockAnalyticsTracker, mockToastManager)
        advanceUntilIdle()

        coVerify { mockRepository.refreshChildren() }
      }

  @Test
  fun `refresh calls repository refresh`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listChildrenCached() } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow } returns flowOf(true)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        every { mockToastManager.showSuccess(any()) } returns Unit

        viewModel =
            ChildrenListViewModel(
                mockRepository, mockContext, mockAnalyticsTracker, mockToastManager)
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        coVerify(exactly = 2) { mockRepository.refreshChildren() }
      }

  @Test
  fun `refresh when Error and state Loading sets Error`() = runTest {
    val unconfined = UnconfinedTestDispatcher(testScheduler)
    Dispatchers.setMain(unconfined)
    try {
      coEvery { mockRepository.listChildrenCached() } returns flowOf(ApiResult.Success(emptyList()))
      coEvery { mockRepository.hasSyncedFlow } returns MutableStateFlow(false)
      coEvery { mockRepository.refreshChildren() } returns
          ApiResult.Error(ApiError.NetworkError("down"))

      viewModel =
          ChildrenListViewModel(mockRepository, mockContext, mockAnalyticsTracker, mockToastManager)
      advanceUntilIdle()

      assertIs<ChildrenListUiState.Error>(viewModel.uiState.value)
    } finally {
      Dispatchers.resetMain()
    }
  }

  @Test
  fun `observeChildren when result is Loading and hasSynced true shows Loading`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listChildrenCached() } returns flowOf(ApiResult.Loading())
        coEvery { mockRepository.hasSyncedFlow } returns flowOf(true)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        every { mockToastManager.showSuccess(any()) } returns Unit

        viewModel =
            ChildrenListViewModel(
                mockRepository, mockContext, mockAnalyticsTracker, mockToastManager)
        advanceUntilIdle()

        assertIs<ChildrenListUiState.Loading>(viewModel.uiState.value)
      }

  @Test
  fun `deleteChild when Error emits deleteError`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listChildrenCached() } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow } returns flowOf(true)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.deleteChild(1) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))
        every { mockToastManager.showSuccess(any()) } returns Unit

        viewModel =
            ChildrenListViewModel(
                mockRepository, mockContext, mockAnalyticsTracker, mockToastManager)
        advanceUntilIdle()
        val emissions = mutableListOf<String>()
        val job = launch { viewModel.deleteError.collect { emissions.add(it) } }
        viewModel.deleteChild(1)
        advanceUntilIdle()
        job.cancel()

        assert(emissions.size == 1)
        // No analytics call on error
        verify(exactly = 0) { mockAnalyticsTracker.logChildDeleted(any()) }
      }

  @Test
  fun `deleteChild calls repository delete`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listChildrenCached() } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow } returns flowOf(true)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.deleteChild(1) } returns ApiResult.Success(Unit)
        every { mockAnalyticsTracker.logChildDeleted(any()) } returns Unit
        every { mockToastManager.showSuccess(any()) } returns Unit

        viewModel =
            ChildrenListViewModel(
                mockRepository, mockContext, mockAnalyticsTracker, mockToastManager)
        advanceUntilIdle()
        viewModel.deleteChild(1)
        advanceUntilIdle()

        coVerify { mockRepository.deleteChild(1) }
        verify { mockAnalyticsTracker.logChildDeleted(0) }
      }
}
