package net.poopyfeed.pf.naps

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import org.junit.Before
import org.junit.Test

class NapsListViewModelTest {

  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedNapsRepository
  private lateinit var viewModel: NapsListViewModel

  @Before
  fun setup() {
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
  }

  @Test
  fun `pagingData flow is exposed from repository`() {
    val pagingData: Flow<PagingData<Nap>> = flowOf()
    every { mockRepository.pagedNaps(1) } returns pagingData

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)

    assert(viewModel.pagingData == pagingData)
  }

  @Test
  fun `deleteError flow is initialized`() {
    every { mockRepository.pagedNaps(1) } returns flowOf()

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)

    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `deleteNap method exists`() {
    every { mockRepository.pagedNaps(1) } returns flowOf()

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)

    // Should not throw
    viewModel.deleteNap(10)
  }

  @Test
  fun `endNap method exists`() {
    every { mockRepository.pagedNaps(1) } returns flowOf()

    viewModel = NapsListViewModel(savedStateHandle, mockRepository)

    // Should not throw
    viewModel.endNap(10)
  }
}
