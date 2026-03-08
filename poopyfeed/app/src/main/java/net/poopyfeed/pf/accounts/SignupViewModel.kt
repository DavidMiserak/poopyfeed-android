package net.poopyfeed.pf.accounts

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.di.TokenManager

/** UI state for the signup screen. */
sealed interface SignupUiState {
  /** Initial state; form is editable. */
  data object Idle : SignupUiState

  /** Signup request in progress. */
  data object Loading : SignupUiState

  /** Signup succeeded; navigate to home. */
  data object Success : SignupUiState

  /** Signup failed; [message] is user-facing. */
  data class Error(val message: String) : SignupUiState
}

/**
 * ViewModel for [SignupFragment]. Registers the user via [AuthRepository], persists token via
 * [TokenManager], and exposes [uiState] for the UI.
 */
@HiltViewModel
class SignupViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val notificationsRepository: NotificationsRepository,
    private val tokenManager: TokenManager,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val _uiState: MutableStateFlow<SignupUiState> = MutableStateFlow(SignupUiState.Idle)
  val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

  /**
   * Attempts signup with [email] and [password]; updates [uiState] to Loading then Success or
   * Error. On success, also populates the timezone cache for the app-wide timezone mismatch banner.
   */
  fun signUp(email: String, password: String) {
    viewModelScope.launch {
      _uiState.value = SignupUiState.Loading

      when (val result = authRepository.signup(email, password)) {
        is ApiResult.Success -> {
          tokenManager.saveToken(result.data)
          // Populate timezone cache for app-wide timezone mismatch banner
          when (val profileResult = authRepository.getProfile()) {
            is ApiResult.Success -> {
              tokenManager.saveProfileTimezone(profileResult.data.timezone)
            }
            else -> {
              // Best-effort; ignore profile fetch errors, user will see banner later
            }
          }
          // Register FCM token for push notifications
          registerFcmToken()
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

  /** Registers the current FCM token with the backend. Best-effort; ignores errors. */
  private suspend fun registerFcmToken() {
    try {
      val token = FirebaseMessaging.getInstance().token.await()
      notificationsRepository.registerDeviceToken(token)
      Log.d(TAG, "FCM token registered")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to register FCM token", e)
    }
  }

  companion object {
    private const val TAG = "SignupViewModel"
  }

  /** Resets UI state from Error back to Idle (e.g. after showing Snackbar). */
  fun clearError() {
    if (_uiState.value is SignupUiState.Error) {
      _uiState.value = SignupUiState.Idle
    }
  }
}
