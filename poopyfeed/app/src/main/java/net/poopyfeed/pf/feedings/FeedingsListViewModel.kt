package net.poopyfeed.pf.feedings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import javax.inject.Inject

/**
 * ViewModel for [FeedingsListFragment]. Exposes paginated feedings for a child with
 * delete support.
 */
@HiltViewModel
class FeedingsListViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedFeedingsRepository,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  val pagingData: Flow<PagingData<Feeding>> = repo.pagedFeedings(childId)

  private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  fun deleteFeeding(feedingId: Int) {
    // TODO: Implement delete with error handling (Task 13)
  }
}
