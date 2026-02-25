package com.poopyfeed.android.ui.screens.diapers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.DiapersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DiaperFormUiState(
    val changeType: String = "wet",
    val changedAt: String = "",
    val apiError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class DiaperFormViewModel
    @Inject
    constructor(
        private val diapersRepository: DiapersRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0
        private val diaperId: Int? = savedStateHandle.get<String>("diaperId")?.toIntOrNull()
        val isEditMode: Boolean = diaperId != null

        private val _uiState = MutableStateFlow(DiaperFormUiState())
        val uiState: StateFlow<DiaperFormUiState> = _uiState.asStateFlow()

        init {
            _uiState.update {
                it.copy(changedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            }
            if (diaperId != null && childId > 0) loadDiaper()
        }

        private fun loadDiaper() {
            val did = diaperId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                diapersRepository.getDiaper(childId, did).fold(
                    onSuccess = { d ->
                        _uiState.update {
                            it.copy(
                                changeType = d.changeType,
                                changedAt = d.changedAt,
                                isLoading = false,
                            )
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

        fun onChangeTypeChange(value: String) {
            _uiState.update { it.copy(changeType = value, apiError = null) }
        }

        fun onChangedAtChange(value: String) {
            _uiState.update { it.copy(changedAt = value, apiError = null) }
        }

        fun submit() {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, apiError = null) }
            viewModelScope.launch {
                if (isEditMode && diaperId != null) {
                    diapersRepository
                        .updateDiaper(childId, diaperId, state.changeType, state.changedAt)
                        .fold(
                            onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                            onFailure = { e ->
                                _uiState.update {
                                    it.copy(isLoading = false, apiError = e.message ?: "Update failed")
                                }
                            },
                        )
                } else {
                    diapersRepository
                        .createDiaper(childId, state.changeType, state.changedAt)
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
