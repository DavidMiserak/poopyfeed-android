package com.poopyfeed.android.ui.screens.children

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

data class ChildrenListUiState(
    val isLoading: Boolean = false,
    val children: List<Child> = emptyList(),
    val error: String? = null,
    val selectedChildId: Int? = null,
)

@HiltViewModel
class ChildrenListViewModel
    @Inject
    constructor(
        private val childrenRepository: ChildrenRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChildrenListUiState())
        val uiState: StateFlow<ChildrenListUiState> = _uiState.asStateFlow()

        init {
            loadChildren()
        }

        fun loadChildren() {
            _uiState.update { it.copy(isLoading = true, error = null) }

            viewModelScope.launch {
                val result = childrenRepository.getChildren()
                result.fold(
                    onSuccess = { children ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                children = children,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load children",
                            )
                        }
                    },
                )
            }
        }

        fun selectChild(childId: Int) {
            _uiState.update { it.copy(selectedChildId = childId) }
        }

    }
