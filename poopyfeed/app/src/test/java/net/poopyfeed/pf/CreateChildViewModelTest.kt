package net.poopyfeed.pf

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import org.junit.Before
import org.junit.Test

/** Unit tests for CreateChildViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateChildViewModelTest {

  private lateinit var mockContext: Context
  private lateinit var mockRepository: CachedChildrenRepository
  private lateinit var viewModel: CreateChildViewModel

  @Before
  fun setup() {
    mockContext = mockk()
    mockRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
    viewModel = CreateChildViewModel(mockRepository, mockContext)
  }

  @Test
  fun `initial state is Idle`() {
    val state = viewModel.uiState.value
    assertIs<CreateChildUiState.Idle>(state)
  }

  @Test
  fun `createChild with valid inputs calls repository`() = runTest {
    coEvery { mockRepository.createChild(any()) } returns
        ApiResult.Success(TestFixtures.mockChild())

    viewModel.createChild("Alice", "2024-01-15", "F")

    // Verify repository was called
    coVerify { mockRepository.createChild(any()) }
  }

  @Test
  fun `createChild with empty name sets ValidationError`() {
    viewModel.createChild("", "2024-01-15", "F")

    val state = viewModel.uiState.value
    assertIs<CreateChildUiState.ValidationError>(state)
  }

  @Test
  fun `createChild with empty DOB sets ValidationError`() {
    viewModel.createChild("Alice", "", "F")

    val state = viewModel.uiState.value
    assertIs<CreateChildUiState.ValidationError>(state)
  }

  @Test
  fun `createChild with empty gender sets ValidationError`() {
    viewModel.createChild("Alice", "2024-01-15", "")

    val state = viewModel.uiState.value
    assertIs<CreateChildUiState.ValidationError>(state)
  }

  @Test
  fun `resetState returns to Idle`() {
    viewModel.resetState()
    val state = viewModel.uiState.value
    assertIs<CreateChildUiState.Idle>(state)
  }

  @Test
  fun `createChild validates inputs before calling repository`() = runTest {
    // This test verifies that with empty inputs, the repository is never called
    viewModel.createChild("", "", "")

    val state = viewModel.uiState.value
    assertIs<CreateChildUiState.ValidationError>(state)
    // Repository should not be called
    coVerify(exactly = 0) { mockRepository.createChild(any()) }
  }
}
