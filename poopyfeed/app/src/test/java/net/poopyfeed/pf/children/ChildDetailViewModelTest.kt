package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Unit tests for ChildDetailViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChildDetailViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var mockRepository: CachedChildrenRepository
  private lateinit var savedStateHandle: SavedStateHandle

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    mockRepository = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    every { mockContext.getString(any()) } returns "formatted_time"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `loads child from repository`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild()
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns
            ApiResult.Success(TestFixtures.mockDashboardSummaryResponse())

        val viewModel = ChildDetailViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        assertEquals(mockChild.name, state.child.name)
      }

  @Test
  fun `loads child with non M or F gender shows Other`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild(gender = "other")
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns
            ApiResult.Success(TestFixtures.mockDashboardSummaryResponse())

        val viewModel = ChildDetailViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        val ready = state
        assertTrue(ready.ageFormatted.contains("Other"))
      }

  @Test
  fun `Ready state includes dashboardSummary when getDashboardSummary succeeds`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild()
        val mockSummary =
            TestFixtures.mockDashboardSummaryResponse(
                todayFeedings = 5, todayDiapers = 3, todayNaps = 2)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns ApiResult.Success(mockSummary)

        val viewModel = ChildDetailViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        val ready = state
        assertEquals(mockSummary, ready.dashboardSummary)
        assertEquals(5, ready.dashboardSummary?.today?.feedings?.count)
        assertEquals(3, ready.dashboardSummary?.today?.diapers?.count)
        assertEquals(2, ready.dashboardSummary?.today?.sleep?.naps)
      }

  @Test
  fun `Ready state has null dashboardSummary when getDashboardSummary fails`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild()
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("failed"))

        val viewModel = ChildDetailViewModel(savedStateHandle, mockRepository, mockContext)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        assertEquals(null, state.dashboardSummary)
      }
}
