package net.poopyfeed.pf.diapers

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
import net.poopyfeed.pf.data.models.CreateDiaperRequest
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.repository.CachedDiapersRepository

/** UI state for the edit diaper bottom sheet. */
sealed interface EditDiaperUiState {
  data object Loading : EditDiaperUiState

  data class Ready(val diaper: Diaper) : EditDiaperUiState

  data class Error(val message: String) : EditDiaperUiState

  data object Saving : EditDiaperUiState

  data object Success : EditDiaperUiState

  data class SaveError(val message: String) : EditDiaperUiState

  data class ValidationError(val typeError: String? = null) : EditDiaperUiState
}

/**
 * ViewModel for [EditDiaperBottomSheetFragment]. Loads diaper for prefill, validates and saves
 * updates.
 */
@HiltViewModel
class EditDiaperViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedDiapersRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])
  private val diaperId: Int = checkNotNull(savedStateHandle["diaperId"])

  private val _uiState: MutableStateFlow<EditDiaperUiState> =
      MutableStateFlow(EditDiaperUiState.Loading)
  val uiState: StateFlow<EditDiaperUiState> = _uiState.asStateFlow()

  init {
    loadDiaper()
  }

  private fun loadDiaper() {
    viewModelScope.launch {
      val diaper = repo.getDiaper(childId, diaperId)
      _uiState.value =
          if (diaper != null) EditDiaperUiState.Ready(diaper)
          else EditDiaperUiState.Error("Diaper change not found.")
    }
  }

  fun saveDiaper(changeType: String, timestamp: String) {
    val typeError = if (changeType.isBlank()) "Select change type." else null
    if (typeError != null) {
      _uiState.value = EditDiaperUiState.ValidationError(typeError = typeError)
      return
    }

    viewModelScope.launch {
      _uiState.value = EditDiaperUiState.Saving
      val request = CreateDiaperRequest(change_type = changeType.trim(), timestamp = timestamp)
      val result = repo.updateDiaper(childId, diaperId, request)
      _uiState.value =
          when (result) {
            is ApiResult.Success -> EditDiaperUiState.Success
            is ApiResult.Error -> EditDiaperUiState.SaveError(result.error.getUserMessage(context))
            is ApiResult.Loading -> EditDiaperUiState.Saving
          }
    }
  }
}
