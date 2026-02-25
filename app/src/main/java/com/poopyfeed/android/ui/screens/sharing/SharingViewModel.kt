package com.poopyfeed.android.ui.screens.sharing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.Invite
import com.poopyfeed.android.data.remote.dto.Share
import com.poopyfeed.android.data.repository.SharingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharingUiState(
    val shares: List<Share> = emptyList(),
    val invites: List<Invite> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val createInviteRole: String = "CG",
    val isCreatingInvite: Boolean = false,
)

@HiltViewModel
class SharingViewModel
    @Inject
    constructor(
        private val sharingRepository: SharingRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(SharingUiState())
        val uiState: StateFlow<SharingUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val sharesResult = sharingRepository.getShares(childId)
                val invitesResult = sharingRepository.getInvites(childId)
                sharesResult.fold(
                    onSuccess = { s ->
                        invitesResult.fold(
                            onSuccess = { i ->
                                _uiState.update {
                                    it.copy(
                                        shares = s,
                                        invites = i,
                                        isLoading = false,
                                        error = null,
                                    )
                                }
                            },
                            onFailure = { e ->
                                _uiState.update {
                                    it.copy(
                                        shares = s,
                                        isLoading = false,
                                        error = e.message ?: "Failed to load invites",
                                    )
                                }
                            },
                        )
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load",
                            )
                        }
                    },
                )
            }
        }

        fun onCreateInviteRoleChange(role: String) {
            _uiState.update { it.copy(createInviteRole = role) }
        }

        fun createInvite() {
            viewModelScope.launch {
                _uiState.update { it.copy(isCreatingInvite = true, error = null) }
                sharingRepository.createInvite(childId, _uiState.value.createInviteRole).fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(isCreatingInvite = false)
                        }
                        load()
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isCreatingInvite = false,
                                error = e.message ?: "Failed to create invite",
                            )
                        }
                    },
                )
            }
        }

        fun revokeShare(shareId: Int) {
            viewModelScope.launch {
                sharingRepository.revokeShare(childId, shareId).fold(
                    onSuccess = { load() },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(error = e.message ?: "Failed to revoke")
                        }
                    },
                )
            }
        }

        fun deleteInvite(inviteId: Int) {
            viewModelScope.launch {
                sharingRepository.deleteInvite(childId, inviteId).fold(
                    onSuccess = { load() },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(error = e.message ?: "Failed to delete invite")
                        }
                    },
                )
            }
        }
    }
