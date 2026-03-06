package net.poopyfeed.pf.naps

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NapsListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedNapsRepository
  private lateinit var viewModel: NapsListViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init triggers refresh`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listNapsCached(1) } returns flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshNaps(1) } returns ApiResult.Success(emptyList())

        viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        coVerify { mockRepository.refreshNaps(1) }
      }

  @Test
  fun `uiState is Ready when repo returns data and hasSynced true`() =
      runTest(testDispatcher) {
        val naps = listOf(TestFixtures.mockNap())
        coEvery { mockRepository.listNapsCached(1) } returns flowOf(ApiResult.Success(naps))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshNaps(1) } returns ApiResult.Success(naps)

        viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<NapsListUiState.Ready>(viewModel.uiState.value)
        assert((viewModel.uiState.value as NapsListUiState.Ready).naps == naps)
      }

  @Test
  fun `uiState is Empty when repo returns empty and hasSynced true`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listNapsCached(1) } returns flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshNaps(1) } returns ApiResult.Success(emptyList())

        viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<NapsListUiState.Empty>(viewModel.uiState.value)
      }

  @Test
  fun `deleteNap success`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listNapsCached(1) } returns flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshNaps(1) } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.deleteNap(1, 10) } returns ApiResult.Success(Unit)

        viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.deleteNap(10)
        advanceUntilIdle()

        coVerify { mockRepository.deleteNap(1, 10) }
      }

  @Test
  fun `endNap calls repo updateNap with end_time set`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listNapsCached(1) } returns flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshNaps(1) } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.updateNap(1, 10, any()) } returns
            ApiResult.Success(TestFixtures.mockNap(id = 10, end_time = "2024-01-15T14:00:00Z"))

        viewModel = NapsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.endNap(10)
        advanceUntilIdle()

        coVerify { mockRepository.updateNap(1, 10, match { it.end_time.isNotBlank() }) }
      }
}
