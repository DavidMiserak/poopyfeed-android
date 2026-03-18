package net.poopyfeed.pf.feedings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.ui.toast.ToastManager

/**
 * ViewModel for [FeedingsListFragment]. Exposes paginated feedings for a child with delete support.
 */
@HiltViewModel
class FeedingsListViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedFeedingsRepository,
    val analyticsTracker: AnalyticsTracker,
    val toastManager: ToastManager,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  val pagingData: Flow<PagingData<Feeding>> = repo.pagedFeedings(childId)

  private val _deleteError: MutableStateFlow<String?> = MutableStateFlow(null)
  val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

  fun refresh() {
    viewModelScope.launch {
      val result = repo.refreshFeedings(childId)
      if (result is ApiResult.Success) {
        toastManager.showSuccess("✓ Synced")
      }
    }
  }

  fun deleteFeeding(feedingId: Int) {
    viewModelScope.launch {
      when (repo.deleteFeeding(childId, feedingId)) {
        is ApiResult.Success -> _deleteError.value = null
        is ApiResult.Error -> _deleteError.value = "Failed to delete feeding"
        is ApiResult.Loading -> {}
      }
    }
  }
}
