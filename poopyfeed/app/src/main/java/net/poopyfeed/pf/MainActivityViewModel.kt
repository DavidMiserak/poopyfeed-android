package net.poopyfeed.pf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.NetworkModule

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

  private val authRepository: AuthRepository by lazy {
    val apiService = NetworkModule.providePoopyFeedApiService(getApplication())
    AuthRepository(apiService)
  }

  private val _logoutNavigateToLogin = MutableSharedFlow<Unit>(replay = 0)
  val logoutNavigateToLogin: SharedFlow<Unit> = _logoutNavigateToLogin.asSharedFlow()

  fun logout() {
    viewModelScope.launch {
      authRepository.logout() // best-effort; ignore result
      NetworkModule.clearAuthToken(getApplication())
      _logoutNavigateToLogin.emit(Unit)
    }
  }
}
