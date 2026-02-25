package com.poopyfeed.android.ui.screens.children

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.ChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildFormUiState(
    val name: String = "",
    val dateOfBirth: String = "",
    val gender: String? = null,
    val nameError: String? = null,
    val dateOfBirthError: String? = null,
    val apiError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class ChildFormViewModel
    @Inject
    constructor(
        private val childrenRepository: ChildrenRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val childId: Int? = savedStateHandle.get<String>("childId")?.toIntOrNull()

        val isEditMode: Boolean = childId != null

        private val _uiState = MutableStateFlow(ChildFormUiState())
        val uiState: StateFlow<ChildFormUiState> = _uiState.asStateFlow()

        init {
            if (childId != null) {
                loadChild()
            }
        }

        private fun loadChild() {
            val id = childId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                childrenRepository.getChild(id).fold(
                    onSuccess = { child ->
                        _uiState.update {
                            it.copy(
                                name = child.name,
                                dateOfBirth = child.dateOfBirth,
                                gender = child.gender,
                                isLoading = false,
                                apiError = null,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                apiError = e.message ?: "Failed to load child",
                            )
                        }
                    },
                )
            }
        }

        fun onNameChange(name: String) {
            _uiState.update { it.copy(name = name, nameError = null, apiError = null) }
        }

        fun onDateOfBirthChange(date: String) {
            _uiState.update { it.copy(dateOfBirth = date, dateOfBirthError = null, apiError = null) }
        }

        fun onGenderChange(gender: String?) {
            _uiState.update { it.copy(gender = gender, apiError = null) }
        }

        fun submit() {
            val state = _uiState.value
            val nameError = if (state.name.isBlank()) "Name is required" else null
            val dateError = if (state.dateOfBirth.isBlank()) "Date of birth is required" else null
            if (nameError != null || dateError != null) {
                _uiState.update {
                    it.copy(nameError = nameError, dateOfBirthError = dateError)
                }
                return
            }
            _uiState.update { it.copy(isLoading = true, apiError = null) }
            viewModelScope.launch {
                if (isEditMode && childId != null) {
                    childrenRepository
                        .updateChild(
                            id = childId,
                            name = state.name,
                            dateOfBirth = state.dateOfBirth,
                            gender = state.gender,
                        )
                        .fold(
                            onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                            onFailure = { e ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        apiError = e.message ?: "Update failed",
                                    )
                                }
                            },
                        )
                } else {
                    childrenRepository
                        .createChild(
                            name = state.name,
                            dateOfBirth = state.dateOfBirth,
                            gender = state.gender,
                        )
                        .fold(
                            onSuccess = { _uiState.update { it.copy(isLoading = false, isSuccess = true) } },
                            onFailure = { e ->
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        apiError = e.message ?: "Create failed",
                                    )
                                }
                            },
                        )
                }
            }
        }
    }
