package net.poopyfeed.pf.naps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.models.UpdateNapRequest
import net.poopyfeed.pf.data.repository.CachedNapsRepository

@HiltViewModel
class NapsListViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedNapsRepository,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  val pagingData: Flow<PagingData<Nap>> = repo.pagedNaps(childId)

  private val _deleteError: MutableStateFlow<String?> = MutableStateFlow(null)
  val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

  fun deleteNap(napId: Int) {
    viewModelScope.launch {
      when (repo.deleteNap(childId, napId)) {
        is ApiResult.Success -> _deleteError.value = null
        is ApiResult.Error -> _deleteError.value = "Failed to delete nap"
        is ApiResult.Loading -> {}
      }
    }
  }

  fun endNap(napId: Int) {
    viewModelScope.launch {
      val now = Instant.now().toString()
      when (val result = repo.updateNap(childId, napId, UpdateNapRequest(end_time = now))) {
        is ApiResult.Success -> _deleteError.value = null
        is ApiResult.Error -> _deleteError.value = "Failed to end nap"
        is ApiResult.Loading -> {}
      }
    }
  }
}
