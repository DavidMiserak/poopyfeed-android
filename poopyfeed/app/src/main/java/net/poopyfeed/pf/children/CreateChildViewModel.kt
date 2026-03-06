package net.poopyfeed.pf.children

import android.content.Context
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
import net.poopyfeed.pf.data.models.CreateChildRequest
import net.poopyfeed.pf.data.repository.CachedChildrenRepository

/** UI state for the create child bottom sheet. */
sealed interface CreateChildUiState {
  /** Initial idle state. */
  data object Idle : CreateChildUiState

  /** Saving child to API. */
  data object Saving : CreateChildUiState

  /** Child created successfully. */
  data object Success : CreateChildUiState

  /** API or network error; [message] is user-facing. */
  data class Error(val message: String) : CreateChildUiState

  /** Client-side validation errors. */
  data class ValidationError(
      val nameError: String? = null,
      val dobError: String? = null,
      val genderError: String? = null,
  ) : CreateChildUiState
}

/** ViewModel for [CreateChildBottomSheetFragment]. Handles form validation and child creation. */
@HiltViewModel
class CreateChildViewModel
@Inject
constructor(
    private val repo: CachedChildrenRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<CreateChildUiState> =
      MutableStateFlow(CreateChildUiState.Idle)
  val uiState: StateFlow<CreateChildUiState> = _uiState.asStateFlow()

  /**
   * Validates and creates a child. On success, emits Success state. On validation or API error,
   * emits appropriate error state.
   */
  fun createChild(name: String, dateOfBirth: String, gender: String) {
    // Validate inputs
    val nameError = if (name.trim().isEmpty()) "Name is required" else null
    val dobError = if (dateOfBirth.isEmpty()) "Date of birth is required" else null
    val genderError = if (gender.isEmpty()) "Gender is required" else null

    if (nameError != null || dobError != null || genderError != null) {
      _uiState.value =
          CreateChildUiState.ValidationError(
              nameError = nameError,
              dobError = dobError,
              genderError = genderError,
          )
      return
    }

    viewModelScope.launch {
      _uiState.value = CreateChildUiState.Saving

      val request =
          CreateChildRequest(
              name = name.trim(),
              date_of_birth = dateOfBirth,
              gender = gender,
          )

      val result = repo.createChild(request)
      _uiState.value =
          when (result) {
            is ApiResult.Success -> CreateChildUiState.Success
            is ApiResult.Error -> CreateChildUiState.Error(result.error.getUserMessage(context))
            is ApiResult.Loading -> CreateChildUiState.Saving
          }
    }
  }

  /** Resets state to Idle for retry or reuse. */
  fun resetState() {
    _uiState.value = CreateChildUiState.Idle
  }
}
