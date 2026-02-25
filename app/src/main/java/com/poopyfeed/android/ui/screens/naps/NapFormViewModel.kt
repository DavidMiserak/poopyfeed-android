package com.poopyfeed.android.ui.screens.naps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.NapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class NapFormUiState(
    val nappedAt: String = "",
    val apiError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class NapFormViewModel
    @Inject
    constructor(
        private val napsRepository: NapsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0
        private val napId: Int? = savedStateHandle.get<String>("napId")?.toIntOrNull()
        val isEditMode: Boolean = napId != null

        private val _uiState = MutableStateFlow(NapFormUiState())
        val uiState: StateFlow<NapFormUiState> = _uiState.asStateFlow()

        init {
            _uiState.update {
                it.copy(nappedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            }
            if (napId != null && childId > 0) loadNap()
        }

        private fun loadNap() {
            val nid = napId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                napsRepository.getNap(childId, nid).fold(
                    onSuccess = { n ->
                        _uiState.update {
                            it.copy(nappedAt = n.nappedAt, isLoading = false)
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(isLoading = false, apiError = e.message ?: "Failed to load")
                        }
                    },
                )
            }
        }

        fun onNappedAtChange(value: String) {
            _uiState.update { it.copy(nappedAt = value, apiError = null) }
        }

        fun submit() {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, apiError = null) }
            viewModelScope.launch {
                if (isEditMode && napId != null) {
                    napsRepository
                        .updateNap(childId, napId, state.nappedAt)
                        .fold(
                            onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                            onFailure = { e ->
                                _uiState.update {
                                    it.copy(isLoading = false, apiError = e.message ?: "Update failed")
                                }
                            },
                        )
                } else {
                    napsRepository
                        .createNap(childId, state.nappedAt)
                        .fold(
                            onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                            onFailure = { e ->
                                _uiState.update {
                                    it.copy(isLoading = false, apiError = e.message ?: "Create failed")
                                }
                            },
                        )
                }
            }
        }
    }
