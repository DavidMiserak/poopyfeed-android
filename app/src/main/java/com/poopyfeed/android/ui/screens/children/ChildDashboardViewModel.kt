package com.poopyfeed.android.ui.screens.children

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.TodaySummaryResponse
import com.poopyfeed.android.data.repository.AnalyticsRepository
import com.poopyfeed.android.data.repository.ChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildDashboardUiState(
    val child: Child? = null,
    val todaySummary: TodaySummaryResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChildDashboardViewModel
    @Inject
    constructor(
        private val childrenRepository: ChildrenRepository,
        private val analyticsRepository: AnalyticsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {

        private val childId: Int =
            savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(ChildDashboardUiState())
        val uiState: StateFlow<ChildDashboardUiState> = _uiState.asStateFlow()

        init {
            loadDashboard()
        }

        fun loadDashboard() {
            if (childId <= 0) {
                _uiState.update { it.copy(error = "Invalid child") }
                return
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            viewModelScope.launch {
                val childDeferred = async { childrenRepository.getChild(childId) }
                val summaryDeferred = async { analyticsRepository.getTodaySummary(childId) }
                val childResult = childDeferred.await()
                val summaryResult = summaryDeferred.await()
                childResult.fold(
                    onSuccess = { child ->
                        _uiState.update {
                            it.copy(
                                child = child,
                                todaySummary = summaryResult.getOrNull(),
                                isLoading = false,
                                error = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Failed to load dashboard",
                            )
                        }
                    },
                )
            }
        }
    }
