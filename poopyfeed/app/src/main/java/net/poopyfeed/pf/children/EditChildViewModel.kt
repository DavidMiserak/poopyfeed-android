package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.models.UpdateChildRequest
import net.poopyfeed.pf.data.repository.CachedChildrenRepository

/** UI state for the edit child bottom sheet. */
sealed interface EditChildUiState {
  /** Loading child data. */
  data object Loading : EditChildUiState

  /** Child loaded; form can be edited. [canEditReminder] true for owner/co-parent. */
  data class Ready(
      val child: Child,
      val canEditReminder: Boolean,
  ) : EditChildUiState

  /** Failed to load child. */
  data class Error(val message: String) : EditChildUiState

  /** Saving in progress. */
  data object Saving : EditChildUiState

  /** Save succeeded; fragment should dismiss. */
  data object Success : EditChildUiState

  /** Save failed with API/network error. */
  data class SaveError(val message: String) : EditChildUiState

  /** Validation error (e.g. empty name). */
  data class ValidationError(val nameError: String? = null, val dobError: String? = null) :
      EditChildUiState
}

/**
 * ViewModel for [EditChildBottomSheetFragment]. Loads child by ID, exposes form state, and saves
 * updates (name, DOB, gender, feeding reminder interval) via PATCH. Feeding reminder section is
 * only relevant when [can_edit] is true (owner/co-parent).
 */
@HiltViewModel
class EditChildViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedChildrenRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<EditChildUiState> =
      MutableStateFlow(EditChildUiState.Loading)
  val uiState: StateFlow<EditChildUiState> = _uiState.asStateFlow()

  private val _deleteSuccess: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 0)
  val deleteSuccess: SharedFlow<Unit> = _deleteSuccess.asSharedFlow()

  private val _deleteError: MutableSharedFlow<String> = MutableSharedFlow(replay = 0)
  val deleteError: SharedFlow<String> = _deleteError.asSharedFlow()

  init {
    viewModelScope.launch {
      repo
          .getChildCached(childId)
          .catch { e ->
            _uiState.value =
                EditChildUiState.Error(
                    e.message
                        ?: context.getString(net.poopyfeed.pf.R.string.child_detail_error_generic))
          }
          .map { child ->
            if (child == null) {
              EditChildUiState.Error(
                  context.getString(net.poopyfeed.pf.R.string.child_detail_error_generic))
            } else {
              EditChildUiState.Ready(child = child, canEditReminder = child.can_edit)
            }
          }
          .collect { state ->
            // Only transition from Loading to Ready/Error; don't overwrite Saving/Success/SaveError
            if (_uiState.value is EditChildUiState.Loading) {
              _uiState.value = state
            }
          }
    }
  }

  /**
   * Saves the form. Validates name and DOB; builds [UpdateChildRequest] and PATCHes. On success
   * emits [EditChildUiState.Success]; on error [EditChildUiState.SaveError] or
   * [EditChildUiState.ValidationError].
   */
  fun save(
      name: String,
      dateOfBirth: String,
      gender: String,
      feedingReminderIntervalHours: Int?,
  ) {
    val nameError = if (name.trim().isEmpty()) "Name is required" else null
    val dobError = if (dateOfBirth.isEmpty()) "Date of birth is required" else null
    if (nameError != null || dobError != null) {
      _uiState.value = EditChildUiState.ValidationError(nameError = nameError, dobError = dobError)
      return
    }

    viewModelScope.launch {
      val current = _uiState.value
      if (current !is EditChildUiState.Ready) return@launch

      _uiState.value = EditChildUiState.Saving

      val request =
          UpdateChildRequest(
              name = name.trim(),
              date_of_birth = dateOfBirth,
              gender = gender,
              feeding_reminder_interval = feedingReminderIntervalHours,
          )

      val result = repo.updateChild(childId, request)
      _uiState.update {
        when (result) {
          is ApiResult.Success -> EditChildUiState.Success
          is ApiResult.Error -> EditChildUiState.SaveError(result.error.getUserMessage(context))
          is ApiResult.Loading -> EditChildUiState.Saving
        }
      }
    }
  }

  /** Deletes the child from the API and cache. Emits [deleteSuccess] or [deleteError]. */
  fun deleteChild() {
    viewModelScope.launch {
      val result = repo.deleteChild(childId)
      when (result) {
        is ApiResult.Success -> _deleteSuccess.emit(Unit)
        is ApiResult.Error -> _deleteError.emit(result.error.getUserMessage(context))
        is ApiResult.Loading -> {
          /* no-op */
        }
      }
    }
  }
}
