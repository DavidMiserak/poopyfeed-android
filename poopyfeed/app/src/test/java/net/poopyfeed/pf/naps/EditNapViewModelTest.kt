package net.poopyfeed.pf.naps

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
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditNapViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedNapsRepository
  private lateinit var viewModel: EditNapViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1, "napId" to 2))
    mockRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init loads nap and emits Ready with correct data`() =
      runTest(testDispatcher) {
        val nap = TestFixtures.mockNap(id = 2, end_time = "2024-01-15T11:00:00Z")
        coEvery { mockRepository.getNap(1, 2) } returns nap

        viewModel = EditNapViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<EditNapUiState.Ready>(viewModel.uiState.value)
        assertEquals(2, (viewModel.uiState.value as EditNapUiState.Ready).nap.id)
        assertEquals(
            "2024-01-15T11:00:00Z", (viewModel.uiState.value as EditNapUiState.Ready).nap.end_time)
      }

  @Test
  fun `init when getNap returns null emits Error`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.getNap(1, 2) } returns null

        viewModel = EditNapViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        assertIs<EditNapUiState.Error>(viewModel.uiState.value)
        assertEquals(
            "Nap not found.",
            (viewModel.uiState.value as EditNapUiState.Error).message,
        )
      }

  @Test
  fun `saveNap with valid data calls repo updateNap and emits Success`() =
      runTest(testDispatcher) {
        val nap = TestFixtures.mockNap(id = 2)
        coEvery { mockRepository.getNap(1, 2) } returns nap
        coEvery { mockRepository.updateNap(1, 2, any()) } returns
            ApiResult.Success(TestFixtures.mockNap(id = 2))

        viewModel = EditNapViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveNap("2024-01-15T10:00:00Z", "2024-01-15T11:00:00Z")
        advanceUntilIdle()

        coVerify { mockRepository.updateNap(1, 2, any()) }
        assertIs<EditNapUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `saveNap API error emits SaveError`() =
      runTest(testDispatcher) {
        val nap = TestFixtures.mockNap(id = 2)
        coEvery { mockRepository.getNap(1, 2) } returns nap
        coEvery { mockRepository.updateNap(1, 2, any()) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel = EditNapViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveNap("2024-01-15T10:00:00Z", null)
        advanceUntilIdle()

        assertIs<EditNapUiState.SaveError>(viewModel.uiState.value)
        assertEquals(
            "Error message",
            (viewModel.uiState.value as EditNapUiState.SaveError).message,
        )
      }

  @Test
  fun `saveNap when repo returns Loading keeps Saving`() =
      runTest(testDispatcher) {
        val nap = TestFixtures.mockNap(id = 2)
        coEvery { mockRepository.getNap(1, 2) } returns nap
        coEvery { mockRepository.updateNap(1, 2, any()) } returns ApiResult.Loading()

        viewModel = EditNapViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveNap("2024-01-15T10:00:00Z", "2024-01-15T11:00:00Z")
        advanceUntilIdle()

        assertIs<EditNapUiState.Saving>(viewModel.uiState.value)
      }
}
