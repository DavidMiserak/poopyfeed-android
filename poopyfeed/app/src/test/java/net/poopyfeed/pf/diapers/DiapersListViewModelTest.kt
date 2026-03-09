package net.poopyfeed.pf.diapers

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import org.junit.Before
import org.junit.Test

class DiapersListViewModelTest {

  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedDiapersRepository
  private lateinit var viewModel: DiapersListViewModel

  @Before
  fun setup() {
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
  }

  @Test
  fun `pagingData flow is exposed from repository`() {
    val pagingData: Flow<PagingData<Diaper>> = flowOf()
    every { mockRepository.pagedDiapers(1) } returns pagingData

    viewModel = DiapersListViewModel(savedStateHandle, mockRepository)

    assert(viewModel.pagingData == pagingData)
  }

  @Test
  fun `deleteError flow is initialized`() {
    every { mockRepository.pagedDiapers(1) } returns flowOf()

    viewModel = DiapersListViewModel(savedStateHandle, mockRepository)

    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `deleteDiaper method exists`() {
    every { mockRepository.pagedDiapers(1) } returns flowOf()

    viewModel = DiapersListViewModel(savedStateHandle, mockRepository)

    // Should not throw
    viewModel.deleteDiaper(10)
  }
}
