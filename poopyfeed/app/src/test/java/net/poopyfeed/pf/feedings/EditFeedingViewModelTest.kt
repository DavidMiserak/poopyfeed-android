package net.poopyfeed.pf.feedings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
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
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditFeedingViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedFeedingsRepository
  private lateinit var viewModel: EditFeedingViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1, "feedingId" to 2))
    mockRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init loads feeding and emits Ready with correct data`() =
      runTest(testDispatcher) {
        val feeding = TestFixtures.mockFeeding(id = 2, feeding_type = "bottle", amount_oz = 4.0)
        coEvery { mockRepository.getFeeding(1, 2) } returns feeding

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<EditFeedingUiState.Ready>(viewModel.uiState.value)
        assertEquals(2, (viewModel.uiState.value as EditFeedingUiState.Ready).feeding.id)
        assertEquals(
            "bottle", (viewModel.uiState.value as EditFeedingUiState.Ready).feeding.feeding_type)
      }

  @Test
  fun `init when getFeeding returns null emits Error`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.getFeeding(1, 2) } returns null

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<EditFeedingUiState.Error>(viewModel.uiState.value)
        assertEquals(
            "Feeding not found.",
            (viewModel.uiState.value as EditFeedingUiState.Error).message,
        )
      }

  @Test
  fun `saveFeeding with valid data calls repo updateFeeding and emits Success`() =
      runTest(testDispatcher) {
        val feeding = TestFixtures.mockFeeding(id = 2)
        coEvery { mockRepository.getFeeding(1, 2) } returns feeding
        coEvery { mockRepository.updateFeeding(1, 2, any()) } returns
            ApiResult.Success(TestFixtures.mockFeeding(id = 2))

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveFeeding("bottle", 5.0, null, "", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        coVerify { mockRepository.updateFeeding(1, 2, any()) }
        assertIs<EditFeedingUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `saveFeeding with invalid data emits ValidationError and does not call repo`() =
      runTest(testDispatcher) {
        val feeding = TestFixtures.mockFeeding(id = 2)
        coEvery { mockRepository.getFeeding(1, 2) } returns feeding

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveFeeding("", null, null, "", "2024-01-15T12:00:00Z")

        assertIs<EditFeedingUiState.ValidationError>(viewModel.uiState.value)
        coVerify(exactly = 0) { mockRepository.updateFeeding(any(), any(), any()) }
      }

  @Test
  fun `saveFeeding API error emits SaveError`() =
      runTest(testDispatcher) {
        val feeding = TestFixtures.mockFeeding(id = 2)
        coEvery { mockRepository.getFeeding(1, 2) } returns feeding
        coEvery { mockRepository.updateFeeding(1, 2, any()) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveFeeding("bottle", 4.0, null, "", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<EditFeedingUiState.SaveError>(viewModel.uiState.value)
        assertEquals(
            "Error message",
            (viewModel.uiState.value as EditFeedingUiState.SaveError).message,
        )
      }

  @Test
  fun `saveFeeding when repo returns Loading keeps Saving state`() =
      runTest(testDispatcher) {
        val feeding = TestFixtures.mockFeeding(id = 2)
        coEvery { mockRepository.getFeeding(1, 2) } returns feeding
        coEvery { mockRepository.updateFeeding(1, 2, any()) } returns ApiResult.Loading()

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveFeeding("bottle", 4.0, null, "", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<EditFeedingUiState.Saving>(viewModel.uiState.value)
      }

  @Test
  fun `saveFeeding bottle with zero amount emits ValidationError`() =
      runTest(testDispatcher) {
        val feeding = TestFixtures.mockFeeding(id = 2)
        coEvery { mockRepository.getFeeding(1, 2) } returns feeding

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveFeeding("bottle", 0.0, null, "", "2024-01-15T12:00:00Z")

        assertIs<EditFeedingUiState.ValidationError>(viewModel.uiState.value)
        assertEquals(
            "Enter amount for bottle feeding.",
            (viewModel.uiState.value as EditFeedingUiState.ValidationError).amountError,
        )
      }

  @Test
  fun `saveFeeding breast with zero minutes emits ValidationError`() =
      runTest(testDispatcher) {
        val feeding = TestFixtures.mockFeeding(id = 2)
        coEvery { mockRepository.getFeeding(1, 2) } returns feeding

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveFeeding("breast", null, 0, "", "2024-01-15T12:00:00Z")

        assertIs<EditFeedingUiState.ValidationError>(viewModel.uiState.value)
        assertEquals(
            "Enter minutes for breast feeding.",
            (viewModel.uiState.value as EditFeedingUiState.ValidationError).minutesError,
        )
      }

  @Test
  fun `saveFeeding breast with invalid side emits ValidationError`() =
      runTest(testDispatcher) {
        val feeding = TestFixtures.mockFeeding(id = 2)
        coEvery { mockRepository.getFeeding(1, 2) } returns feeding

        viewModel = EditFeedingViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveFeeding("breast", null, 10, "invalid", "2024-01-15T12:00:00Z")

        assertIs<EditFeedingUiState.ValidationError>(viewModel.uiState.value)
        assertEquals(
            "Select side for breast feeding.",
            (viewModel.uiState.value as EditFeedingUiState.ValidationError).sideError,
        )
      }
}
