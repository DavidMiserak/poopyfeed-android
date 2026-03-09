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
  private lateinit var viewModel: NapsListViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `pagingData flow is exposed from repository`() {
    val pagingData: Flow<PagingData<Nap>> = flowOf()
    every { mockRepository.pagedNaps(1) } returns pagingData

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)

    assert(viewModel.pagingData == pagingData)
  }

  @Test
  fun `deleteError flow is initialized`() {
    every { mockRepository.pagedNaps(1) } returns flowOf()

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)

    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `deleteNap method exists`() {
    every { mockRepository.pagedNaps(1) } returns flowOf()

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)

    // Should not throw
    viewModel.deleteNap(10)
  }

  @Test
  fun `endNap calls updateNap with end_time`() = runTest {
    every { mockRepository.pagedNaps(1) } returns flowOf()
    val requestSlot = slot<UpdateNapRequest>()
    val mockNap = TestFixtures.mockNap()
    coEvery { mockRepository.updateNap(1, 10, capture(requestSlot)) } returns
        ApiResult.Success(mockNap)

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)
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

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)
    viewModel.endNap(10)
    advanceUntilIdle()

    assert(viewModel.deleteError.value == "Failed to end nap")
  }
}
