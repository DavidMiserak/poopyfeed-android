package com.poopyfeed.android.ui.screens.feedings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.Feeding
import com.poopyfeed.android.data.repository.FeedingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedingsListUiState(
    val feedings: List<Feeding> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class FeedingsListViewModel
    @Inject
    constructor(
        private val feedingsRepository: FeedingsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(FeedingsListUiState())
        val uiState: StateFlow<FeedingsListUiState> = _uiState.asStateFlow()

        init {
            loadFeedings()
        }

        fun loadFeedings() {
            if (childId <= 0) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                feedingsRepository.getFeedings(childId).fold(
                    onSuccess = { list ->
                        _uiState.update {
                            it.copy(feedings = list, isLoading = false, error = null)
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load feedings",
                            )
                        }
                    },
                )
            }
        }
    }
