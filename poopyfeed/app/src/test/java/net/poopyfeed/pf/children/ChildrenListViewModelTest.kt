package net.poopyfeed.pf.children

import android.content.Context
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import org.junit.Before
import org.junit.Test

/** Unit tests for ChildrenListViewModel. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChildrenListViewModelTest {

  private lateinit var mockContext: Context
  private lateinit var mockRepository: CachedChildrenRepository
  private lateinit var viewModel: ChildrenListViewModel

  @Before
  fun setup() {
    mockContext = mockk()
    mockRepository = mockk()
    every { mockContext.getString(any()) } returns "Error message"
  }

  @Test
  fun `init calls refresh on start`() = runTest {
    coEvery { mockRepository.listChildrenCached() } returns flowOf(ApiResult.Success(emptyList()))
    coEvery { mockRepository.hasSyncedFlow } returns flowOf(true)
    coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())

    viewModel = ChildrenListViewModel(mockRepository, mockContext)

    // Verify refresh was called during init
    coVerify { mockRepository.refreshChildren() }
  }

  @Test
  fun `refresh calls repository refresh`() = runTest {
    coEvery { mockRepository.listChildrenCached() } returns flowOf(ApiResult.Success(emptyList()))
    coEvery { mockRepository.hasSyncedFlow } returns flowOf(true)
    coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())

    viewModel = ChildrenListViewModel(mockRepository, mockContext)

    viewModel.refresh()

    coVerify(exactly = 2) { mockRepository.refreshChildren() } // Called in init and refresh()
  }

  @Test
  fun `deleteChild calls repository delete`() = runTest {
    coEvery { mockRepository.listChildrenCached() } returns flowOf(ApiResult.Success(emptyList()))
    coEvery { mockRepository.hasSyncedFlow } returns flowOf(true)
    coEvery { mockRepository.refreshChildren() } returns ApiResult.Success(emptyList())
    coEvery { mockRepository.deleteChild(1) } returns ApiResult.Success(Unit)

    viewModel = ChildrenListViewModel(mockRepository, mockContext)

    viewModel.deleteChild(1)

    coVerify { mockRepository.deleteChild(1) }
  }
}
