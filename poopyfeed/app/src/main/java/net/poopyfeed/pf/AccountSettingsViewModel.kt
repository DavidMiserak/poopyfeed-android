package net.poopyfeed.pf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.UserProfile
import net.poopyfeed.pf.data.models.UserProfileUpdate
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager

sealed interface AccountSettingsUiState {
  data object Loading : AccountSettingsUiState

  data class Ready(val profile: UserProfile, val timezones: List<String>) : AccountSettingsUiState

  data object Unauthorized : AccountSettingsUiState

  data class Error(val message: String) : AccountSettingsUiState

  data object Saving : AccountSettingsUiState

  data class Saved(val profile: UserProfile, val timezones: List<String>) : AccountSettingsUiState
}

@HiltViewModel
class AccountSettingsViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<AccountSettingsUiState> =
      MutableStateFlow(AccountSettingsUiState.Loading)
  val uiState: StateFlow<AccountSettingsUiState> = _uiState.asStateFlow()

  private val allTimezones: List<String> by lazy { TimeZone.getAvailableIDs().toList().sorted() }

  init {
    loadProfile()
  }

  fun loadProfile() {
    viewModelScope.launch {
      if (tokenManager.getToken() == null) {
        _uiState.value = AccountSettingsUiState.Unauthorized
        return@launch
      }

      _uiState.value = AccountSettingsUiState.Loading

      when (val result = authRepository.getProfile()) {
        is ApiResult.Success -> {
          val profile = result.data
          _uiState.value = AccountSettingsUiState.Ready(profile = profile, timezones = allTimezones)
        }
        is ApiResult.Error -> {
          val error = result.error
          if (error is ApiError.HttpError && error.statusCode == 401) {
            tokenManager.clearToken()
            _uiState.value = AccountSettingsUiState.Unauthorized
          } else {
            _uiState.value = AccountSettingsUiState.Error(error.getUserMessage(context))
          }
        }
        is ApiResult.Loading -> {
          // no-op; we manage Loading locally
        }
      }
    }
  }

  fun saveProfile(firstName: String, lastName: String, timezone: String) {
    val currentState = _uiState.value
    if (currentState !is AccountSettingsUiState.Ready) {
      return
    }

    viewModelScope.launch {
      _uiState.value = AccountSettingsUiState.Saving

      val update =
          UserProfileUpdate(first_name = firstName, last_name = lastName, timezone = timezone)

      when (val result = authRepository.updateProfile(update)) {
        is ApiResult.Success -> {
          val updatedProfile = result.data
          _uiState.value =
              AccountSettingsUiState.Saved(profile = updatedProfile, timezones = allTimezones)
        }
        is ApiResult.Error -> {
          _uiState.value = AccountSettingsUiState.Error(result.error.getUserMessage(context))
        }
        is ApiResult.Loading -> {
          // no-op
        }
      }
    }
  }
}
