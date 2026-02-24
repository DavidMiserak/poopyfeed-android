package com.poopyfeed.android.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val apiError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onEmailChange(email: String) {
            _uiState.update { it.copy(email = email, emailError = null, apiError = null) }
        }

        fun onPasswordChange(password: String) {
            _uiState.update { it.copy(password = password, passwordError = null, apiError = null) }
        }

        fun login() {
            val state = _uiState.value

            val emailError = validateEmail(state.email)
            val passwordError = validatePassword(state.password)

            if (emailError != null || passwordError != null) {
                _uiState.update {
                    it.copy(emailError = emailError, passwordError = passwordError)
                }
                return
            }

            _uiState.update { it.copy(isLoading = true, apiError = null) }

            viewModelScope.launch {
                val result = authRepository.login(state.email, state.password)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                apiError = error.message ?: "Login failed",
                            )
                        }
                    },
                )
            }
        }

        companion object {
            private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

            fun validateEmail(email: String): String? {
                return when {
                    email.isBlank() -> "Email is required"
                    !EMAIL_REGEX.matches(email) -> "Enter a valid email address"
                    else -> null
                }
            }

            fun validatePassword(password: String): String? {
                return when {
                    password.isBlank() -> "Password is required"
                    password.length < 8 -> "Password must be at least 8 characters"
                    else -> null
                }
            }
        }
    }
