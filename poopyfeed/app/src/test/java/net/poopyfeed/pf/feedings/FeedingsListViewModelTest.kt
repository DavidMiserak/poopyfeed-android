package net.poopyfeed.pf.feedings

import androidx.lifecycle.SavedStateHandle
import androidx.paging.PagingData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import org.junit.Before
import org.junit.Test

class FeedingsListViewModelTest {

  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedFeedingsRepository
  private lateinit var viewModel: FeedingsListViewModel

  @Before
  fun setup() {
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
  }

  @Test
  fun `pagingData flow is exposed from repository`() {
    val pagingData: Flow<PagingData<Feeding>> = flowOf()
    every { mockRepository.pagedFeedings(1) } returns pagingData

    viewModel = FeedingsListViewModel(savedStateHandle, mockRepository)

    assert(viewModel.pagingData == pagingData)
  }

  @Test
  fun `deleteError flow is initialized`() {
    every { mockRepository.pagedFeedings(1) } returns flowOf()

    viewModel = FeedingsListViewModel(savedStateHandle, mockRepository)

    assert(viewModel.deleteError.value == null)
  }

  @Test
  fun `deleteFeeding method exists`() {
    every { mockRepository.pagedFeedings(1) } returns flowOf()

    viewModel = FeedingsListViewModel(savedStateHandle, mockRepository)

    // Should not throw
    viewModel.deleteFeeding(10)
  }
}
