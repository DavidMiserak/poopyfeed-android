package com.poopyfeed.android.ui.screens.catchup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.BatchRepository
import com.poopyfeed.android.data.repository.CatchUpBatchEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CatchUpUiState(
    val events: List<CatchUpBatchEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val submitSuccess: Boolean = false,
)

@HiltViewModel
class CatchUpViewModel
    @Inject
    constructor(
        private val batchRepository: BatchRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(CatchUpUiState())
        val uiState: StateFlow<CatchUpUiState> = _uiState.asStateFlow()

        fun addFeeding() {
            val at = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            _uiState.update {
                it.copy(
                    events = it.events + CatchUpBatchEvent.Feeding(fedAt = at, feedingType = "bottle", amountOz = 4.0),
                    error = null,
                )
            }
        }

        fun addDiaper() {
            val at = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            _uiState.update {
                it.copy(
                    events = it.events + CatchUpBatchEvent.Diaper(changedAt = at, changeType = "wet"),
                    error = null,
                )
            }
        }

        fun addNap() {
            val at = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            _uiState.update {
                it.copy(
                    events = it.events + CatchUpBatchEvent.Nap(nappedAt = at),
                    error = null,
                )
            }
        }

        fun removeAt(index: Int) {
            if (index in _uiState.value.events.indices) {
                _uiState.update {
                    it.copy(events = it.events.toMutableList().apply { removeAt(index) })
                }
            }
        }

        fun submit(onSuccess: () -> Unit) {
            val events = _uiState.value.events
            if (events.isEmpty()) {
                _uiState.update { it.copy(error = "Add at least one event.") }
                return
            }
            if (childId <= 0) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                batchRepository.createBatch(childId, events).fold(
                    onSuccess = { response ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = null,
                                submitSuccess = true,
                                events = emptyList(),
                            )
                        }
                        onSuccess()
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "Failed to save events",
                            )
                        }
                    },
                )
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }
    }
