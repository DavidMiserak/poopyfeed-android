package com.poopyfeed.android.ui.screens.naps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.Nap
import com.poopyfeed.android.data.repository.NapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NapsListUiState(
    val naps: List<Nap> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class NapsListViewModel
    @Inject
    constructor(
        private val napsRepository: NapsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(NapsListUiState())
        val uiState: StateFlow<NapsListUiState> = _uiState.asStateFlow()

        init {
            loadNaps()
        }

        fun loadNaps() {
            if (childId <= 0) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                napsRepository.getNaps(childId).fold(
                    onSuccess = { list ->
                        _uiState.update {
                            it.copy(naps = list, isLoading = false, error = null)
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load naps",
                            )
                        }
                    },
                )
            }
        }
    }
