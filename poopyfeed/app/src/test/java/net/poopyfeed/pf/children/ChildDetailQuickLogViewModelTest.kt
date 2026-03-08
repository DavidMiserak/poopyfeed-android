package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
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
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.sync.SyncScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChildDetailQuickLogViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var childrenRepo: CachedChildrenRepository
  private lateinit var diapersRepo: CachedDiapersRepository
  private lateinit var feedingsRepo: CachedFeedingsRepository
  private lateinit var napsRepo: CachedNapsRepository
  private lateinit var mockSyncScheduler: SyncScheduler
  private lateinit var viewModel: ChildDetailQuickLogViewModel

  private val childId = 1

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to childId))
    childrenRepo = mockk()
    diapersRepo = mockk()
    feedingsRepo = mockk()
    napsRepo = mockk()
    mockSyncScheduler = mockk(relaxed = true)
    every { mockContext.getString(any()) } returns "Error message"
    every { childrenRepo.getChildCached(childId) } returns
        flowOf(TestFixtures.mockChild(id = childId))
    viewModel =
        ChildDetailQuickLogViewModel(
            savedStateHandle,
            childrenRepo,
            diapersRepo,
            feedingsRepo,
            napsRepo,
            mockSyncScheduler,
            mockContext,
        )
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `createDiaperNow success emits Success state`() =
      runTest(testDispatcher) {
        coEvery { diapersRepo.createDiaper(childId, any()) } returns
            ApiResult.Success(TestFixtures.mockDiaper())

        viewModel.createDiaperNow("both")
        advanceUntilIdle()

        assertIs<QuickLogDiaperUiState.Success>(viewModel.diaperState.value)
      }

  @Test
  fun `createDiaperNow error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { diapersRepo.createDiaper(childId, any()) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("fail"))

        viewModel.createDiaperNow("wet")
        advanceUntilIdle()

        assertIs<QuickLogDiaperUiState.Error>(viewModel.diaperState.value)
      }

  @Test
  fun `createDiaperNow blank changeType does nothing`() =
      runTest(testDispatcher) {
        viewModel.createDiaperNow("")
        advanceUntilIdle()

        assertIs<QuickLogDiaperUiState.Idle>(viewModel.diaperState.value)
      }

  @Test
  fun `createBottleNow success emits Success state`() =
      runTest(testDispatcher) {
        coEvery { feedingsRepo.createFeeding(childId, any()) } returns
            ApiResult.Success(TestFixtures.mockFeeding())

        viewModel.createBottleNow(4.0)
        advanceUntilIdle()

        assertIs<QuickLogFeedingUiState.Success>(viewModel.feedingState.value)
      }

  @Test
  fun `createBottleNow error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { feedingsRepo.createFeeding(childId, any()) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.HttpError(500, "Error", "Server"))

        viewModel.createBottleNow(2.0)
        advanceUntilIdle()

        assertIs<QuickLogFeedingUiState.Error>(viewModel.feedingState.value)
      }

  @Test
  fun `createBottleNow zero amount does nothing`() =
      runTest(testDispatcher) {
        viewModel.createBottleNow(0.0)
        advanceUntilIdle()

        assertIs<QuickLogFeedingUiState.Idle>(viewModel.feedingState.value)
      }

  @Test
  fun `createNapNow success emits Success state`() =
      runTest(testDispatcher) {
        coEvery { napsRepo.createNap(childId, any()) } returns
            ApiResult.Success(TestFixtures.mockNap(end_time = null))

        viewModel.createNapNow()
        advanceUntilIdle()

        assertIs<QuickLogNapUiState.Success>(viewModel.napState.value)
      }

  @Test
  fun `createNapNow error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { napsRepo.createNap(childId, any()) } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.NetworkError("offline"))

        viewModel.createNapNow()
        advanceUntilIdle()

        assertIs<QuickLogNapUiState.Error>(viewModel.napState.value)
      }

  @Test
  fun `createDiaperNow when repo returns Loading keeps Saving`() =
      runTest(testDispatcher) {
        coEvery { diapersRepo.createDiaper(childId, any()) } returns ApiResult.Loading()

        viewModel.createDiaperNow("wet")
        advanceUntilIdle()

        assertIs<QuickLogDiaperUiState.Saving>(viewModel.diaperState.value)
      }

  @Test
  fun `createBottleNow when repo returns Loading keeps Saving`() =
      runTest(testDispatcher) {
        coEvery { feedingsRepo.createFeeding(childId, any()) } returns ApiResult.Loading()

        viewModel.createBottleNow(3.0)
        advanceUntilIdle()

        assertIs<QuickLogFeedingUiState.Saving>(viewModel.feedingState.value)
      }

  @Test
  fun `createNapNow when repo returns Loading keeps Saving`() =
      runTest(testDispatcher) {
        coEvery { napsRepo.createNap(childId, any()) } returns ApiResult.Loading()

        viewModel.createNapNow()
        advanceUntilIdle()

        assertIs<QuickLogNapUiState.Saving>(viewModel.napState.value)
      }

  @Test
  fun `bottleAmounts defaults when child is null`() =
      runTest(testDispatcher) {
        every { childrenRepo.getChildCached(childId) } returns flowOf(null)
        viewModel =
            ChildDetailQuickLogViewModel(
                savedStateHandle,
                childrenRepo,
                diapersRepo,
                feedingsRepo,
                napsRepo,
                mockSyncScheduler,
                mockContext,
            )
        advanceUntilIdle()

        assertEquals(BottleAmounts(2.0, 4.0, 6.0), viewModel.bottleAmounts.value)
      }

  @Test
  fun `bottleAmounts defaults when getChildCached flow throws`() =
      runTest(testDispatcher) {
        every { childrenRepo.getChildCached(childId) } returns
            flow { throw RuntimeException("err") }
        viewModel =
            ChildDetailQuickLogViewModel(
                savedStateHandle,
                childrenRepo,
                diapersRepo,
                feedingsRepo,
                napsRepo,
                mockSyncScheduler,
                mockContext,
            )
        advanceUntilIdle()

        assertEquals(BottleAmounts(2.0, 4.0, 6.0), viewModel.bottleAmounts.value)
      }

  @Test
  fun `bottleAmounts uses child custom bottle oz when set`() =
      runTest(testDispatcher) {
        val child =
            TestFixtures.mockChild(
                id = childId,
                custom_bottle_low_oz = "3",
                custom_bottle_mid_oz = "5",
                custom_bottle_high_oz = "7",
            )
        every { childrenRepo.getChildCached(childId) } returns flowOf(child)
        viewModel =
            ChildDetailQuickLogViewModel(
                savedStateHandle,
                childrenRepo,
                diapersRepo,
                feedingsRepo,
                napsRepo,
                mockSyncScheduler,
                mockContext,
            )
        val values = mutableListOf<BottleAmounts>()
        val job = launch { viewModel.bottleAmounts.collect { values.add(it) } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(BottleAmounts(3.0, 5.0, 7.0), values.last())
      }

  @Test
  fun `bottleAmounts uses default 2 when child custom bottle oz is null`() =
      runTest(testDispatcher) {
        val child =
            TestFixtures.mockChild(
                id = childId,
                custom_bottle_low_oz = "3",
                custom_bottle_mid_oz = null,
                custom_bottle_high_oz = "7",
            )
        every { childrenRepo.getChildCached(childId) } returns flowOf(child)
        viewModel =
            ChildDetailQuickLogViewModel(
                savedStateHandle,
                childrenRepo,
                diapersRepo,
                feedingsRepo,
                napsRepo,
                mockSyncScheduler,
                mockContext,
            )
        val values = mutableListOf<BottleAmounts>()
        val job = launch { viewModel.bottleAmounts.collect { values.add(it) } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(BottleAmounts(3.0, 2.0, 7.0), values.last())
      }
}
