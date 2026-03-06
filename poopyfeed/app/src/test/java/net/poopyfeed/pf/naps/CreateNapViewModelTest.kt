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
class CreateNapViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedNapsRepository
  private lateinit var viewModel: CreateNapViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
    viewModel = CreateNapViewModel(savedStateHandle, mockRepository, mockContext)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `createNap happy path calls repo and emits Success`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createNap(1, any()) } returns
            ApiResult.Success(TestFixtures.mockNap(end_time = null))

        viewModel.createNap("2024-01-15T12:00:00Z")
        advanceUntilIdle()

        coVerify { mockRepository.createNap(1, any()) }
        assertIs<CreateNapUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `createNap API error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createNap(1, any()) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel.createNap("2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<CreateNapUiState.Error>(viewModel.uiState.value)
        assert((viewModel.uiState.value as CreateNapUiState.Error).message == "Error message")
      }
}
