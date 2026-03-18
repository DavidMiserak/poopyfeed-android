package net.poopyfeed.pf.diapers

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
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.ui.toast.ToastManager

@HiltViewModel
class DiapersListViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedDiapersRepository,
    val analyticsTracker: AnalyticsTracker,
    val toastManager: ToastManager,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  val pagingData: Flow<PagingData<Diaper>> = repo.pagedDiapers(childId)

  private val _deleteError: MutableStateFlow<String?> = MutableStateFlow(null)
  val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

  fun refresh() {
    viewModelScope.launch {
      val result = repo.refreshDiapers(childId)
      if (result is ApiResult.Success) {
        toastManager.showSuccess("✓ Synced")
      }
    }
  }

  fun deleteDiaper(diaperId: Int) {
    viewModelScope.launch {
      when (repo.deleteDiaper(childId, diaperId)) {
        is ApiResult.Success -> _deleteError.value = null
        is ApiResult.Error -> _deleteError.value = "Failed to delete diaper"
        is ApiResult.Loading -> {}
      }
    }
  }
}
