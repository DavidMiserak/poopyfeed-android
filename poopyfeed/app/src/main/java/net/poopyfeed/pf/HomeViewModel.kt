package net.poopyfeed.pf

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.NetworkModule

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Ready(val firstName: String, val lastName: String) : HomeUiState
    data object Unauthorized : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState: MutableStateFlow<HomeUiState> =
        MutableStateFlow(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState

    private val authRepository: AuthRepository by lazy {
        val apiService = NetworkModule.providePoopyFeedApiService(getApplication())
        AuthRepository(apiService)
    }

    init {
        loadProfile()
    }

    fun hasToken(): Boolean {
        val token = NetworkModule.getAuthToken(getApplication())
        return token != null
    }

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
                    _uiState.value = HomeUiState.Ready(
                        firstName = profile.first_name,
                        lastName = profile.last_name
                    )
                }

                is ApiResult.Error -> {
                    val error = result.error
                    if (error is ApiError.HttpError && error.statusCode == 401) {
                        NetworkModule.clearAuthToken(getApplication())
                        _uiState.value = HomeUiState.Unauthorized
                    } else {
                        _uiState.value = HomeUiState.Error(error.getUserMessage())
                    }
                }

                is ApiResult.Loading -> {
                    // no-op; we manage Loading locally
                }
            }
        }
    }
}
