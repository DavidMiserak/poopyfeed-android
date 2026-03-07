package net.poopyfeed.pf.naps

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.models.UpdateNapRequest
import net.poopyfeed.pf.data.repository.CachedNapsRepository

/** UI state for the edit nap bottom sheet. */
sealed interface EditNapUiState {
  data object Loading : EditNapUiState

  data class Ready(val nap: Nap) : EditNapUiState

  data class Error(val message: String) : EditNapUiState

  data object Saving : EditNapUiState

  data object Success : EditNapUiState

  data class SaveError(val message: String) : EditNapUiState
}

/**
 * ViewModel for [EditNapBottomSheetFragment]. Loads nap for prefill, saves start/end time updates.
 */
@HiltViewModel
class EditNapViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedNapsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])
  private val napId: Int = checkNotNull(savedStateHandle["napId"])

  private val _uiState: MutableStateFlow<EditNapUiState> = MutableStateFlow(EditNapUiState.Loading)
  val uiState: StateFlow<EditNapUiState> = _uiState.asStateFlow()

  init {
    loadNap()
  }

  private fun loadNap() {
    viewModelScope.launch {
      val nap = repo.getNap(childId, napId)
      _uiState.value =
          if (nap != null) EditNapUiState.Ready(nap) else EditNapUiState.Error("Nap not found.")
    }
  }

  fun saveNap(startTime: String, endTime: String?) {
    viewModelScope.launch {
      _uiState.value = EditNapUiState.Saving
      val request = UpdateNapRequest(start_time = startTime, end_time = endTime)
      val result = repo.updateNap(childId, napId, request)
      _uiState.value =
          when (result) {
            is ApiResult.Success -> EditNapUiState.Success
            is ApiResult.Error -> EditNapUiState.SaveError(result.error.getUserMessage(context))
            is ApiResult.Loading -> EditNapUiState.Saving
          }
    }
  }
}
