package net.poopyfeed.pf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.NetworkModule

sealed interface SignupUiState {
  data object Idle : SignupUiState

  data object Loading : SignupUiState

  data class Success(val token: String) : SignupUiState

  data class Error(val message: String) : SignupUiState
}

class SignupViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState: MutableStateFlow<SignupUiState> = MutableStateFlow(SignupUiState.Idle)
  val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

  private val authRepository: AuthRepository by lazy {
    val apiService = NetworkModule.providePoopyFeedApiService(getApplication())
    AuthRepository(apiService)
  }

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
