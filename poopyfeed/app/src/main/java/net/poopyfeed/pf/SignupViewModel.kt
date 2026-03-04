package net.poopyfeed.pf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository

sealed interface SignupUiState {
  data object Idle : SignupUiState

  data object Loading : SignupUiState

  data class Success(val token: String) : SignupUiState

  data class Error(val message: String) : SignupUiState
}

class SignupViewModel(private val authRepository: AuthRepository) : ViewModel() {

  private val _uiState: MutableStateFlow<SignupUiState> = MutableStateFlow(SignupUiState.Idle)
  val uiState: StateFlow<SignupUiState> = _uiState

  fun signUp(email: String, password: String) {
    viewModelScope.launch {
      _uiState.value = SignupUiState.Loading

      when (val result = authRepository.signup(email, password)) {
        is ApiResult.Success -> {
          _uiState.value = SignupUiState.Success(result.data)
        }
        is ApiResult.Error -> {
          _uiState.value = SignupUiState.Error(result.error.getUserMessage())
        }
        is ApiResult.Loading -> {
          // no-op; repository does not emit Loading for this call
        }
      }
    }
  }

  fun clearError() {
    if (_uiState.value is SignupUiState.Error) {
      _uiState.value = SignupUiState.Idle
    }
  }
}
