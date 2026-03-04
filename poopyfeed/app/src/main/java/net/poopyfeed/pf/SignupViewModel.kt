package net.poopyfeed.pf

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
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager

sealed interface SignupUiState {
  data object Idle : SignupUiState

  data object Loading : SignupUiState

  data object Success : SignupUiState

  data class Error(val message: String) : SignupUiState
}

@HiltViewModel
class SignupViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<SignupUiState> = MutableStateFlow(SignupUiState.Idle)
  val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

  fun signUp(email: String, password: String) {
    viewModelScope.launch {
      _uiState.value = SignupUiState.Loading

      when (val result = authRepository.signup(email, password)) {
        is ApiResult.Success -> {
          tokenManager.saveToken(result.data)
          _uiState.value = SignupUiState.Success
        }
        is ApiResult.Error -> {
          _uiState.value = SignupUiState.Error(result.error.getUserMessage(context))
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
