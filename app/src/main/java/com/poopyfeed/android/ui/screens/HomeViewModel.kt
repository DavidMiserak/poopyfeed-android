package com.poopyfeed.android.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()

    private val _logoutComplete = MutableStateFlow(false)
    val logoutComplete: StateFlow<Boolean> = _logoutComplete.asStateFlow()

    fun logout() {
        _isLoggingOut.value = true
        viewModelScope.launch {
            authRepository.logout()
            _isLoggingOut.value = false
            _logoutComplete.value = true
        }
    }
}
