package com.poopyfeed.android.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.UserProfile
import com.poopyfeed.android.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    // Loading
    val isLoadingProfile: Boolean = false,
    val isSavingProfile: Boolean = false,
    val isSavingPassword: Boolean = false,
    val isDeletingAccount: Boolean = false,
    // Profile data
    val profile: UserProfile? = null,
    // Profile tab form fields
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val timezone: String = "UTC",
    // Security tab form fields
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    // Account tab form field
    val deletePassword: String = "",
    // Field errors
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val deletePasswordError: String? = null,
    // API errors per section
    val profileApiError: String? = null,
    val passwordApiError: String? = null,
    val deleteApiError: String? = null,
    // Success messages
    val profileSuccessMessage: String? = null,
    val passwordSuccessMessage: String? = null,
    // Navigation trigger
    val deleteSuccess: Boolean = false,
    // Tab selection
    val selectedTabIndex: Int = 0,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _uiState.update { it.copy(isLoadingProfile = true) }

        viewModelScope.launch {
            val result = profileRepository.getProfile()
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            profile = profile,
                            firstName = profile.firstName,
                            lastName = profile.lastName,
                            email = profile.email,
                            timezone = profile.timezone,
                            isLoadingProfile = false,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingProfile = false,
                            profileApiError = error.message ?: "Failed to load profile",
                        )
                    }
                },
            )
        }
    }

    fun onFirstNameChange(value: String) {
        _uiState.update { it.copy(firstName = value, firstNameError = null, profileApiError = null) }
    }

    fun onLastNameChange(value: String) {
        _uiState.update { it.copy(lastName = value, lastNameError = null, profileApiError = null) }
    }

    fun onTimezoneChange(value: String) {
        _uiState.update { it.copy(timezone = value, profileApiError = null) }
    }

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value, currentPasswordError = null, passwordApiError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null, passwordApiError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, passwordApiError = null) }
    }

    fun onDeletePasswordChange(value: String) {
        _uiState.update { it.copy(deletePassword = value, deletePasswordError = null, deleteApiError = null) }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }

    fun saveProfile() {
        val state = _uiState.value

        val firstNameError = validateNonEmpty(state.firstName, "First name")
        val lastNameError = validateNonEmpty(state.lastName, "Last name")

        if (firstNameError != null || lastNameError != null) {
            _uiState.update {
                it.copy(
                    firstNameError = firstNameError,
                    lastNameError = lastNameError,
                )
            }
            return
        }

        _uiState.update { it.copy(isSavingProfile = true, profileApiError = null, profileSuccessMessage = null) }

        viewModelScope.launch {
            val result = profileRepository.updateProfile(
                firstName = state.firstName,
                lastName = state.lastName,
                timezone = state.timezone,
            )
            result.fold(
                onSuccess = { profile ->
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            profile = profile,
                            profileSuccessMessage = "Profile updated successfully",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingProfile = false,
                            profileApiError = error.message ?: "Failed to save profile",
                        )
                    }
                },
            )
        }
    }

    fun changePassword() {
        val state = _uiState.value

        val currentPasswordError = validateNonEmpty(state.currentPassword, "Current password")
        val newPasswordError = validatePassword(state.newPassword)
        val confirmPasswordError = validateConfirmPassword(state.newPassword, state.confirmPassword)

        if (currentPasswordError != null || newPasswordError != null || confirmPasswordError != null) {
            _uiState.update {
                it.copy(
                    currentPasswordError = currentPasswordError,
                    newPasswordError = newPasswordError,
                    confirmPasswordError = confirmPasswordError,
                )
            }
            return
        }

        _uiState.update { it.copy(isSavingPassword = true, passwordApiError = null, passwordSuccessMessage = null) }

        viewModelScope.launch {
            val result = profileRepository.changePassword(
                currentPassword = state.currentPassword,
                newPassword = state.newPassword,
                newPasswordConfirm = state.confirmPassword,
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSavingPassword = false,
                            currentPassword = "",
                            newPassword = "",
                            confirmPassword = "",
                            passwordSuccessMessage = "Password changed successfully",
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSavingPassword = false,
                            passwordApiError = error.message ?: "Failed to change password",
                        )
                    }
                },
            )
        }
    }

    fun deleteAccount() {
        val state = _uiState.value

        val deletePasswordError = validateNonEmpty(state.deletePassword, "Password")

        if (deletePasswordError != null) {
            _uiState.update { it.copy(deletePasswordError = deletePasswordError) }
            return
        }

        _uiState.update { it.copy(isDeletingAccount = true, deleteApiError = null) }

        viewModelScope.launch {
            val result = profileRepository.deleteAccount(state.deletePassword)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isDeletingAccount = false, deleteSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            deleteApiError = error.message ?: "Failed to delete account",
                        )
                    }
                },
            )
        }
    }

    companion object {
        val TIMEZONES = listOf(
            "UTC",
            "America/New_York",
            "America/Chicago",
            "America/Denver",
            "America/Los_Angeles",
            "Europe/London",
            "Europe/Paris",
            "Europe/Berlin",
            "Europe/Madrid",
            "Europe/Amsterdam",
            "Asia/Tokyo",
            "Asia/Shanghai",
            "Asia/Hong_Kong",
            "Asia/Bangkok",
            "Asia/Singapore",
            "Asia/Dubai",
            "Asia/Kolkata",
            "Australia/Sydney",
            "Australia/Melbourne",
            "Australia/Brisbane",
            "Pacific/Auckland",
            "America/Toronto",
            "America/Mexico_City",
            "America/Sao_Paulo",
            "Africa/Cairo",
            "Africa/Johannesburg",
            "Africa/Lagos",
            "Asia/Seoul",
            "Asia/Jakarta",
        )

        fun validateNonEmpty(value: String, fieldName: String): String? {
            return if (value.isBlank()) "$fieldName is required" else null
        }

        fun validatePassword(password: String): String? {
            return when {
                password.isBlank() -> "Password is required"
                password.length < 8 -> "Password must be at least 8 characters"
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
