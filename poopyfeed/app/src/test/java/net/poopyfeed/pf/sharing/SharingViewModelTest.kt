package net.poopyfeed.pf.sharing

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.SharingRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharingViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var mockSharingRepository: SharingRepository
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockAnalyticsTracker: net.poopyfeed.pf.analytics.AnalyticsTracker
  private lateinit var viewModel: SharingViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    mockSharingRepository = mockk()
    savedStateHandle = mockk()
    mockAnalyticsTracker = mockk(relaxed = true)
    every { mockContext.getString(any()) } returns "Error message"
    every { savedStateHandle.get<Int>("childId") } returns 7
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init loads invites and shares and emits Ready`() =
      runTest(testDispatcher) {
        val invites = emptyList<net.poopyfeed.pf.data.models.ChildInvite>()
        val shares = listOf(TestFixtures.mockChildShare(id = 1))
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(invites)
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(shares)

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()

        assertIs<SharingUiState.Ready>(viewModel.uiState.value)
        val ready = viewModel.uiState.value as SharingUiState.Ready
        assertTrue(ready.items.size >= 2)
        val shareRow = ready.items.filterIsInstance<SharingListItem.ShareRow>().single()
        assertTrue(shareRow.share.id == 1)
      }

  @Test
  fun `init with invites emits Ready with InviteRow items`() =
      runTest(testDispatcher) {
        val invite =
            net.poopyfeed.pf.data.models.ChildInvite(
                id = 1,
                token = "t1",
                role = "caregiver",
                roleDisplay = "Caregiver",
                isActive = true,
                createdAt = "",
                inviteUrl = null)
        val invites = listOf(invite)
        val shares = emptyList<net.poopyfeed.pf.data.models.ChildShare>()
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(invites)
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(shares)

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()

        assertIs<SharingUiState.Ready>(viewModel.uiState.value)
        val ready = viewModel.uiState.value as SharingUiState.Ready
        val inviteRows = ready.items.filterIsInstance<SharingListItem.InviteRow>()
        assertTrue(inviteRows.size == 1)
        assertTrue(inviteRows[0].invite.id == 1)
      }

  @Test
  fun `init when listInvites fails emits Error`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("Network down"))
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()

        assertIs<SharingUiState.Error>(viewModel.uiState.value)
      }

  @Test
  fun `init when listShares fails emits Error`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("Network down"))

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()

        assertIs<SharingUiState.Error>(viewModel.uiState.value)
      }

  @Test
  fun `refresh calls listInvites and listShares again`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        io.mockk.coVerify(atLeast = 2) { mockSharingRepository.listInvites(7) }
        io.mockk.coVerify(atLeast = 2) { mockSharingRepository.listShares(7) }
      }

  @Test
  fun `toggleInvite when API returns Error emits errorMessage`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())
        val invite =
            net.poopyfeed.pf.data.models.ChildInvite(
                id = 1,
                token = "t1",
                role = "caregiver",
                roleDisplay = "Caregiver",
                isActive = true,
                createdAt = "",
                inviteUrl = null)
        coEvery { mockSharingRepository.toggleInvite(7, 1) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("Toggle failed"))

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()

        val messages = mutableListOf<String>()
        val job = launch { viewModel.errorMessage.collect { messages.add(it) } }
        viewModel.toggleInvite(invite)
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertTrue(messages.isNotEmpty())
      }

  @Test
  fun `deleteInvite when API returns Error emits errorMessage`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())
        val invite =
            net.poopyfeed.pf.data.models.ChildInvite(
                id = 2,
                token = "t2",
                role = "co-parent",
                roleDisplay = "Co-parent",
                isActive = false,
                createdAt = "",
                inviteUrl = null)
        coEvery { mockSharingRepository.deleteInvite(7, 2) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.HttpError(403, "Forbidden", ""))

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()

        val messages = mutableListOf<String>()
        val job = launch { viewModel.errorMessage.collect { messages.add(it) } }
        viewModel.deleteInvite(invite)
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertTrue(messages.isNotEmpty())
      }

  @Test
  fun `toggleInvite when API returns Loading does not emit error`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())
        val invite =
            net.poopyfeed.pf.data.models.ChildInvite(
                id = 1,
                token = "t1",
                role = "caregiver",
                roleDisplay = "Caregiver",
                isActive = true,
                createdAt = "",
                inviteUrl = null)
        coEvery { mockSharingRepository.toggleInvite(7, 1) } returns ApiResult.Loading()

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()
        val messages = mutableListOf<String>()
        val job = launch { viewModel.errorMessage.collect { messages.add(it) } }
        viewModel.toggleInvite(invite)
        advanceUntilIdle()
        job.cancel()
        assertTrue(messages.isEmpty())
      }

  @Test
  fun `load when listInvites returns Loading and listShares Success keeps Loading state`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Loading()
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()

        assertIs<SharingUiState.Loading>(viewModel.uiState.value)
      }

  @Test
  fun `load when listInvites Success and listShares Loading keeps Loading state`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Loading()

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()

        assertIs<SharingUiState.Loading>(viewModel.uiState.value)
      }

  @Test
  fun `toggleInvite when API returns Success triggers refresh`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())
        val invite =
            net.poopyfeed.pf.data.models.ChildInvite(
                id = 1,
                token = "t1",
                role = "caregiver",
                roleDisplay = "Caregiver",
                isActive = true,
                createdAt = "",
                inviteUrl = null)
        coEvery { mockSharingRepository.toggleInvite(7, 1) } returns ApiResult.Success(invite)

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()
        viewModel.toggleInvite(invite)
        advanceUntilIdle()

        io.mockk.coVerify(atLeast = 2) { mockSharingRepository.listInvites(7) }
        io.mockk.coVerify(atLeast = 2) { mockSharingRepository.listShares(7) }
      }

  @Test
  fun `deleteInvite when API returns Success triggers refresh`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())
        val invite =
            net.poopyfeed.pf.data.models.ChildInvite(
                id = 2,
                token = "t2",
                role = "co-parent",
                roleDisplay = "Co-parent",
                isActive = false,
                createdAt = "",
                inviteUrl = null)
        coEvery { mockSharingRepository.deleteInvite(7, 2) } returns ApiResult.Success(Unit)

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()
        viewModel.deleteInvite(invite)
        advanceUntilIdle()

        io.mockk.coVerify(atLeast = 2) { mockSharingRepository.listInvites(7) }
        io.mockk.coVerify(atLeast = 2) { mockSharingRepository.listShares(7) }
      }

  @Test
  fun `deleteInvite when API returns Loading does not emit error`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())
        val invite =
            net.poopyfeed.pf.data.models.ChildInvite(
                id = 2,
                token = "t2",
                role = "co-parent",
                roleDisplay = "Co-parent",
                isActive = false,
                createdAt = "",
                inviteUrl = null)
        coEvery { mockSharingRepository.deleteInvite(7, 2) } returns ApiResult.Loading()

        viewModel =
            SharingViewModel(
                savedStateHandle, mockSharingRepository, mockContext, mockAnalyticsTracker)
        advanceUntilIdle()
        val messages = mutableListOf<String>()
        val job = launch { viewModel.errorMessage.collect { messages.add(it) } }
        viewModel.deleteInvite(invite)
        advanceUntilIdle()
        job.cancel()
        assertTrue(messages.isEmpty())
      }
}
