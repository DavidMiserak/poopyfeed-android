package net.poopyfeed.pf.feedings

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
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedingsListViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedFeedingsRepository
  private lateinit var viewModel: FeedingsListViewModel

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
        coEvery { mockRepository.listFeedingsCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshFeedings(1) } returns ApiResult.Success(emptyList())

        viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        coVerify { mockRepository.refreshFeedings(1) }
      }

  @Test
  fun `uiState is Ready when repo returns data and hasSynced true`() =
      runTest(testDispatcher) {
        val feedings = listOf(TestFixtures.mockFeeding())
        coEvery { mockRepository.listFeedingsCached(1) } returns flowOf(ApiResult.Success(feedings))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshFeedings(1) } returns ApiResult.Success(feedings)

        viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<FeedingsListUiState.Ready>(viewModel.uiState.value)
        assert((viewModel.uiState.value as FeedingsListUiState.Ready).feedings == feedings)
      }

  @Test
  fun `uiState is Empty when repo returns empty and hasSynced true`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listFeedingsCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshFeedings(1) } returns ApiResult.Success(emptyList())

        viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<FeedingsListUiState.Empty>(viewModel.uiState.value)
      }

  @Test
  fun `deleteFeeding success`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listFeedingsCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshFeedings(1) } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.deleteFeeding(1, 10) } returns ApiResult.Success(Unit)

        viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.deleteFeeding(10)
        advanceUntilIdle()

        coVerify { mockRepository.deleteFeeding(1, 10) }
      }

  @Test
  fun `deleteFeeding error emits deleteError`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.listFeedingsCached(1) } returns
            flowOf(ApiResult.Success(emptyList()))
        coEvery { mockRepository.hasSyncedFlow(1) } returns flowOf(true)
        coEvery { mockRepository.refreshFeedings(1) } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.deleteFeeding(1, 10) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel = FeedingsListViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        val emissions = mutableListOf<String>()
        val job = this.launch { viewModel.deleteError.collect { emissions.add(it) } }
        viewModel.deleteFeeding(10)
        advanceUntilIdle()

        job.cancel()
        assert(emissions.size == 1)
        assert(emissions[0] == "Error message")
      }
}
