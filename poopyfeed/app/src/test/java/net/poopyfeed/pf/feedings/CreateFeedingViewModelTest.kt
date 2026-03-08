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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.sync.SyncScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateFeedingViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedFeedingsRepository
  private lateinit var mockSyncScheduler: SyncScheduler
  private lateinit var viewModel: CreateFeedingViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
    mockSyncScheduler = mockk(relaxed = true)
    every { mockContext.getString(any()) } returns "Error message"
    viewModel =
        CreateFeedingViewModel(savedStateHandle, mockRepository, mockSyncScheduler, mockContext)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `createFeeding with empty type sets ValidationError and does not call repo`() {
    viewModel.createFeeding("", null, null, "", "2024-01-15T12:00:00Z")

    assertIs<CreateFeedingUiState.ValidationError>(viewModel.uiState.value)
    coVerify(exactly = 0) { mockRepository.createFeeding(any(), any()) }
  }

  @Test
  fun `createFeeding bottle with zero amount sets ValidationError`() {
    viewModel.createFeeding("bottle", 0.0, null, "", "2024-01-15T12:00:00Z")

    assertIs<CreateFeedingUiState.ValidationError>(viewModel.uiState.value)
    coVerify(exactly = 0) { mockRepository.createFeeding(any(), any()) }
  }

  @Test
  fun `createFeeding breast without minutes sets ValidationError`() {
    viewModel.createFeeding("breast", null, null, "", "2024-01-15T12:00:00Z")

    assertIs<CreateFeedingUiState.ValidationError>(viewModel.uiState.value)
    coVerify(exactly = 0) { mockRepository.createFeeding(any(), any()) }
  }

  @Test
  fun `createFeeding happy path bottle calls repo and emits Success`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createFeeding(1, any()) } returns
            ApiResult.Success(TestFixtures.mockFeeding())

        viewModel.createFeeding("bottle", 4.0, null, "", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        coVerify { mockRepository.createFeeding(1, any()) }
        assertIs<CreateFeedingUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `createFeeding API error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createFeeding(1, any()) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel.createFeeding("breast", null, 15, "left", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<CreateFeedingUiState.Error>(viewModel.uiState.value)
        assert((viewModel.uiState.value as CreateFeedingUiState.Error).message == "Error message")
      }

  @Test
  fun `createFeeding when repo returns Loading keeps Saving`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createFeeding(1, any()) } returns ApiResult.Loading()

        viewModel.createFeeding("bottle", 4.0, null, "", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<CreateFeedingUiState.Saving>(viewModel.uiState.value)
      }
}
