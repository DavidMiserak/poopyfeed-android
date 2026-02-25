package com.poopyfeed.android.ui.screens.children

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.repository.ChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildDeleteUiState(
    val child: Child? = null,
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val deleteSuccess: Boolean = false,
)

@HiltViewModel
class ChildDeleteViewModel
    @Inject
    constructor(
        private val childrenRepository: ChildrenRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(ChildDeleteUiState())
        val uiState: StateFlow<ChildDeleteUiState> = _uiState.asStateFlow()

        init {
            if (childId > 0) loadChild()
        }

        private fun loadChild() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                childrenRepository.getChild(childId).fold(
                    onSuccess = { child ->
                        _uiState.update {
                            it.copy(child = child, isLoading = false, error = null)
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to load child",
                            )
                        }
                    },
                )
            }
        }

        fun deleteChild() {
            if (childId <= 0) return
            viewModelScope.launch {
                _uiState.update { it.copy(isDeleting = true, error = null) }
                childrenRepository.deleteChild(childId).fold(
                    onSuccess = {
                        _uiState.update { it.copy(isDeleting = false, deleteSuccess = true) }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isDeleting = false,
                                error = e.message ?: "Delete failed",
                            )
                        }
                    },
                )
            }
        }
    }
