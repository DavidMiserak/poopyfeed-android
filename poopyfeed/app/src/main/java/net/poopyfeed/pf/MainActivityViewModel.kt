package net.poopyfeed.pf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager

/**
 * ViewModel for [MainActivity]. Handles logout (clears token and session) and emits a one-shot
 * event to navigate back to [net.poopyfeed.pf.LoginFragment] with a clean back stack.
 */
@HiltViewModel
class MainActivityViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

  /**
   * Emits once when logout completes; the activity should navigate to Login and clear the back
   * stack.
   */
  private val _logoutNavigateToLogin = MutableSharedFlow<Unit>(replay = 0)
  val logoutNavigateToLogin: SharedFlow<Unit> = _logoutNavigateToLogin.asSharedFlow()

  /** Performs logout (API session + local token/cookies) and triggers navigation to Login. */
  fun logout() {
    viewModelScope.launch {
      authRepository.logout() // best-effort; ignore result
      tokenManager.clearToken()
      _logoutNavigateToLogin.emit(Unit)
    }
  }
}
