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

sealed interface LoginUiState {
  data object Idle : LoginUiState

  data object Loading : LoginUiState

  data object Success : LoginUiState

  data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<LoginUiState> = MutableStateFlow(LoginUiState.Idle)
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  fun checkExistingToken(): Boolean = tokenManager.getToken() != null

  fun login(email: String, password: String) {
    viewModelScope.launch {
      _uiState.value = LoginUiState.Loading

      when (val result = authRepository.login(email, password)) {
        is ApiResult.Success -> {
          tokenManager.saveToken(result.data)
          _uiState.value = LoginUiState.Success
        }
        is ApiResult.Error -> {
          _uiState.value = LoginUiState.Error(result.error.getUserMessage(context))
        }
        is ApiResult.Loading -> {
          // no-op; repository does not currently emit Loading, we manage it here
        }
      }
    }
  }

  fun clearError() {
    if (_uiState.value is LoginUiState.Error) {
      _uiState.value = LoginUiState.Idle
    }
  }
}
