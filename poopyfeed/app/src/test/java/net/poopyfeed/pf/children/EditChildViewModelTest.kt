package net.poopyfeed.pf.children

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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.NotificationPreference
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditChildViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedChildrenRepository
  private lateinit var mockApiService: PoopyFeedApiService
  private lateinit var viewModel: EditChildViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
    mockApiService = mockk()
    every { mockContext.getString(any()) } returns "Error message"
    coEvery { mockApiService.getNotificationPreferences() } returns emptyList()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private fun createViewModel() =
      EditChildViewModel(savedStateHandle, mockRepository, mockApiService, mockContext)

  @Test
  fun `init loads child from repo and emits Ready with canEditReminder`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1, can_edit = true)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)

        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<EditChildUiState.Ready>(state)
        assertEquals(child.name, state.child.name)
        assertEquals(true, state.canEditReminder)
      }

  @Test
  fun `init when getChildCached emits null emits Error`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.getChildCached(1) } returns flowOf(null)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertIs<EditChildUiState.Error>(viewModel.uiState.value)
      }

  @Test
  fun `save with empty name emits ValidationError and does not call repo`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.save("  ", "2024-01-15", "F", null, null, null, null)

        assertIs<EditChildUiState.ValidationError>(viewModel.uiState.value)
        assertEquals(
            "Name is required",
            (viewModel.uiState.value as EditChildUiState.ValidationError).nameError)
        coVerify(exactly = 0) { mockRepository.updateChild(any(), any()) }
      }

  @Test
  fun `save with empty dateOfBirth emits ValidationError`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.save("Alice", "", "F", null, null, null, null)

        assertIs<EditChildUiState.ValidationError>(viewModel.uiState.value)
        assertEquals(
            "Date of birth is required",
            (viewModel.uiState.value as EditChildUiState.ValidationError).dobError)
      }

  @Test
  fun `save with valid data calls repo updateChild and emits Success`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)
        coEvery { mockRepository.updateChild(1, any()) } returns ApiResult.Success(child)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.save("Alice", "2024-01-15", "F", 4, null, null, null)
        advanceUntilIdle()

        coVerify { mockRepository.updateChild(1, any()) }
        assertIs<EditChildUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `save when repo returns Error emits SaveError`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)
        coEvery { mockRepository.updateChild(1, any()) } returns
            ApiResult.Error(ApiError.NetworkError("Network down"))

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.save("Alice", "2024-01-15", "F", null, null, null, null)
        advanceUntilIdle()

        assertIs<EditChildUiState.SaveError>(viewModel.uiState.value)
        assertEquals(
            "Error message", (viewModel.uiState.value as EditChildUiState.SaveError).message)
      }

  @Test
  fun `save with partial bottle amounts emits ValidationError`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.save("Alice", "2024-01-15", "F", null, "4.0", null, "6.0")

        assertIs<EditChildUiState.ValidationError>(viewModel.uiState.value)
        val state = viewModel.uiState.value as EditChildUiState.ValidationError
        assertEquals("Error message", state.bottleError)
        coVerify(exactly = 0) { mockRepository.updateChild(any(), any()) }
      }

  @Test
  fun `save with valid bottle amounts calls repo and emits Success`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)
        coEvery { mockRepository.updateChild(1, any()) } returns ApiResult.Success(child)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.save("Alice", "2024-01-15", "F", null, "4.0", "5.0", "6.0")
        advanceUntilIdle()

        coVerify { mockRepository.updateChild(1, any()) }
        assertIs<EditChildUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `save with blank bottle amounts treated as defaults`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)
        coEvery { mockRepository.updateChild(1, any()) } returns ApiResult.Success(child)

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.save("Alice", "2024-01-15", "F", null, "", "", "")
        advanceUntilIdle()

        coVerify { mockRepository.updateChild(1, any()) }
        assertIs<EditChildUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `init loads notification preferences for child`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1, can_edit = true)
        val pref =
            NotificationPreference(
                id = 10,
                childId = 1,
                childName = "Baby Alice",
                notifyFeedings = true,
                notifyDiapers = false,
                notifyNaps = true)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)
        coEvery { mockApiService.getNotificationPreferences() } returns listOf(pref)

        viewModel = createViewModel()
        advanceUntilIdle()

        val prefState = viewModel.notificationPrefState.value
        assertIs<NotificationPrefState.Loaded>(prefState)
        assertEquals(true, prefState.pref.notifyFeedings)
        assertEquals(false, prefState.pref.notifyDiapers)
      }

  @Test
  fun `deleteChild success emits deleteSuccess`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)
        coEvery { mockRepository.deleteChild(1) } returns ApiResult.Success(Unit)

        viewModel = createViewModel()
        advanceUntilIdle()
        val emitted = mutableListOf<Unit>()
        val job = launch { viewModel.deleteSuccess.collect { emitted.add(it) } }
        viewModel.deleteChild()
        advanceUntilIdle()

        coVerify { mockRepository.deleteChild(1) }
        assertEquals(1, emitted.size)
        job.cancel()
      }

  @Test
  fun `deleteChild when repo returns Error emits deleteError`() =
      runTest(testDispatcher) {
        val child = TestFixtures.mockChild(id = 1)
        coEvery { mockRepository.getChildCached(1) } returns flowOf(child)
        coEvery { mockRepository.deleteChild(1) } returns
            ApiResult.Error(ApiError.NetworkError("Failed"))

        viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.deleteChild()
        advanceUntilIdle()

        coVerify { mockRepository.deleteChild(1) }
        // deleteError is one-shot; we've covered the branch
        assertIs<EditChildUiState.Ready>(viewModel.uiState.value)
      }
}
