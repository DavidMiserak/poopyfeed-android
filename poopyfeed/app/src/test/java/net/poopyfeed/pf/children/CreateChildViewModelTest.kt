package net.poopyfeed.pf.children

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
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
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.ui.toast.ToastManager
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Unit tests for CreateChildViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateChildViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var mockRepository: CachedChildrenRepository
  private lateinit var mockAnalyticsTracker: AnalyticsTracker
  private lateinit var mockToastManager: ToastManager
  private lateinit var viewModel: CreateChildViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    mockRepository = mockk()
    mockAnalyticsTracker = mockk()
    mockToastManager = mockk(relaxed = true)
    every { mockContext.getString(any()) } returns "Error message"
    viewModel = CreateChildViewModel(mockRepository, mockAnalyticsTracker, mockContext, mockToastManager)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is Idle`() {
    val state = viewModel.uiState.value
    assertIs<CreateChildUiState.Idle>(state)
  }

  @Test
  fun `createChild with valid inputs calls repository`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild()
        coEvery { mockRepository.createChild(any()) } returns ApiResult.Success(mockChild)
        every { mockRepository.listChildrenCached() } returns
            flowOf(ApiResult.Success(listOf(mockChild)))
        every { mockAnalyticsTracker.logChildCreated(any()) } returns Unit

        viewModel.createChild("Alice", "2024-01-15", "F")
        advanceUntilIdle()

        coVerify { mockRepository.createChild(any()) }
        verify { mockAnalyticsTracker.logChildCreated(1) }
        assertIs<CreateChildUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `createChild when repo returns Error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createChild(any()) } returns
            ApiResult.Error(ApiError.NetworkError("Network down"))

        viewModel.createChild("Alice", "2024-01-15", "F")
        advanceUntilIdle()

        assertIs<CreateChildUiState.Error>(viewModel.uiState.value)
        assertEquals("Error message", (viewModel.uiState.value as CreateChildUiState.Error).message)
      }

  @Test
  fun `createChild when repo returns Loading keeps Saving state`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createChild(any()) } returns ApiResult.Loading()

        viewModel.createChild("Alice", "2024-01-15", "F")
        advanceUntilIdle()

        assertIs<CreateChildUiState.Saving>(viewModel.uiState.value)
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
  fun `createChild validates inputs before calling repository`() =
      runTest(testDispatcher) {
        viewModel.createChild("", "", "")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<CreateChildUiState.ValidationError>(state)
        coVerify(exactly = 0) { mockRepository.createChild(any()) }
      }

  @Test
  fun `createChild logs analytics with child count on success`() =
      runTest(testDispatcher) {
        val child1 = TestFixtures.mockChild()
        val child2 = TestFixtures.mockChild().copy(id = 2, name = "Bob")
        coEvery { mockRepository.createChild(any()) } returns ApiResult.Success(child1)
        every { mockRepository.listChildrenCached() } returns
            flowOf(ApiResult.Success(listOf(child1, child2)))
        every { mockAnalyticsTracker.logChildCreated(any()) } returns Unit

        viewModel.createChild("Alice", "2024-01-15", "F")
        advanceUntilIdle()

        verify { mockAnalyticsTracker.logChildCreated(2) }
      }
}
