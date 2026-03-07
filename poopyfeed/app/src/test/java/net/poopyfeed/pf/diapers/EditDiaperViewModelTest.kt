package net.poopyfeed.pf.diapers

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
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditDiaperViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedDiapersRepository
  private lateinit var viewModel: EditDiaperViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1, "diaperId" to 2))
    mockRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init loads diaper and emits Ready with correct data`() = runTest(testDispatcher) {
    val diaper = TestFixtures.mockDiaper(id = 2, change_type = "wet")
    coEvery { mockRepository.getDiaper(1, 2) } returns diaper

    viewModel = EditDiaperViewModel(savedStateHandle, mockRepository, mockContext)
    advanceUntilIdle()

    assertIs<EditDiaperUiState.Ready>(viewModel.uiState.value)
    assertEquals(2, (viewModel.uiState.value as EditDiaperUiState.Ready).diaper.id)
    assertEquals("wet", (viewModel.uiState.value as EditDiaperUiState.Ready).diaper.change_type)
  }

  @Test
  fun `init when getDiaper returns null emits Error`() = runTest(testDispatcher) {
    coEvery { mockRepository.getDiaper(1, 2) } returns null

    viewModel = EditDiaperViewModel(savedStateHandle, mockRepository, mockContext)
    advanceUntilIdle()

    assertIs<EditDiaperUiState.Error>(viewModel.uiState.value)
    assertEquals(
        "Diaper change not found.",
        (viewModel.uiState.value as EditDiaperUiState.Error).message,
    )
  }

  @Test
  fun `saveDiaper with valid data calls repo updateDiaper and emits Success`() =
      runTest(testDispatcher) {
        val diaper = TestFixtures.mockDiaper(id = 2)
        coEvery { mockRepository.getDiaper(1, 2) } returns diaper
        coEvery { mockRepository.updateDiaper(1, 2, any()) } returns
            ApiResult.Success(TestFixtures.mockDiaper(id = 2))

        viewModel = EditDiaperViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveDiaper("both", "2024-01-15T12:00:00Z")
        advanceUntilIdle()

        coVerify { mockRepository.updateDiaper(1, 2, any()) }
        assertIs<EditDiaperUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `saveDiaper with invalid data emits ValidationError and does not call repo`() =
      runTest(testDispatcher) {
        val diaper = TestFixtures.mockDiaper(id = 2)
        coEvery { mockRepository.getDiaper(1, 2) } returns diaper

        viewModel = EditDiaperViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()
        viewModel.saveDiaper("", "2024-01-15T12:00:00Z")

        assertIs<EditDiaperUiState.ValidationError>(viewModel.uiState.value)
        coVerify(exactly = 0) { mockRepository.updateDiaper(any(), any(), any()) }
      }

  @Test
  fun `saveDiaper API error emits SaveError`() = runTest(testDispatcher) {
    val diaper = TestFixtures.mockDiaper(id = 2)
    coEvery { mockRepository.getDiaper(1, 2) } returns diaper
    coEvery { mockRepository.updateDiaper(1, 2, any()) } returns
        ApiResult.Error(ApiError.NetworkError("fail"))

    viewModel = EditDiaperViewModel(savedStateHandle, mockRepository, mockContext)
    advanceUntilIdle()
    viewModel.saveDiaper("wet", "2024-01-15T12:00:00Z")
    advanceUntilIdle()

    assertIs<EditDiaperUiState.SaveError>(viewModel.uiState.value)
    assertEquals(
        "Error message",
        (viewModel.uiState.value as EditDiaperUiState.SaveError).message,
    )
  }
}
