package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateDiaperRequest
import net.poopyfeed.pf.data.models.CreateFeedingRequest
import net.poopyfeed.pf.data.models.CreateNapRequest
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.data.repository.CachedNapsRepository

/** UI state for quick-log diaper creation (instant log with time = now). */
sealed interface QuickLogDiaperUiState {
  data object Idle : QuickLogDiaperUiState

  data object Saving : QuickLogDiaperUiState

  data object Success : QuickLogDiaperUiState

  data class Error(val message: String) : QuickLogDiaperUiState
}

/** UI state for quick-log feeding creation (bottle, time = now). */
sealed interface QuickLogFeedingUiState {
  data object Idle : QuickLogFeedingUiState

  data object Saving : QuickLogFeedingUiState

  data object Success : QuickLogFeedingUiState

  data class Error(val message: String) : QuickLogFeedingUiState
}

/** UI state for quick-log nap creation (start time = now, in progress). */
sealed interface QuickLogNapUiState {
  data object Idle : QuickLogNapUiState

  data object Saving : QuickLogNapUiState

  data object Success : QuickLogNapUiState

  data class Error(val message: String) : QuickLogNapUiState
}

/** Per-child bottle amounts for quick log (from backend custom_bottle_*_oz). */
data class BottleAmounts(val lowOz: Double, val midOz: Double, val highOz: Double)

private fun parseBottleOz(value: String?): Double =
    value?.toDoubleOrNull()?.coerceIn(0.1, 32.0) ?: 2.0

private val DEFAULT_BOTTLE_AMOUNTS = BottleAmounts(2.0, 4.0, 6.0)

/**
 * ViewModel for [ChildDetailQuickLogBottomSheetFragment]. Handles instant diaper and bottle feeding
 * creation with time always "now". Uses child's custom_bottle_*_oz from backend for the three
 * bottle options.
 */
@HiltViewModel
class ChildDetailQuickLogViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val childrenRepo: CachedChildrenRepository,
    private val diapersRepo: CachedDiapersRepository,
    private val feedingsRepo: CachedFeedingsRepository,
    private val napsRepo: CachedNapsRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  /** Bottle amounts for the current child (from API); defaults to 2, 4, 6 oz until loaded. */
  val bottleAmounts: StateFlow<BottleAmounts> =
      childrenRepo
          .getChildCached(childId)
          .map { child ->
            if (child == null) DEFAULT_BOTTLE_AMOUNTS
            else
                BottleAmounts(
                    lowOz = parseBottleOz(child.custom_bottle_low_oz),
                    midOz = parseBottleOz(child.custom_bottle_mid_oz),
                    highOz = parseBottleOz(child.custom_bottle_high_oz),
                )
          }
          .catch { emit(DEFAULT_BOTTLE_AMOUNTS) }
          .stateIn(
              scope = viewModelScope,
              started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
              initialValue = DEFAULT_BOTTLE_AMOUNTS,
          )

  private val _diaperState: MutableStateFlow<QuickLogDiaperUiState> =
      MutableStateFlow(QuickLogDiaperUiState.Idle)
  val diaperState: StateFlow<QuickLogDiaperUiState> = _diaperState.asStateFlow()

  private val _feedingState: MutableStateFlow<QuickLogFeedingUiState> =
      MutableStateFlow(QuickLogFeedingUiState.Idle)
  val feedingState: StateFlow<QuickLogFeedingUiState> = _feedingState.asStateFlow()

  private val _napState: MutableStateFlow<QuickLogNapUiState> =
      MutableStateFlow(QuickLogNapUiState.Idle)
  val napState: StateFlow<QuickLogNapUiState> = _napState.asStateFlow()

  /** Creates a diaper log with [changeType] (wet, dirty, both) and timestamp = now. */
  fun createDiaperNow(changeType: String) {
    if (changeType.isBlank()) return
    viewModelScope.launch {
      _diaperState.value = QuickLogDiaperUiState.Saving
      val timestamp = Clock.System.now().toString()
      val request = CreateDiaperRequest(change_type = changeType.trim(), timestamp = timestamp)
      val result = diapersRepo.createDiaper(childId, request)
      _diaperState.value =
          when (result) {
            is ApiResult.Success -> QuickLogDiaperUiState.Success
            is ApiResult.Error -> QuickLogDiaperUiState.Error(result.error.getUserMessage(context))
            is ApiResult.Loading -> QuickLogDiaperUiState.Saving
          }
    }
  }

  /** Creates a bottle feeding with [amountOz] and timestamp = now. */
  fun createBottleNow(amountOz: Double) {
    if (amountOz <= 0) return
    viewModelScope.launch {
      _feedingState.value = QuickLogFeedingUiState.Saving
      val timestamp = Clock.System.now().toString()
      val request =
          CreateFeedingRequest(
              feeding_type = "bottle",
              amount_oz = amountOz,
              durationMinutes = null,
              side = null,
              timestamp = timestamp,
          )
      val result = feedingsRepo.createFeeding(childId, request)
      _feedingState.value =
          when (result) {
            is ApiResult.Success -> QuickLogFeedingUiState.Success
            is ApiResult.Error -> QuickLogFeedingUiState.Error(result.error.getUserMessage(context))
            is ApiResult.Loading -> QuickLogFeedingUiState.Saving
          }
    }
  }

  /** Starts a nap with start time = now (in progress). */
  fun createNapNow() {
    viewModelScope.launch {
      _napState.value = QuickLogNapUiState.Saving
      val startTime = Clock.System.now().toString()
      val request = CreateNapRequest(start_time = startTime, end_time = null)
      val result = napsRepo.createNap(childId, request)
      _napState.value =
          when (result) {
            is ApiResult.Success -> QuickLogNapUiState.Success
            is ApiResult.Error -> QuickLogNapUiState.Error(result.error.getUserMessage(context))
            is ApiResult.Loading -> QuickLogNapUiState.Saving
          }
    }
  }
}
