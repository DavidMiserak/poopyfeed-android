package com.poopyfeed.android.ui.screens.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.AuthRepository
import com.poopyfeed.android.ui.screens.login.LoginViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignupUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val apiError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignupUiState())
    val uiState: StateFlow<SignupUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, apiError = null) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, apiError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, apiError = null) }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update {
            it.copy(confirmPassword = confirmPassword, confirmPasswordError = null, apiError = null)
        }
    }

    fun signup() {
        val state = _uiState.value

        val nameError = validateName(state.name)
        val emailError = LoginViewModel.validateEmail(state.email)
        val passwordError = LoginViewModel.validatePassword(state.password)
        val confirmPasswordError = validateConfirmPassword(state.password, state.confirmPassword)

        if (nameError != null || emailError != null || passwordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, apiError = null) }

        viewModelScope.launch {
            val result = authRepository.signup(state.email, state.password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            apiError = error.message ?: "Signup failed",
                        )
                    }
                },
            )
        }
    }

    companion object {
        fun validateName(name: String): String? {
            return when {
                name.isBlank() -> "Name is required"
                else -> null
            }
        }

        fun validateConfirmPassword(password: String, confirmPassword: String): String? {
            return when {
                confirmPassword.isBlank() -> "Please confirm your password"
                confirmPassword != password -> "Passwords do not match"
                else -> null
            }
        }
    }
}
