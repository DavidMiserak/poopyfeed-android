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
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class FeedingFormUiState(
    val feedingType: String = "bottle",
    val fedAt: String = "",
    val amountOz: String = "",
    val durationMinutes: String = "",
    val side: String = "left",
    val amountError: String? = null,
    val durationError: String? = null,
    val sideError: String? = null,
    val apiError: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

@HiltViewModel
class FeedingFormViewModel
    @Inject
    constructor(
        private val feedingsRepository: FeedingsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0
        private val feedingId: Int? =
            savedStateHandle.get<String>("feedingId")?.toIntOrNull()

        val isEditMode: Boolean = feedingId != null

        private val _uiState = MutableStateFlow(FeedingFormUiState())
        val uiState: StateFlow<FeedingFormUiState> = _uiState.asStateFlow()

        init {
            _uiState.update {
                it.copy(
                    fedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                )
            }
            if (feedingId != null && childId > 0) {
                loadFeeding()
            }
        }

        private fun loadFeeding() {
            val fid = feedingId ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                feedingsRepository.getFeeding(childId, fid).fold(
                    onSuccess = { feeding ->
                        _uiState.update {
                            it.copy(
                                feedingType = feeding.feedingType,
                                fedAt = feeding.fedAt,
                                amountOz = feeding.amountOz ?: "",
                                durationMinutes = feeding.durationMinutes?.toString() ?: "",
                                side = feeding.side ?: "left",
                                isLoading = false,
                            )
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                apiError = e.message ?: "Failed to load",
                            )
                        }
                    },
                )
            }
        }

        fun onFeedingTypeChange(type: String) {
            _uiState.update {
                it.copy(
                    feedingType = type,
                    amountError = null,
                    durationError = null,
                    sideError = null,
                    apiError = null,
                )
            }
        }

        fun onFedAtChange(value: String) {
            _uiState.update { it.copy(fedAt = value, apiError = null) }
        }

        fun onAmountOzChange(value: String) {
            _uiState.update { it.copy(amountOz = value, amountError = null, apiError = null) }
        }

        fun onDurationMinutesChange(value: String) {
            _uiState.update { it.copy(durationMinutes = value, durationError = null, apiError = null) }
        }

        fun onSideChange(value: String) {
            _uiState.update { it.copy(side = value, sideError = null, apiError = null) }
        }

        fun submit() {
            val state = _uiState.value
            var amountError: String? = null
            var durationError: String? = null
            var sideError: String? = null
            if (state.feedingType == "bottle") {
                val oz = state.amountOz.toDoubleOrNull()
                if (oz == null || oz < 0.1 || oz > 50) {
                    amountError = "Amount must be between 0.1 and 50 oz"
                }
            } else {
                val min = state.durationMinutes.toIntOrNull()
                if (min == null || min < 1 || min > 180) {
                    durationError = "Duration must be 1–180 minutes"
                }
                if (state.side !in listOf("left", "right", "both")) {
                    sideError = "Select a side"
                }
            }
            if (amountError != null || durationError != null || sideError != null) {
                _uiState.update {
                    it.copy(
                        amountError = amountError,
                        durationError = durationError,
                        sideError = sideError,
                    )
                }
                return
            }
            _uiState.update { it.copy(isLoading = true, apiError = null) }
            viewModelScope.launch {
                if (isEditMode && feedingId != null) {
                    feedingsRepository
                        .updateFeeding(
                            childId = childId,
                            id = feedingId,
                            feedingType = state.feedingType,
                            fedAt = state.fedAt,
                            amountOz = state.amountOz.takeIf { state.feedingType == "bottle" },
                            durationMinutes =
                                state.durationMinutes.toIntOrNull()
                                    .takeIf { state.feedingType == "breast" },
                            side = state.side.takeIf { state.feedingType == "breast" },
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
                    feedingsRepository
                        .createFeeding(
                            childId = childId,
                            feedingType = state.feedingType,
                            fedAt = state.fedAt,
                            amountOz = state.amountOz.takeIf { state.feedingType == "bottle" },
                            durationMinutes =
                                state.durationMinutes.toIntOrNull()
                                    .takeIf { state.feedingType == "breast" },
                            side = state.side.takeIf { state.feedingType == "breast" },
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
