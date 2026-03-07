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
  private lateinit var viewModel: SharingViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    mockSharingRepository = mockk()
    savedStateHandle = mockk()
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
                savedStateHandle,
                mockSharingRepository,
                mockContext,
            )
        advanceUntilIdle()

        assertIs<SharingUiState.Ready>(viewModel.uiState.value)
        val ready = viewModel.uiState.value as SharingUiState.Ready
        assertTrue(ready.items.size >= 2)
        val shareRow = ready.items.filterIsInstance<SharingListItem.ShareRow>().single()
        assertTrue(shareRow.share.id == 1)
      }

  @Test
  fun `init when listInvites fails emits Error`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.listInvites(7) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("Network down"))
        coEvery { mockSharingRepository.listShares(7) } returns ApiResult.Success(emptyList())

        viewModel =
            SharingViewModel(
                savedStateHandle,
                mockSharingRepository,
                mockContext,
            )
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
                savedStateHandle,
                mockSharingRepository,
                mockContext,
            )
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
                savedStateHandle,
                mockSharingRepository,
                mockContext,
            )
        advanceUntilIdle()
        viewModel.refresh()
        advanceUntilIdle()

        io.mockk.coVerify(atLeast = 2) { mockSharingRepository.listInvites(7) }
        io.mockk.coVerify(atLeast = 2) { mockSharingRepository.listShares(7) }
      }
}
