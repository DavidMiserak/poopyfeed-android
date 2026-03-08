package net.poopyfeed.pf

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.UserProfileUpdate
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.data.session.ClearSessionUseCase
import net.poopyfeed.pf.di.TokenManager

/** Banner state for timezone mismatch between device and profile. */
sealed interface TimezoneBannerState {
  /** Banner is hidden (no mismatch or user dismissed). */
  data object Hidden : TimezoneBannerState

  /** Mismatch detected; show banner with both device and profile timezones. */
  data class Visible(val deviceTimezone: String, val profileTimezone: String) : TimezoneBannerState

  /** API call in progress to update profile timezone. */
  data class Saving(val deviceTimezone: String, val profileTimezone: String) : TimezoneBannerState
}

/**
 * ViewModel for [MainActivity]. Handles logout, timezone mismatch detection, and timezone updates.
 * Timezone banner state is checked on every app resume (STARTED lifecycle event).
 */
@HiltViewModel
class MainActivityViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val clearSessionUseCase: ClearSessionUseCase,
    private val tokenManager: TokenManager,
    private val notificationsRepository: NotificationsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  /**
   * Emits once when logout completes; the activity should navigate to Login and clear the back
   * stack.
   */
  private val _logoutNavigateToLogin = MutableSharedFlow<Unit>(replay = 0)
  val logoutNavigateToLogin: SharedFlow<Unit> = _logoutNavigateToLogin.asSharedFlow()

  /**
   * Unread notification count for the bottom nav badge. Updated by polling when app is in
   * foreground.
   */
  private val _unreadCount = MutableStateFlow(0)
  val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

  /** Current timezone banner state (Hidden, Visible, or Saving). */
  private val _timezoneBanner = MutableStateFlow<TimezoneBannerState>(TimezoneBannerState.Hidden)
  val timezoneBanner: StateFlow<TimezoneBannerState> = _timezoneBanner.asStateFlow()

  /** One-shot error event for Snackbar (emitted when update fails). */
  private val _bannerError = MutableSharedFlow<String>(replay = 0)
  val bannerError: SharedFlow<String> = _bannerError.asSharedFlow()

  /** Performs logout (API session + local cache + token) and triggers navigation to Login. */
  fun logout() {
    viewModelScope.launch {
      authRepository.logout() // best-effort; ignore result
      clearSessionUseCase()
      _logoutNavigateToLogin.emit(Unit)
    }
  }

  /**
   * Checks for timezone mismatch between device and cached profile timezone. Called on every app
   * resume (STARTED). If mismatch detected, updates [timezoneBanner] to Visible; otherwise Hidden.
   * No-op if called while Saving to avoid race conditions.
   */
  fun checkTimezoneMismatch() {
    val cachedTz = tokenManager.getProfileTimezone() ?: return
    val deviceTz = TimeZone.getDefault().id

    // Don't reset Visible/Saving states unnecessarily
    if (_timezoneBanner.value is TimezoneBannerState.Saving) {
      return
    }

    _timezoneBanner.value =
        if (cachedTz != deviceTz) {
          TimezoneBannerState.Visible(deviceTimezone = deviceTz, profileTimezone = cachedTz)
        } else {
          TimezoneBannerState.Hidden
        }
  }

  /** Calls API to update profile timezone to device timezone. On success, hides banner. */
  fun useDeviceTimezone() {
    val current = _timezoneBanner.value
    val (deviceTz, profileTz) =
        when (current) {
          is TimezoneBannerState.Visible -> Pair(current.deviceTimezone, current.profileTimezone)
          else -> return
        }

    viewModelScope.launch {
      _timezoneBanner.value =
          TimezoneBannerState.Saving(deviceTimezone = deviceTz, profileTimezone = profileTz)

      when (val result = authRepository.updateProfile(UserProfileUpdate(timezone = deviceTz))) {
        is ApiResult.Success -> {
          tokenManager.saveProfileTimezone(deviceTz)
          _timezoneBanner.value = TimezoneBannerState.Hidden
        }
        is ApiResult.Error -> {
          _timezoneBanner.value =
              TimezoneBannerState.Visible(
                  deviceTimezone = deviceTz, profileTimezone = profileTz) // restore
          _bannerError.emit(result.error.getUserMessage(context))
        }
        else -> Unit
      }
    }
  }

  /** Dismisses the timezone banner for the session without updating profile. */
  fun dismissTimezoneBanner() {
    _timezoneBanner.value = TimezoneBannerState.Hidden
  }

  /**
   * Fetches unread notification count from API and updates [unreadCount]. Called every 30s when app
   * is in foreground.
   */
  fun refreshUnreadCount() {
    viewModelScope.launch {
      when (val result = notificationsRepository.getUnreadCount()) {
        is ApiResult.Success -> _unreadCount.value = result.data
        is ApiResult.Error -> Unit // leave count unchanged on error
        else -> Unit
      }
    }
  }
}
