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
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.sync.SyncScheduler

/** UI state for the create diaper bottom sheet. */
sealed interface CreateDiaperUiState {
  data object Idle : CreateDiaperUiState

  data object Saving : CreateDiaperUiState

  data object Success : CreateDiaperUiState

  data class Error(val message: String) : CreateDiaperUiState

  data class ValidationError(val typeError: String? = null) : CreateDiaperUiState
}

/** ViewModel for [CreateDiaperBottomSheetFragment]. Handles validation and diaper creation. */
@HiltViewModel
class CreateDiaperViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedDiapersRepository,
    private val syncScheduler: SyncScheduler,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<CreateDiaperUiState> =
      MutableStateFlow(CreateDiaperUiState.Idle)
  val uiState: StateFlow<CreateDiaperUiState> = _uiState.asStateFlow()

  fun createDiaper(changeType: String, timestamp: String) {
    val typeError = if (changeType.isBlank()) "Select change type." else null
    if (typeError != null) {
      _uiState.value = CreateDiaperUiState.ValidationError(typeError = typeError)
      return
    }

    viewModelScope.launch {
      _uiState.value = CreateDiaperUiState.Saving
      val request = CreateDiaperRequest(change_type = changeType.trim(), timestamp = timestamp)
      val result = repo.createDiaper(childId, request)
      _uiState.value =
          when (result) {
            is ApiResult.Success -> {
              syncScheduler.enqueueIfPending()
              CreateDiaperUiState.Success
            }
            is ApiResult.Error -> CreateDiaperUiState.Error(result.error.getUserMessage(context))
            is ApiResult.Loading -> CreateDiaperUiState.Saving
          }
    }
  }
}
