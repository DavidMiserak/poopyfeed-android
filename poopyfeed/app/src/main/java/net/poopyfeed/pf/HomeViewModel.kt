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
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager

sealed interface HomeUiState {
  data object Loading : HomeUiState

  data class Ready(val email: String) : HomeUiState

  data object Unauthorized : HomeUiState

  data class Error(val message: String) : HomeUiState
}

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState.Loading)
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    loadProfile()
  }

  fun hasToken(): Boolean = tokenManager.getToken() != null

  fun loadProfile() {
    viewModelScope.launch {
      if (!hasToken()) {
        _uiState.value = HomeUiState.Unauthorized
        return@launch
      }

      _uiState.value = HomeUiState.Loading

      when (val result = authRepository.getProfile()) {
        is ApiResult.Success -> {
          val profile = result.data
          _uiState.value = HomeUiState.Ready(email = profile.email)
        }
        is ApiResult.Error -> {
          val error = result.error
          if (error is ApiError.HttpError && error.statusCode == 401) {
            tokenManager.clearToken()
            _uiState.value = HomeUiState.Unauthorized
          } else {
            _uiState.value = HomeUiState.Error(error.getUserMessage(context))
          }
        }
        is ApiResult.Loading -> {
          // no-op; we manage Loading locally
        }
      }
    }
  }
}
