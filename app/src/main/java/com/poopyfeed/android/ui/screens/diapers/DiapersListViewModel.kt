package com.poopyfeed.android.ui.screens.diapers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.Diaper
import com.poopyfeed.android.data.repository.DiapersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiapersListUiState(
    val diapers: List<Diaper> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class DiapersListViewModel
    @Inject
    constructor(
        private val diapersRepository: DiapersRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(DiapersListUiState())
        val uiState: StateFlow<DiapersListUiState> = _uiState.asStateFlow()

        init {
            loadDiapers()
        }

        fun loadDiapers() {
            if (childId <= 0) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                diapersRepository.getDiapers(childId).fold(
                    onSuccess = { list ->
                        _uiState.update {
                            it.copy(diapers = list, isLoading = false, error = null)
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load diapers",
                            )
                        }
                    },
                )
            }
        }
    }
