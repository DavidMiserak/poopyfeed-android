package com.poopyfeed.android.ui.screens.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.SharingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AcceptInviteUiState(
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
)

@HiltViewModel
class AcceptInviteViewModel
    @Inject
    constructor(
        private val sharingRepository: SharingRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AcceptInviteUiState())
        val uiState: StateFlow<AcceptInviteUiState> = _uiState.asStateFlow()

        fun setToken(token: String) {
            _uiState.update { it.copy(token = token, error = null) }
        }

        fun accept() {
            val token = _uiState.value.token
            if (token.isBlank()) {
                _uiState.update { it.copy(error = "Invalid invite link") }
                return
            }
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                sharingRepository.acceptInvite(token).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, success = true) }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to accept invite",
                            )
                        }
                    },
                )
            }
        }
    }
