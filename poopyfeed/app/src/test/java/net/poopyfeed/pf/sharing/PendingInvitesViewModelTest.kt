package net.poopyfeed.pf.sharing

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertIs
import kotlin.test.assertTrue
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
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.data.repository.ChildrenRepository
import net.poopyfeed.pf.data.repository.SharingRepository
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingInvitesViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var mockSharingRepository: SharingRepository
  private lateinit var mockChildrenRepository: ChildrenRepository
  private lateinit var mockCachedChildrenRepository: CachedChildrenRepository
  private lateinit var viewModel: PendingInvitesViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    mockSharingRepository = mockk()
    mockChildrenRepository = mockk()
    mockCachedChildrenRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `init with pending invites loads and resolves child names`() =
      runTest(testDispatcher) {
        val invite = TestFixtures.mockShareInvite(id = 1, child = 5)
        coEvery { mockSharingRepository.getPendingInvites() } returns
            ApiResult.Success(listOf(invite))
        every { mockChildrenRepository.getChild(5) } returns
            flowOf(ApiResult.Success(TestFixtures.mockChild(id = 5, name = "Baby Sam")))

        viewModel =
            PendingInvitesViewModel(
                mockSharingRepository,
                mockChildrenRepository,
                mockCachedChildrenRepository,
                mockContext,
            )
        advanceUntilIdle()

        assertIs<PendingInvitesUiState.Ready>(viewModel.uiState.value)
        val ready = viewModel.uiState.value as PendingInvitesUiState.Ready
        assertTrue(ready.invites.size == 1)
        assertTrue(ready.invites[0].childName == "Baby Sam")
        assertTrue(ready.invites[0].invite.id == 1)
      }

  @Test
  fun `init with empty invites emits Empty`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.getPendingInvites() } returns ApiResult.Success(emptyList())

        viewModel =
            PendingInvitesViewModel(
                mockSharingRepository,
                mockChildrenRepository,
                mockCachedChildrenRepository,
                mockContext,
            )
        advanceUntilIdle()

        assertIs<PendingInvitesUiState.Empty>(viewModel.uiState.value)
      }

  @Test
  fun `init when getPendingInvites fails emits Error`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.getPendingInvites() } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("Network down"))

        viewModel =
            PendingInvitesViewModel(
                mockSharingRepository,
                mockChildrenRepository,
                mockCachedChildrenRepository,
                mockContext,
            )
        advanceUntilIdle()

        assertIs<PendingInvitesUiState.Error>(viewModel.uiState.value)
      }

  @Test
  fun `acceptByToken success refreshes cache and emits navigateToChild`() =
      runTest(testDispatcher) {
        coEvery { mockSharingRepository.getPendingInvites() } returns ApiResult.Success(emptyList())
        coEvery { mockSharingRepository.acceptInvite("the-token") } returns
            ApiResult.Success(TestFixtures.mockChild(id = 3, name = "Baby"))
        coEvery { mockCachedChildrenRepository.refreshChildren() } returns
            ApiResult.Success(emptyList())

        viewModel =
            PendingInvitesViewModel(
                mockSharingRepository,
                mockChildrenRepository,
                mockCachedChildrenRepository,
                mockContext,
            )
        advanceUntilIdle()

        val collected = mutableListOf<Int>()
        val job = launch { viewModel.navigateToChild.collect { collected.add(it) } }
        viewModel.acceptByToken("the-token")
        advanceUntilIdle()

        coVerify { mockSharingRepository.acceptInvite("the-token") }
        coVerify { mockCachedChildrenRepository.refreshChildren() }
        assertTrue(collected.size == 1 && collected[0] == 3)
        job.cancel()
      }
}
