package com.poopyfeed.android.ui.screens.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.Diaper
import com.poopyfeed.android.data.remote.dto.Feeding
import com.poopyfeed.android.data.remote.dto.Nap
import com.poopyfeed.android.data.repository.DiapersRepository
import com.poopyfeed.android.data.repository.FeedingsRepository
import com.poopyfeed.android.data.repository.NapsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Unified timeline entry for display (merged feedings, diapers, naps). */
sealed class TimelineItem {
    abstract val timestamp: String

    data class FeedingItem(val feeding: Feeding) : TimelineItem() {
        override val timestamp: String get() = feeding.fedAt
    }

    data class DiaperItem(val diaper: Diaper) : TimelineItem() {
        override val timestamp: String get() = diaper.changedAt
    }

    data class NapItem(val nap: Nap) : TimelineItem() {
        override val timestamp: String get() = nap.nappedAt
    }
}

data class TimelineUiState(
    val items: List<TimelineItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class TimelineViewModel
    @Inject
    constructor(
        private val feedingsRepository: FeedingsRepository,
        private val diapersRepository: DiapersRepository,
        private val napsRepository: NapsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(TimelineUiState())
        val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

        fun loadTimeline() {
            if (childId <= 0) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val feedingsDeferred = async { feedingsRepository.getFeedings(childId) }
                val diapersDeferred = async { diapersRepository.getDiapers(childId) }
                val napsDeferred = async { napsRepository.getNaps(childId) }
                val feedings = feedingsDeferred.await()
                val diapers = diapersDeferred.await()
                val naps = napsDeferred.await()
                val error =
                    when {
                        feedings.isFailure -> feedings.exceptionOrNull()?.message
                        diapers.isFailure -> diapers.exceptionOrNull()?.message
                        naps.isFailure -> naps.exceptionOrNull()?.message
                        else -> null
                    }
                if (error != null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = error)
                    }
                    return@launch
                }
                val fList = feedings.getOrElse { emptyList() }.map { TimelineItem.FeedingItem(it) }
                val dList = diapers.getOrElse { emptyList() }.map { TimelineItem.DiaperItem(it) }
                val nList = naps.getOrElse { emptyList() }.map { TimelineItem.NapItem(it) }
                val merged = (fList + dList + nList).sortedByDescending { it.timestamp }
                _uiState.update {
                    it.copy(items = merged, isLoading = false, error = null)
                }
            }
        }
    }
