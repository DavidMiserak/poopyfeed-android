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

        companion object {
            fun getChildAge(dateOfBirth: String): String {
                return try {
                    val birthDate = dateOfBirth.split("-").map { it.toInt() }
                    if (birthDate.size < 3) return "Unknown age"

                    val year = birthDate[0]
                    val month = birthDate[1]
                    val day = birthDate[2]

                    val today = java.util.Calendar.getInstance()
                    val birthCalendar =
                        java.util.Calendar.getInstance().apply {
                            set(year, month - 1, day)
                        }

                    var years = today.get(java.util.Calendar.YEAR) - birthCalendar.get(java.util.Calendar.YEAR)
                    var months = today.get(java.util.Calendar.MONTH) - birthCalendar.get(java.util.Calendar.MONTH)

                    if (months < 0) {
                        years--
                        months += 12
                    }

                    if (today.get(java.util.Calendar.DAY_OF_MONTH) < birthCalendar.get(java.util.Calendar.DAY_OF_MONTH)) {
                        months--
                        if (months < 0) {
                            years--
                            months += 12
                        }
                    }

                    when {
                        years > 0 && months > 0 -> "$years y $months m"
                        years > 0 -> "$years y"
                        months > 0 -> "$months m"
                        else -> "0 m"
                    }
                } catch (e: Exception) {
                    "Unknown age"
                }
            }

            fun getGenderEmoji(gender: String?): String {
                return when (gender?.uppercase()) {
                    "M" -> "👦"
                    "F" -> "👧"
                    else -> "👶"
                }
            }
        }
    }
