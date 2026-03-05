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

/** UI state for the account settings screen. */
sealed interface AccountSettingsUiState {
  // Profile Section States
  /** Profile is loading. */
  data object Loading : AccountSettingsUiState

  /** Profile loaded; [profile] and [timezones] for the form. */
  data class Ready(val profile: UserProfile, val timezones: List<String>) : AccountSettingsUiState

  /** No token; cannot edit. */
  data object Unauthorized : AccountSettingsUiState

  /** Load or save failed; [message] is user-facing. */
  data class Error(val message: String) : AccountSettingsUiState

  /** Profile save in progress. */
  data object Saving : AccountSettingsUiState

  /** Profile save succeeded; [profile] and [timezones] updated. */
  data class Saved(val profile: UserProfile, val timezones: List<String>) : AccountSettingsUiState

  // Password Change Section States
  /** Password change in progress. */
  data object ChangingPassword : AccountSettingsUiState

  /** Password changed successfully. */
  data object PasswordChanged : AccountSettingsUiState

  /** Password change failed; [message] is user-facing. */
  data class PasswordChangeError(val message: String) : AccountSettingsUiState

  // Account Deletion Section States
  /** Account deletion in progress. */
  data object DeletingAccount : AccountSettingsUiState

  /** Account deleted successfully; navigate to login. */
  data object AccountDeleted : AccountSettingsUiState

  /** Account deletion failed; [message] is user-facing. */
  data class DeletionError(val message: String) : AccountSettingsUiState
}

/**
 * ViewModel for [AccountSettingsFragment]. Handles profile editing, password changes, and account
 * deletion via [AuthRepository]; exposes [uiState] for the UI to react to state changes.
 */
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

  /** Fetches profile from API and updates [uiState]; sets Unauthorized if no token. */
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

  /**
   * Sends profile update to API; transitions to Saving then Saved or Error. No-op if not in Ready
   * state.
   */
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

  /**
   * Change the authenticated user's password.
   *
   * Validates that passwords match and have minimum length before API call. Transitions to
   * ChangingPassword, then PasswordChanged or PasswordChangeError. Returns new token silently (no
   * logout). No-op if not in Ready state.
   */
  fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
    val currentState = _uiState.value
    if (currentState !is AccountSettingsUiState.Ready) {
      return
    }

    // Client-side validation
    when {
      currentPassword.isBlank() -> {
        _uiState.value =
            AccountSettingsUiState.PasswordChangeError(
                context.getString(R.string.account_current_password_label) + " required")
        return
      }
      newPassword.isBlank() -> {
        _uiState.value =
            AccountSettingsUiState.PasswordChangeError(
                context.getString(R.string.account_new_password_label) + " required")
        return
      }
      newPassword.length < 8 -> {
        _uiState.value =
            AccountSettingsUiState.PasswordChangeError(
                context.getString(R.string.account_password_validation_error_short))
        return
      }
      newPassword != confirmPassword -> {
        _uiState.value =
            AccountSettingsUiState.PasswordChangeError(
                context.getString(R.string.account_password_validation_error_mismatch))
        return
      }
    }

    viewModelScope.launch {
      _uiState.value = AccountSettingsUiState.ChangingPassword

      when (val result = authRepository.changePassword(currentPassword, newPassword)) {
        is ApiResult.Success -> {
          // Token rotation: store new token (user remains logged in)
          tokenManager.saveToken(result.data)
          _uiState.value = AccountSettingsUiState.PasswordChanged
        }
        is ApiResult.Error -> {
          _uiState.value =
              AccountSettingsUiState.PasswordChangeError(result.error.getUserMessage(context))
        }
        is ApiResult.Loading -> {
          // no-op
        }
      }
    }
  }

  /**
   * Delete the authenticated user's account permanently.
   *
   * Irreversible operation. Transitions to DeletingAccount, then AccountDeleted or DeletionError.
   * On success, caller should handle navigation to login. No-op if not in Ready state.
   */
  fun deleteAccount(password: String) {
    val currentState = _uiState.value
    if (currentState !is AccountSettingsUiState.Ready) {
      return
    }

    // Basic validation
    if (password.isBlank()) {
      _uiState.value =
          AccountSettingsUiState.DeletionError(
              context.getString(R.string.account_current_password_label) + " required")
      return
    }

    viewModelScope.launch {
      _uiState.value = AccountSettingsUiState.DeletingAccount

      when (val result = authRepository.deleteAccount(password)) {
        is ApiResult.Success -> {
          // Account deleted successfully; caller should handle cleanup
          tokenManager.clearToken()
          _uiState.value = AccountSettingsUiState.AccountDeleted
        }
        is ApiResult.Error -> {
          _uiState.value =
              AccountSettingsUiState.DeletionError(result.error.getUserMessage(context))
        }
        is ApiResult.Loading -> {
          // no-op
        }
      }
    }
  }

  /** Clear password change state after success/error (reset the section). */
  fun clearPasswordChangeState() {
    val currentState = _uiState.value
    if (currentState is AccountSettingsUiState.Ready) {
      // Already in Ready, no action needed
      return
    }
    if (currentState is AccountSettingsUiState.PasswordChanged ||
        currentState is AccountSettingsUiState.PasswordChangeError) {
      // Reload profile to ensure Ready state
      loadProfile()
    }
  }

  /** Clear deletion state after success/error. On success, caller navigates to login. */
  fun clearDeletionState() {
    val currentState = _uiState.value
    if (currentState is AccountSettingsUiState.DeletionError) {
      // Reload profile to recover to Ready state
      loadProfile()
    }
    // AccountDeleted state is handled by caller (navigation)
  }
}
