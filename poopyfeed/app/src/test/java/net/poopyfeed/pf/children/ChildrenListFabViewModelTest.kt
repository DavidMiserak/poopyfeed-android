package net.poopyfeed.pf.children

import android.content.Context
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
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
class ChildrenListFabViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var sharingRepository: SharingRepository
  private lateinit var viewModel: ChildrenListFabViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    sharingRepository = mockk()
    every { mockContext.getString(any()) } returns "Invalid token"
    viewModel = ChildrenListFabViewModel(sharingRepository, mockContext)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `acceptByToken success emits navigateToChildId`() =
      runTest(testDispatcher) {
        coEvery { sharingRepository.acceptInvite("valid-token") } returns
            ApiResult.Success(TestFixtures.mockChild(id = 5, name = "Baby"))

        val childIds = mutableListOf<Int>()
        val job = launch { viewModel.navigateToChildId.collect { childIds.add(it) } }
        viewModel.acceptByToken("valid-token")
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assertEquals(listOf(5), childIds)
      }

  @Test
  fun `acceptByToken error emits errorMessage`() =
      runTest(testDispatcher) {
        coEvery { sharingRepository.acceptInvite("bad") } returns
            ApiResult.Error(net.poopyfeed.pf.data.models.ApiError.HttpError(400, "Bad", "Invalid"))

        val messages = mutableListOf<String>()
        val job = launch { viewModel.errorMessage.collect { messages.add(it) } }
        viewModel.acceptByToken("bad")
        advanceUntilIdle()
        job.cancel()
        advanceUntilIdle()

        assert(messages.isNotEmpty())
      }

  @Test
  fun `acceptByToken blank does not call repo`() =
      runTest(testDispatcher) {
        viewModel.acceptByToken("")
        advanceUntilIdle()

        assertEquals(false, viewModel.isSubmitting.value)
      }

  @Test
  fun `acceptByToken when repo returns Loading does not emit navigate or error`() =
      runTest(testDispatcher) {
        coEvery { sharingRepository.acceptInvite("token") } returns ApiResult.Loading()

        val childIds = mutableListOf<Int>()
        val messages = mutableListOf<String>()
        val childJob = launch { viewModel.navigateToChildId.collect { childIds.add(it) } }
        val msgJob = launch { viewModel.errorMessage.collect { messages.add(it) } }
        viewModel.acceptByToken("token")
        advanceUntilIdle()
        childJob.cancel()
        msgJob.cancel()

        assertEquals(emptyList<Int>(), childIds)
        assertEquals(emptyList<String>(), messages)
        assertEquals(false, viewModel.isSubmitting.value)
      }
}
