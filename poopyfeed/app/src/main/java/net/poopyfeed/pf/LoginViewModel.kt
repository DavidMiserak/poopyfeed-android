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

sealed interface LoginUiState {
  data object Idle : LoginUiState

  data object Loading : LoginUiState

  data object Success : LoginUiState

  data class Error(val message: String) : LoginUiState
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState: MutableStateFlow<LoginUiState> = MutableStateFlow(LoginUiState.Idle)
  val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

  private val authRepository: AuthRepository by lazy {
    val apiService = NetworkModule.providePoopyFeedApiService(getApplication())
    AuthRepository(apiService)
  }

  fun checkExistingToken(): Boolean {
    val token = NetworkModule.getAuthToken(getApplication())
    return token != null
  }

  fun login(email: String, password: String) {
    viewModelScope.launch {
      _uiState.value = LoginUiState.Loading

      when (val result = authRepository.login(email, password)) {
        is ApiResult.Success -> {
          NetworkModule.saveAuthToken(getApplication(), result.data)
          _uiState.value = LoginUiState.Success
        }
        is ApiResult.Error -> {
          _uiState.value = LoginUiState.Error(result.error.getUserMessage())
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
