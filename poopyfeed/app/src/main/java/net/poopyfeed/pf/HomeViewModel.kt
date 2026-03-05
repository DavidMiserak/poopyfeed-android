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

/** UI state for the home screen. */
sealed interface HomeUiState {
  /** Profile is loading. */
  data object Loading : HomeUiState

  /** Profile loaded; [email] is the current user's email. */
  data class Ready(val email: String) : HomeUiState

  /** No token or auth failed; navigate to login. */
  data object Unauthorized : HomeUiState

  /** Request failed; [message] is user-facing. */
  data class Error(val message: String) : HomeUiState
}

/**
 * ViewModel for [HomeFragment]. Loads user profile on init; exposes [uiState]. Use [hasToken] to
 * gate navigation and [loadProfile] for refresh.
 */
@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<HomeUiState> = MutableStateFlow(HomeUiState.Loading)
  val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

  init {
    loadProfile()
  }

  /** True if an auth token is stored. */
  fun hasToken(): Boolean = tokenManager.getToken() != null

  /** Fetches profile from API and updates [uiState]; sets Unauthorized if no token. */
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
