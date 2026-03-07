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
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository

/** UI state for the edit feeding bottom sheet. */
sealed interface EditFeedingUiState {
  data object Loading : EditFeedingUiState

  data class Ready(val feeding: Feeding) : EditFeedingUiState

  data class Error(val message: String) : EditFeedingUiState

  data object Saving : EditFeedingUiState

  data object Success : EditFeedingUiState

  data class SaveError(val message: String) : EditFeedingUiState

  data class ValidationError(
      val typeError: String? = null,
      val amountError: String? = null,
      val minutesError: String? = null,
      val sideError: String? = null,
  ) : EditFeedingUiState
}

/**
 * ViewModel for [EditFeedingBottomSheetFragment]. Loads feeding for prefill, validates and saves
 * updates.
 */
@HiltViewModel
class EditFeedingViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedFeedingsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])
  private val feedingId: Int = checkNotNull(savedStateHandle["feedingId"])

  private val _uiState: MutableStateFlow<EditFeedingUiState> =
      MutableStateFlow(EditFeedingUiState.Loading)
  val uiState: StateFlow<EditFeedingUiState> = _uiState.asStateFlow()

  init {
    loadFeeding()
  }

  private fun loadFeeding() {
    viewModelScope.launch {
      val feeding = repo.getFeeding(childId, feedingId)
      _uiState.value =
          if (feeding != null) EditFeedingUiState.Ready(feeding)
          else EditFeedingUiState.Error("Feeding not found.")
    }
  }

  fun saveFeeding(
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
          EditFeedingUiState.ValidationError(
              typeError = typeError,
              amountError = amountError,
              minutesError = minutesError,
              sideError = sideError,
          )
      return
    }

    viewModelScope.launch {
      _uiState.value = EditFeedingUiState.Saving
      val request =
          CreateFeedingRequest(
              feeding_type = feedingType.trim(),
              amount_oz = if (feedingType.equals("bottle", ignoreCase = true)) amountOz else null,
              durationMinutes =
                  if (feedingType.equals("breast", ignoreCase = true)) durationMinutes else null,
              side = if (feedingType.equals("breast", ignoreCase = true)) side else null,
              timestamp = timestamp,
          )
      val result = repo.updateFeeding(childId, feedingId, request)
      _uiState.value =
          when (result) {
            is ApiResult.Success -> EditFeedingUiState.Success
            is ApiResult.Error -> EditFeedingUiState.SaveError(result.error.getUserMessage(context))
            is ApiResult.Loading -> EditFeedingUiState.Saving
          }
    }
  }
}
