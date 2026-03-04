package com.poopyfeed.android.ui.screens.fussbus

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.remote.dto.Child
import com.poopyfeed.android.data.remote.dto.PatternAlertsResponse
import com.poopyfeed.android.data.remote.dto.TodaySummaryResponse
import com.poopyfeed.android.data.repository.AnalyticsRepository
import com.poopyfeed.android.data.repository.ChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fuss Bus symptom type (step 1). */
enum class FussBusSymptom {
    Crying,
    RefusingFood,
    WonTSleep,
    GeneralFussiness,
}

/** Single checklist item for step 2. */
data class FussBusChecklistItem(
    val id: String,
    val label: String,
    val detail: String?,
    val isChecked: Boolean,
    val isAutoChecked: Boolean,
    val isWarning: Boolean,
)

/** Sealed UI state for the Fuss Bus wizard. */
sealed class FussBusUiState {
    data object Loading : FussBusUiState()

    data class Step1Symptom(val childAgeMonths: Float) : FussBusUiState()

    data class Step2Checklist(
        val checklist: List<FussBusChecklistItem>,
        val checkedCount: Int,
        val totalCount: Int,
    ) : FussBusUiState()

    data class Step3Suggestions(val suggestions: List<String>) : FussBusUiState()

    data class Error(val message: String) : FussBusUiState()
}

@HiltViewModel
class FussBusViewModel
    @Inject
    constructor(
        private val childrenRepository: ChildrenRepository,
        private val analyticsRepository: AnalyticsRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow<FussBusUiState>(FussBusUiState.Loading)
        val uiState: StateFlow<FussBusUiState> = _uiState.asStateFlow()

        private var child: Child? = null
        private var patternAlerts: PatternAlertsResponse? = null
        private var todaySummary: TodaySummaryResponse? = null
        private var selectedSymptom: FussBusSymptom? = null
        private var manualCheckedIds: MutableSet<String> = mutableSetOf()

        init {
            loadData()
        }

        private fun loadData() {
            if (childId <= 0) {
                _uiState.value = FussBusUiState.Error("Invalid child")
                return
            }
            viewModelScope.launch {
                val childDeferred = async { childrenRepository.getChild(childId) }
                val alertsDeferred = async { analyticsRepository.getPatternAlerts(childId) }
                val summaryDeferred = async { analyticsRepository.getTodaySummary(childId) }
                val childResult = childDeferred.await()
                childResult.fold(
                    onSuccess = { c ->
                        child = c
                        patternAlerts = alertsDeferred.await().getOrNull()
                        todaySummary = summaryDeferred.await().getOrNull()
                        val ageMonths = parseAgeMonths(c.dateOfBirth)
                        _uiState.value = FussBusUiState.Step1Symptom(childAgeMonths = ageMonths)
                    },
                    onFailure = { e ->
                        _uiState.value = FussBusUiState.Error(e.message ?: "Failed to load")
                    },
                )
            }
        }

        private fun parseAgeMonths(dateOfBirth: String?): Float {
            if (dateOfBirth.isNullOrBlank()) return 0f
            return try {
                val parts = dateOfBirth.split("-").map { it.toIntOrNull() ?: return 0f }
                if (parts.size < 3) return 0f
                val birth = java.time.LocalDate.of(parts[0], parts[1], parts[2])
                val now = java.time.LocalDate.now()
                java.time.temporal.ChronoUnit.MONTHS.between(birth, now).toFloat() +
                    (java.time.temporal.ChronoUnit.DAYS.between(birth, now) % 30) / 30f
            } catch (_: Exception) {
                0f
            }
        }

        fun onSymptomSelected(symptom: FussBusSymptom) {
            selectedSymptom = symptom
            val list = buildChecklist()
            val total = list.size
            val checked = list.count { it.isChecked } + list.count { manualCheckedIds.contains(it.id) }
            _uiState.value =
                FussBusUiState.Step2Checklist(
                    checklist = list,
                    checkedCount = list.count { it.isChecked } + list.count { it.id in manualCheckedIds },
                    totalCount = total,
                )
        }

        private fun buildChecklist(): List<FussBusChecklistItem> {
            val alerts = patternAlerts
            val summary = todaySummary
            val list = mutableListOf<FussBusChecklistItem>()
            // Fed recently
            val fedChecked = alerts?.feeding?.lastFedAt != null
            list.add(
                FussBusChecklistItem(
                    id = "fed",
                    label = "Fed recently",
                    detail = if (fedChecked) "From tracking" else "No feedings logged today",
                    isChecked = fedChecked,
                    isAutoChecked = true,
                    isWarning = false,
                ),
            )
            // Clean diaper (simplified: use last 2h from child or summary)
            list.add(
                FussBusChecklistItem(
                    id = "diaper",
                    label = "Clean diaper",
                    detail = "Check if changed within last 2 hours",
                    isChecked = false,
                    isAutoChecked = false,
                    isWarning = false,
                ),
            )
            // Nap on schedule
            val napChecked = alerts?.nap?.lastNapEndedAt != null
            list.add(
                FussBusChecklistItem(
                    id = "nap",
                    label = "Nap on schedule",
                    detail = if (napChecked) "From tracking" else "Check wake window",
                    isChecked = napChecked,
                    isAutoChecked = true,
                    isWarning = false,
                ),
            )
            list.add(
                FussBusChecklistItem(
                    id = "temp",
                    label = "Comfortable temperature",
                    detail = null,
                    isChecked = manualCheckedIds.contains("temp"),
                    isAutoChecked = false,
                    isWarning = false,
                ),
            )
            list.add(
                FussBusChecklistItem(
                    id = "calm",
                    label = "Not overstimulated",
                    detail = null,
                    isChecked = manualCheckedIds.contains("calm"),
                    isAutoChecked = false,
                    isWarning = false,
                ),
            )
            return list
        }

        fun toggleChecklistItem(id: String) {
            if (manualCheckedIds.contains(id)) {
                manualCheckedIds.remove(id)
            } else {
                manualCheckedIds.add(id)
            }
            val list = buildChecklist()
            _uiState.value =
                FussBusUiState.Step2Checklist(
                    checklist = list,
                    checkedCount = list.count { item -> item.isChecked || item.id in manualCheckedIds },
                    totalCount = list.size,
                )
        }

        fun goToStep3() {
            val suggestions = buildSuggestions()
            _uiState.value = FussBusUiState.Step3Suggestions(suggestions = suggestions)
        }

        private fun buildSuggestions(): List<String> {
            val list = mutableListOf<String>()
            patternAlerts?.feeding?.message?.let { list.add(it) }
            patternAlerts?.nap?.message?.let { list.add(it) }
            list.add("Try comforting touch: rock, cuddle, or gentle massage.")
            list.add("Calming sounds: white noise, soft music, or singing.")
            if (list.isEmpty()) list.add("Baby may need comfort, feeding, or a diaper check.")
            return list
        }

        fun goToStep1() {
            val ageMonths = child?.let { parseAgeMonths(it.dateOfBirth) } ?: 0f
            _uiState.value = FussBusUiState.Step1Symptom(childAgeMonths = ageMonths)
        }

        fun goToStep2() {
            selectedSymptom?.let { onSymptomSelected(it) }
        }

        fun onDone() {
            // Navigate back is handled by screen
        }
    }
