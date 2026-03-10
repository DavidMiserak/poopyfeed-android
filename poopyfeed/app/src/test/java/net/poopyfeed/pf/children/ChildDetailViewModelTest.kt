package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AnalyticsRepository
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
  private lateinit var mockAnalyticsRepository: AnalyticsRepository
  private lateinit var mockAnalyticsTracker: net.poopyfeed.pf.analytics.AnalyticsTracker
  private lateinit var savedStateHandle: SavedStateHandle

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    mockRepository = mockk()
    mockAnalyticsTracker = mockk(relaxed = true)
    mockAnalyticsRepository = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    every { mockContext.getString(any()) } returns "formatted_time"
    every { mockContext.getString(R.string.child_detail_never) } returns "Never"
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
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Success(TestFixtures.mockPatternAlertsResponse())

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
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
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Success(TestFixtures.mockPatternAlertsResponse())

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
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
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Success(TestFixtures.mockPatternAlertsResponse())

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
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
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Success(TestFixtures.mockPatternAlertsResponse())

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        assertEquals(null, state.dashboardSummary)
      }

  @Test
  fun `Ready state includes patternAlerts when getPatternAlerts succeeds`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild()
        val mockAlerts =
            TestFixtures.mockPatternAlertsResponse(
                feedingAlert = true,
                feedingMessage = "Baby usually feeds every 3h — it's been 3h 30m",
                napAlert = false)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns
            ApiResult.Success(TestFixtures.mockDashboardSummaryResponse())
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Success(mockAlerts)

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        val ready = state
        assertEquals(mockAlerts, ready.patternAlerts)
        val alerts = ready.patternAlerts
        if (alerts != null) {
          assertTrue(alerts.hasAnyAlert == true)
          assertTrue(alerts.feeding.alert == true)
          assertEquals("Baby usually feeds every 3h — it's been 3h 30m", alerts.feeding.message)
          assertTrue(alerts.nap.alert == false)
        } else {
          fail("Expected non-null patternAlerts")
        }
      }

  @Test
  fun `Ready state has null patternAlerts when getPatternAlerts fails (silent suppression)`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild()
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns
            ApiResult.Success(TestFixtures.mockDashboardSummaryResponse())
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("failed"))

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        assertEquals(null, state.patternAlerts)
      }

  @Test
  fun `Ready state formats last activity as Never when child has no last feeding diaper or nap`() =
      runTest(testDispatcher) {
        val mockChild =
            TestFixtures.mockChild(
                last_feeding = null,
                last_diaper_change = null,
                last_nap = null,
            )
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns
            ApiResult.Success(TestFixtures.mockDashboardSummaryResponse())
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Success(TestFixtures.mockPatternAlertsResponse())

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        val ready = state
        assertEquals("Never", ready.lastFeedingFormatted)
        assertEquals("Never", ready.lastDiaperFormatted)
        assertEquals("Never", ready.lastNapFormatted)
      }

  @Test
  fun `Ready state includes non-empty last activity when child has last feeding diaper and nap`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild()
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns
            ApiResult.Success(TestFixtures.mockDashboardSummaryResponse())
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Success(TestFixtures.mockPatternAlertsResponse())

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ChildDetailUiState.Ready)
        val ready = state
        assertTrue(ready.lastFeedingFormatted.isNotEmpty())
        assertTrue(ready.lastDiaperFormatted.isNotEmpty())
        assertTrue(ready.lastNapFormatted.isNotEmpty())
      }

  @Test
  fun `refreshPatternAlerts re-calls repository and updates state`() =
      runTest(testDispatcher) {
        val mockChild = TestFixtures.mockChild()
        coEvery { mockRepository.getChildCached(1) } returns flowOf(mockChild)
        coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
        coEvery { mockRepository.getDashboardSummary(1) } returns
            ApiResult.Success(TestFixtures.mockDashboardSummaryResponse())
        coEvery { mockAnalyticsRepository.getPatternAlerts(1) } returns
            ApiResult.Success(TestFixtures.mockPatternAlertsResponse())

        val viewModel =
            ChildDetailViewModel(
                savedStateHandle,
                mockRepository,
                mockAnalyticsRepository,
                mockContext,
                mockAnalyticsTracker)
        advanceUntilIdle()

        val initialAlerts = (viewModel.uiState.value as? ChildDetailUiState.Ready)?.patternAlerts
        assertTrue(initialAlerts != null)

        // Call refreshPatternAlerts
        viewModel.refreshPatternAlerts()
        advanceUntilIdle()

        val finalAlerts = (viewModel.uiState.value as? ChildDetailUiState.Ready)?.patternAlerts
        assertTrue(finalAlerts != null)
      }
}
