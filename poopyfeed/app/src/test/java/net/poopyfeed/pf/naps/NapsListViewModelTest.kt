package net.poopyfeed.pf.naps

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.models.UpdateNapRequest
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NapsListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedNapsRepository
  private lateinit var mockAnalyticsTracker: net.poopyfeed.pf.analytics.AnalyticsTracker
  private lateinit var viewModel: NapsListViewModel

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
    val pagingData: Flow<PagingData<Nap>> = flowOf()
    every { mockRepository.pagedNaps(1) } returns pagingData

    viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)

    assert(viewModel.pagingData == pagingData)
  }

  @Test
  fun `deleteError flow is initialized`() {
    every { mockRepository.pagedNaps(1) } returns flowOf()

    viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)

    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `deleteNap calls repository and clears error on success`() = runTest {
    every { mockRepository.pagedNaps(1) } returns flowOf()
    coEvery { mockRepository.deleteNap(1, 10) } returns ApiResult.Success(Unit)

    viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)
    viewModel.deleteError.value // access to initialize

    viewModel.deleteNap(10)
    advanceUntilIdle()

    coVerify { mockRepository.deleteNap(1, 10) }
    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `deleteNap sets error on failure`() = runTest {
    every { mockRepository.pagedNaps(1) } returns flowOf()
    coEvery { mockRepository.deleteNap(1, 10) } returns
        ApiResult.Error(ApiError.NetworkError("offline"))

    viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)
    viewModel.deleteNap(10)
    advanceUntilIdle()

    assert(viewModel.deleteError.value == "Failed to delete nap")
  }

  @Test
  fun `endNap calls updateNap with end_time`() = runTest {
    every { mockRepository.pagedNaps(1) } returns flowOf()
    val requestSlot = slot<UpdateNapRequest>()
    val mockNap = TestFixtures.mockNap()
    coEvery { mockRepository.updateNap(1, 10, capture(requestSlot)) } returns
        ApiResult.Success(mockNap)

    viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)
    viewModel.endNap(10)
    advanceUntilIdle()

    coVerify { mockRepository.updateNap(1, 10, any()) }
    assert(requestSlot.captured.end_time != null) { "end_time should be set" }
    assert(requestSlot.captured.start_time == null) { "start_time should not be set" }
    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `endNap sets error on failure`() = runTest {
    every { mockRepository.pagedNaps(1) } returns flowOf()
    coEvery { mockRepository.updateNap(1, 10, any()) } returns
        ApiResult.Error(ApiError.NetworkError("offline"))

    viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockAnalyticsTracker)
    viewModel.endNap(10)
    advanceUntilIdle()

    assert(viewModel.deleteError.value == "Failed to end nap")
  }
}
