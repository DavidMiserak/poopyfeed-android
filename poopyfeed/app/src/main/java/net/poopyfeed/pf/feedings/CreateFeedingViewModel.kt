package net.poopyfeed.pf.feedings

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
import net.poopyfeed.pf.data.models.CreateFeedingRequest
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.sync.SyncScheduler

/** UI state for the create feeding bottom sheet. */
sealed interface CreateFeedingUiState {
  data object Idle : CreateFeedingUiState

  data object Saving : CreateFeedingUiState

  data object Success : CreateFeedingUiState

  data class Error(val message: String) : CreateFeedingUiState

  data class ValidationError(
      val typeError: String? = null,
      val amountError: String? = null,
      val minutesError: String? = null,
      val sideError: String? = null,
  ) : CreateFeedingUiState
}

/** ViewModel for [CreateFeedingBottomSheetFragment]. Handles validation and feeding creation. */
@HiltViewModel
class CreateFeedingViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedFeedingsRepository,
    private val syncScheduler: SyncScheduler,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<CreateFeedingUiState> =
      MutableStateFlow(CreateFeedingUiState.Idle)
  val uiState: StateFlow<CreateFeedingUiState> = _uiState.asStateFlow()

  fun createFeeding(
      feedingType: String,
      amountOz: Double?,
      durationMinutes: Int?,
      side: String,
      timestamp: String
  ) {
    val typeError = if (feedingType.isBlank()) "Select feeding type." else null
    val amountError =
        if (feedingType.equals("bottle", ignoreCase = true) &&
            (amountOz == null || amountOz <= 0)) {
          "Enter amount for bottle feeding."
        } else null
    val minutesError =
        if (feedingType.equals("breast", ignoreCase = true) &&
            (durationMinutes == null || durationMinutes <= 0)) {
          "Enter minutes for breast feeding."
        } else null
    val sideError =
        if (feedingType.equals("breast", ignoreCase = true) &&
            (side != "left" && side != "right" && side != "both")) {
          "Select side for breast feeding."
        } else null

    if (typeError != null || amountError != null || minutesError != null || sideError != null) {
      _uiState.value =
          CreateFeedingUiState.ValidationError(
              typeError = typeError,
              amountError = amountError,
              minutesError = minutesError,
              sideError = sideError,
          )
      return
    }

    viewModelScope.launch {
      _uiState.value = CreateFeedingUiState.Saving
      val request =
          CreateFeedingRequest(
              feeding_type = feedingType.trim(),
              amount_oz = if (feedingType.equals("bottle", ignoreCase = true)) amountOz else null,
              durationMinutes =
                  if (feedingType.equals("breast", ignoreCase = true)) durationMinutes else null,
              side = if (feedingType.equals("breast", ignoreCase = true)) side else null,
              timestamp = timestamp,
          )
      val result = repo.createFeeding(childId, request)
      _uiState.value =
          when (result) {
            is ApiResult.Success -> {
              syncScheduler.enqueueIfPending()
              CreateFeedingUiState.Success
            }
            is ApiResult.Error -> CreateFeedingUiState.Error(result.error.getUserMessage(context))
            is ApiResult.Loading -> CreateFeedingUiState.Saving
          }
    }
  }
}
