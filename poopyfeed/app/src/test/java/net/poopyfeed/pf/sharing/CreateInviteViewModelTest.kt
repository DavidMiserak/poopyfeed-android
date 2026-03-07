package net.poopyfeed.pf.sharing

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.ShareInviteResponse
import net.poopyfeed.pf.data.repository.SharingRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateInviteViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var sharingRepository: SharingRepository
  private lateinit var viewModel: CreateInviteViewModel

  private val childId = 7

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to childId))
    sharingRepository = mockk()
    every { mockContext.getString(any()) } returns "Please select a role"
    viewModel =
        CreateInviteViewModel(
            savedStateHandle,
            sharingRepository,
            mockContext,
        )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `setRole updates Ready state`() =
      runTest(testDispatcher) {
        viewModel.setRole("co-parent")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<CreateInviteUiState.Ready>(state)
        assert((state as CreateInviteUiState.Ready).selectedRole == "co-parent")
        assertNull(state.roleError)
      }

  @Test
  fun `submit without role sets roleError`() =
      runTest(testDispatcher) {
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<CreateInviteUiState.Ready>(state)
        assert((state as CreateInviteUiState.Ready).roleError != null)
      }

  @Test
  fun `submit with role success emits InviteCreated`() =
      runTest(testDispatcher) {
        viewModel.setRole("caregiver")
        advanceUntilIdle()
        coEvery { sharingRepository.createShare(childId, any()) } returns
            ApiResult.Success(
                ShareInviteResponse(
                    id = 1,
                    token = "abc123",
                    role = "caregiver",
                    isActive = true,
                    createdAt = "2024-01-01"))

        viewModel.submit()
        advanceUntilIdle()

        assertIs<CreateInviteUiState.InviteCreated>(viewModel.uiState.value)
        assert(
            (viewModel.uiState.value as CreateInviteUiState.InviteCreated).inviteCode == "abc123")
      }

  @Test
  fun `submit with role API error restores Ready and emits errorMessage`() =
      runTest(testDispatcher) {
        viewModel.setRole("co-parent")
        advanceUntilIdle()
        coEvery { sharingRepository.createShare(childId, any()) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.HttpError(500, "Error", "Server"))

        val messages = mutableListOf<String>()
        val job = launch { viewModel.errorMessage.collect { messages.add(it) } }
        viewModel.submit()
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertIs<CreateInviteUiState.Ready>(viewModel.uiState.value)
        assert(messages.isNotEmpty())
      }

  @Test
  fun `submit when API returns Loading keeps Submitting`() =
      runTest(testDispatcher) {
        viewModel.setRole("caregiver")
        advanceUntilIdle()
        coEvery { sharingRepository.createShare(childId, any()) } returns ApiResult.Loading()

        viewModel.submit()
        advanceUntilIdle()

        assertIs<CreateInviteUiState.Submitting>(viewModel.uiState.value)
      }
}
