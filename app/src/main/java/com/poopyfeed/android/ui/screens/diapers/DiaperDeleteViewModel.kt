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
import javax.inject.Inject

data class DiaperDeleteUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleteSuccess: Boolean = false,
)

@HiltViewModel
class DiaperDeleteViewModel
    @Inject
    constructor(
        private val diapersRepository: DiapersRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0
        val diaperId: Int = savedStateHandle.get<String>("diaperId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(DiaperDeleteUiState())
        val uiState: StateFlow<DiaperDeleteUiState> = _uiState.asStateFlow()

        fun delete() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                diapersRepository.deleteDiaper(childId, diaperId).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, deleteSuccess = true) }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Delete failed",
                            )
                        }
                    },
                )
            }
        }
    }
