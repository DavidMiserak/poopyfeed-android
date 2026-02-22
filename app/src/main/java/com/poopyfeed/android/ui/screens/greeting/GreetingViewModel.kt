package com.poopyfeed.android.ui.screens.greeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthCheckState {
    LOADING,
    AUTHENTICATED,
    UNAUTHENTICATED,
}

@HiltViewModel
class GreetingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthCheckState.LOADING)
    val authState: StateFlow<AuthCheckState> = _authState.asStateFlow()

    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val hasToken = authRepository.hasToken()
            _authState.value = if (hasToken) {
                AuthCheckState.AUTHENTICATED
            } else {
                AuthCheckState.UNAUTHENTICATED
            }
        }
    }
}
