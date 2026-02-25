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
import javax.inject.Inject

data class NapDeleteUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleteSuccess: Boolean = false,
)

@HiltViewModel
class NapDeleteViewModel
    @Inject
    constructor(
        private val napsRepository: NapsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0
        val napId: Int = savedStateHandle.get<String>("napId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(NapDeleteUiState())
        val uiState: StateFlow<NapDeleteUiState> = _uiState.asStateFlow()

        fun delete() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                napsRepository.deleteNap(childId, napId).fold(
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
