package com.poopyfeed.android.ui.screens.children

import androidx.lifecycle.SavedStateHandle
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.TodaySummaryDiapers
import com.poopyfeed.android.data.remote.dto.TodaySummaryFeedings
import com.poopyfeed.android.data.remote.dto.TodaySummaryResponse
import com.poopyfeed.android.data.remote.dto.TodaySummarySleep
import com.poopyfeed.android.data.repository.AnalyticsRepository
import com.poopyfeed.android.data.repository.ChildrenRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChildDashboardViewModelTest {
    private lateinit var childrenRepository: ChildrenRepository
    private lateinit var analyticsRepository: AnalyticsRepository
    private val testDispatcher = StandardTestDispatcher()

    private val mockChild: Child =
        Child(
            id = 1,
            name = "Test Child",
            dateOfBirth = "2024-01-15",
            gender = "F",
            userRole = "owner",
            canEdit = true,
            canManageSharing = true,
            createdAt = "2024-01-15T10:00:00Z",
            updatedAt = "2024-01-15T10:00:00Z",
            lastDiaperChange = null,
            lastNap = null,
            lastFeeding = null,
        )

    private val mockSummary: TodaySummaryResponse =
        TodaySummaryResponse(
            childId = 1,
            period = "today",
            feedings = TodaySummaryFeedings(count = 2, totalOz = 6.0, bottle = 2, breast = 0),
            diapers = TodaySummaryDiapers(count = 3, wet = 2, dirty = 1, both = 0),
            sleep = TodaySummarySleep(naps = 1, totalMinutes = 45, avgDuration = 45),
            lastUpdated = "2024-01-15T14:00:00Z",
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        childrenRepository = mock()
        analyticsRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `childId 0 from SavedStateHandle sets error and does not call API`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "0"))
            val viewModel =
                ChildDashboardViewModel(
                    childrenRepository,
                    analyticsRepository,
                    savedStateHandle,
                )
            advanceUntilIdle()

            assertEquals("Invalid child", viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.child)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `invalid childId from SavedStateHandle sets error`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "abc"))
            val viewModel =
                ChildDashboardViewModel(
                    childrenRepository,
                    analyticsRepository,
                    savedStateHandle,
                )
            advanceUntilIdle()

            assertEquals("Invalid child", viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.child)
        }

    @Test
    fun `loadDashboard with valid childId and both repos success sets child and todaySummary`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
            whenever(childrenRepository.getChild(1)).thenReturn(Result.success(mockChild))
            whenever(analyticsRepository.getTodaySummary(1)).thenReturn(Result.success(mockSummary))

            val viewModel =
                ChildDashboardViewModel(
                    childrenRepository,
                    analyticsRepository,
                    savedStateHandle,
                )
            advanceUntilIdle()

            assertEquals(mockChild, viewModel.uiState.value.child)
            assertEquals(mockSummary, viewModel.uiState.value.todaySummary)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `loadDashboard when getChild fails sets error and isLoading false`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
            whenever(childrenRepository.getChild(1))
                .thenReturn(Result.failure(Exception("Child not found")))
            whenever(analyticsRepository.getTodaySummary(1))
                .thenReturn(Result.success(mockSummary))

            val viewModel =
                ChildDashboardViewModel(
                    childrenRepository,
                    analyticsRepository,
                    savedStateHandle,
                )
            advanceUntilIdle()

            assertEquals("Child not found", viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.child)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `loadDashboard when getTodaySummary fails still sets child and clears loading`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
            whenever(childrenRepository.getChild(1)).thenReturn(Result.success(mockChild))
            whenever(analyticsRepository.getTodaySummary(1))
                .thenReturn(Result.failure(Exception("Summary unavailable")))

            val viewModel =
                ChildDashboardViewModel(
                    childrenRepository,
                    analyticsRepository,
                    savedStateHandle,
                )
            advanceUntilIdle()

            assertEquals(mockChild, viewModel.uiState.value.child)
            assertNull(viewModel.uiState.value.todaySummary)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `loadDashboard retry sets isLoading then result`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
            whenever(childrenRepository.getChild(1)).thenReturn(Result.success(mockChild))
            whenever(analyticsRepository.getTodaySummary(1)).thenReturn(Result.success(mockSummary))

            val viewModel =
                ChildDashboardViewModel(
                    childrenRepository,
                    analyticsRepository,
                    savedStateHandle,
                )
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.child)
            assertFalse(viewModel.uiState.value.isLoading)

            viewModel.loadDashboard()
            assertTrue(viewModel.uiState.value.isLoading)

            advanceUntilIdle()
            assertEquals(mockChild, viewModel.uiState.value.child)
            assertEquals(mockSummary, viewModel.uiState.value.todaySummary)
            assertFalse(viewModel.uiState.value.isLoading)
        }
}
