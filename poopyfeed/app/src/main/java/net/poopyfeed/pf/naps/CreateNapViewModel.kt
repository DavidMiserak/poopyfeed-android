package net.poopyfeed.pf.naps

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
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateNapRequest
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.sync.SyncScheduler
import net.poopyfeed.pf.util.handleAndLogError

/** UI state for the create nap bottom sheet. */
sealed interface CreateNapUiState {
  data object Idle : CreateNapUiState

  data object Saving : CreateNapUiState

  data object Success : CreateNapUiState

  data class Error(val message: String) : CreateNapUiState
}

/**
 * ViewModel for [CreateNapBottomSheetFragment]. Creates a nap with start time and optional end
 * time.
 */
@HiltViewModel
class CreateNapViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedNapsRepository,
    private val syncScheduler: SyncScheduler,
    private val analyticsTracker: AnalyticsTracker,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int =
      savedStateHandle.get<Int>("childId")
          ?: throw IllegalArgumentException("CreateNapViewModel requires childId argument")

  private val _uiState: MutableStateFlow<CreateNapUiState> = MutableStateFlow(CreateNapUiState.Idle)
  val uiState: StateFlow<CreateNapUiState> = _uiState.asStateFlow()

  fun createNap(startTime: String, endTime: String? = null) {
    viewModelScope.launch {
      _uiState.value = CreateNapUiState.Saving
      val request = CreateNapRequest(start_time = startTime, end_time = endTime)
      val result = repo.createNap(childId, request)
      _uiState.value =
          when (result) {
            is ApiResult.Success -> {
              syncScheduler.enqueueIfPending()
              val nap = result.data
              // Calculate and log duration; use -1 for open-ended naps
              val durationMinutes =
                  if (nap.end_time != null) {
                    try {
                      val startMs = Instant.parse(nap.start_time).toEpochMilliseconds()
                      val endMs = Instant.parse(nap.end_time).toEpochMilliseconds()
                      ((endMs - startMs) / (60 * 1000)).toInt()
                    } catch (e: Exception) {
                      analyticsTracker.logError(
                          "NapDurationCalculationError", e.message ?: "Unknown exception")
                      0 // Log with 0 duration if calculation fails
                    }
                  } else {
                    -1 // Open-ended nap (no end_time set)
                  }
              analyticsTracker.logNapLogged(durationMinutes)
              CreateNapUiState.Success
            }
            is ApiResult.Error -> {
              handleAndLogError(analyticsTracker, result.error, "createNap")
              CreateNapUiState.Error(result.error.getUserMessage(context))
            }
            is ApiResult.Loading -> CreateNapUiState.Saving
          }
    }
  }
}
