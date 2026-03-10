package net.poopyfeed.pf.feedings

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedingsListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedFeedingsRepository
  private lateinit var mockAnalyticsTracker: net.poopyfeed.pf.analytics.AnalyticsTracker
  private lateinit var viewModel: FeedingsListViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
    mockAnalyticsTracker = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `pagingData flow is exposed from repository`() {
    val pagingData: Flow<PagingData<Feeding>> = flowOf()
    every { mockRepository.pagedFeedings(1) } returns pagingData

    viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)

    assert(viewModel.pagingData == pagingData)
  }

  @Test
  fun `deleteError flow is initialized`() {
    every { mockRepository.pagedFeedings(1) } returns flowOf()

    viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)

    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `deleteFeeding calls repository and clears error on success`() = runTest {
    every { mockRepository.pagedFeedings(1) } returns flowOf()
    coEvery { mockRepository.deleteFeeding(1, 10) } returns ApiResult.Success(Unit)

    viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)
    viewModel.deleteFeeding(10)
    advanceUntilIdle()

    coVerify { mockRepository.deleteFeeding(1, 10) }
    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `deleteFeeding sets error on failure`() = runTest {
    every { mockRepository.pagedFeedings(1) } returns flowOf()
    coEvery { mockRepository.deleteFeeding(1, 10) } returns
        ApiResult.Error(ApiError.NetworkError("offline"))

    viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)
    viewModel.deleteFeeding(10)
    advanceUntilIdle()

    assert(viewModel.deleteError.value == "Failed to delete feeding")
  }
}
