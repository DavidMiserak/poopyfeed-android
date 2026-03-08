package net.poopyfeed.pf.diapers

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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiapersListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedDiapersRepository
  private lateinit var viewModel: DiapersListViewModel

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
        coEvery { mockRepository.listDiapersCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshDiapers(1) } returns ApiResult.Success(emptyList())

        viewModel = DiapersListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        coVerify { mockRepository.refreshDiapers(1) }
      }

  @Test
  fun `uiState is Ready when repo returns data and hasSynced true`() =
      runTest(testDispatcher) {
        val diapers = listOf(TestFixtures.mockDiaper())
        coEvery { mockRepository.listDiapersCached(1) } returns flowOf(ApiResult.Success(diapers))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshDiapers(1) } returns ApiResult.Success(diapers)

        viewModel = DiapersListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<DiapersListUiState.Ready>(viewModel.uiState.value)
        assert((viewModel.uiState.value as DiapersListUiState.Ready).diapers == diapers)
      }

  @Test
  fun `uiState is Empty when repo returns empty and hasSynced true`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listDiapersCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshDiapers(1) } returns ApiResult.Success(emptyList())

        viewModel = DiapersListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<DiapersListUiState.Empty>(viewModel.uiState.value)
      }

  @Test
  fun `deleteDiaper success`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listDiapersCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshDiapers(1) } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.deleteDiaper(1, 10) } returns ApiResult.Success(Unit)

        viewModel = DiapersListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.deleteDiaper(10)
        advanceUntilIdle()

        coVerify { mockRepository.deleteDiaper(1, 10) }
      }

  @Test
  fun `observeDiapers when result is Loading and hasSynced true shows Loading`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listDiapersCached(1) } returns flowOf(ApiResult.Loading())
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshDiapers(1) } returns ApiResult.Success(emptyList())

        viewModel = DiapersListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<DiapersListUiState.Loading>(viewModel.uiState.value)
      }

  @Test
  fun `deleteDiaper error emits deleteError`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listDiapersCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshDiapers(1) } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.deleteDiaper(1, 10) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel = DiapersListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        val emissions = mutableListOf<String>()
        val job = this.launch { viewModel.deleteError.collect { emissions.add(it) } }
        viewModel.deleteDiaper(10)
        advanceUntilIdle()

        job.cancel()
        assert(emissions.size == 1)
      }

  @Test
  fun `refresh when Error and state Loading sets Empty`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listDiapersCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(false, true)
        coEvery { mockRepository.refreshDiapers(1) } returns
            ApiResult.Error(ApiError.NetworkError("down"))

        viewModel = DiapersListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<DiapersListUiState.Empty>(viewModel.uiState.value)
      }

  @Test
  fun `refresh when Error and state Ready sets Error`() =
      runTest(testDispatcher) {
        val diapers = listOf(TestFixtures.mockDiaper())
        coEvery { mockRepository.listDiapersCached(1) } returns flowOf(ApiResult.Success(diapers))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshDiapers(1) } returns
            ApiResult.Error(ApiError.NetworkError("down"))

        viewModel = DiapersListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        assertIs<DiapersListUiState.Ready>(viewModel.uiState.value)
        viewModel.refresh()
        advanceUntilIdle()

        assertIs<DiapersListUiState.Error>(viewModel.uiState.value)
      }
}
