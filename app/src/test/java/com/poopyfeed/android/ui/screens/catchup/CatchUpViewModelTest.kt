package com.poopyfeed.android.ui.screens.catchup

import androidx.lifecycle.SavedStateHandle
import com.poopyfeed.android.data.remote.dto.BatchResponse
import com.poopyfeed.android.data.repository.BatchRepository
import com.poopyfeed.android.data.repository.CatchUpBatchEvent
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CatchUpViewModelTest {
    private lateinit var batchRepository: BatchRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        batchRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addFeeding adds one Feeding with bottle and amountOz 4`() {
        val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
        val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)

        viewModel.addFeeding()

        val events = viewModel.uiState.value.events
        assertEquals(1, events.size)
        val feeding = events[0] as CatchUpBatchEvent.Feeding
        assertEquals("bottle", feeding.feedingType)
        assertEquals(4.0, feeding.amountOz)
    }

    @Test
    fun `addDiaper adds one Diaper with changeType wet`() {
        val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
        val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)

        viewModel.addDiaper()

        val events = viewModel.uiState.value.events
        assertEquals(1, events.size)
        val diaper = events[0] as CatchUpBatchEvent.Diaper
        assertEquals("wet", diaper.changeType)
    }

    @Test
    fun `addNap adds one Nap`() {
        val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
        val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)

        viewModel.addNap()

        val events = viewModel.uiState.value.events
        assertEquals(1, events.size)
        assertTrue(events[0] is CatchUpBatchEvent.Nap)
    }

    @Test
    fun `removeAt valid index removes event`() {
        val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
        val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)
        viewModel.addFeeding()
        viewModel.addDiaper()
        assertEquals(2, viewModel.uiState.value.events.size)

        viewModel.removeAt(0)

        assertEquals(1, viewModel.uiState.value.events.size)
        assertTrue(viewModel.uiState.value.events[0] is CatchUpBatchEvent.Diaper)
    }

    @Test
    fun `removeAt invalid index does nothing`() {
        val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
        val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)
        viewModel.addFeeding()
        assertEquals(1, viewModel.uiState.value.events.size)

        viewModel.removeAt(1)
        viewModel.removeAt(-1)

        assertEquals(1, viewModel.uiState.value.events.size)
    }

    @Test
    fun `submit with empty events sets error and does not call API`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
            val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)

            viewModel.submit({})
            advanceUntilIdle()

            assertEquals("Add at least one event.", viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.submitSuccess)
        }

    @Test
    fun `submit with childId 0 does not call API`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "0"))
            val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)
            viewModel.addFeeding()

            viewModel.submit({})
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.events.size)
            assertFalse(viewModel.uiState.value.submitSuccess)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `submit success clears events sets submitSuccess and invokes onSuccess`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
            whenever(batchRepository.createBatch(eq(1), any()))
                .thenReturn(Result.success(BatchResponse(created = emptyList(), count = 1)))
            val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)
            viewModel.addFeeding()
            var onSuccessCalled = false

            viewModel.submit { onSuccessCalled = true }
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.events.isEmpty())
            assertTrue(viewModel.uiState.value.submitSuccess)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.error)
            assertTrue(onSuccessCalled)
        }

    @Test
    fun `submit failure sets error and isLoading false`() =
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
            whenever(batchRepository.createBatch(eq(1), any()))
                .thenReturn(Result.failure(Exception("Server error")))
            val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)
            viewModel.addFeeding()

            viewModel.submit({})
            advanceUntilIdle()

            assertEquals("Server error", viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `clearError sets error to null`() {
        val savedStateHandle = SavedStateHandle(mapOf("childId" to "1"))
        val viewModel = CatchUpViewModel(batchRepository, savedStateHandle)
        viewModel.submit({})
        assertEquals("Add at least one event.", viewModel.uiState.value.error)

        viewModel.clearError()

        assertNull(viewModel.uiState.value.error)
    }
}
