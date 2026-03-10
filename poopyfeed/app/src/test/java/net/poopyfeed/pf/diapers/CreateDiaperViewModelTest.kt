package net.poopyfeed.pf.diapers

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.sync.SyncScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateDiaperViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedDiapersRepository
  private lateinit var mockSyncScheduler: SyncScheduler
  private lateinit var mockAnalyticsTracker: AnalyticsTracker
  private lateinit var viewModel: CreateDiaperViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
    mockSyncScheduler = mockk(relaxed = true)
    mockAnalyticsTracker = mockk(relaxed = true)
    every { mockContext.getString(any()) } returns "Error message"
    viewModel =
        CreateDiaperViewModel(savedStateHandle, mockRepository, mockSyncScheduler, mockAnalyticsTracker, mockContext)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `createDiaper with empty type sets ValidationError and does not call repo`() {
    viewModel.createDiaper("", "2024-01-15T12:00:00Z")

    assertIs<CreateDiaperUiState.ValidationError>(viewModel.uiState.value)
    coVerify(exactly = 0) { mockRepository.createDiaper(any(), any()) }
  }

  @Test
  fun `createDiaper happy path calls repo and emits Success`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createDiaper(1, any()) } returns
            ApiResult.Success(TestFixtures.mockDiaper())

        viewModel.createDiaper("wet", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        coVerify { mockRepository.createDiaper(1, any()) }
        assertIs<CreateDiaperUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `createDiaper API error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createDiaper(1, any()) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel.createDiaper("both", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<CreateDiaperUiState.Error>(viewModel.uiState.value)
        assert((viewModel.uiState.value as CreateDiaperUiState.Error).message == "Error message")
      }

  @Test
  fun `createDiaper when repo returns Loading keeps Saving`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createDiaper(1, any()) } returns ApiResult.Loading()

        viewModel.createDiaper("wet", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<CreateDiaperUiState.Saving>(viewModel.uiState.value)
      }

  @Test
  fun `createDiaper logs analytics with change type on success`() =
      runTest(testDispatcher) {
        val mockDiaper = TestFixtures.mockDiaper().copy(change_type = "both")
        coEvery { mockRepository.createDiaper(1, any()) } returns ApiResult.Success(mockDiaper)

        viewModel.createDiaper("both", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        verify { mockAnalyticsTracker.logDiaperLogged("both") }
      }
}
