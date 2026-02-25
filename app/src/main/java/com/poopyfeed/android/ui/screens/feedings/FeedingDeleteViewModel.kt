package com.poopyfeed.android.ui.screens.feedings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.FeedingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedingDeleteUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val deleteSuccess: Boolean = false,
)

@HiltViewModel
class FeedingDeleteViewModel
    @Inject
    constructor(
        private val feedingsRepository: FeedingsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0
        val feedingId: Int = savedStateHandle.get<String>("feedingId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(FeedingDeleteUiState())
        val uiState: StateFlow<FeedingDeleteUiState> = _uiState.asStateFlow()

        fun delete() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                feedingsRepository.deleteFeeding(childId, feedingId).fold(
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
