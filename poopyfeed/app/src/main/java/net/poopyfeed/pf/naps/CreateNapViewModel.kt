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

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

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
              // Calculate duration if end_time is present
              val nap = result.data
              if (nap.end_time != null) {
                try {
                  val startMs = Instant.parse(nap.start_time).toEpochMilliseconds()
                  val endMs = Instant.parse(nap.end_time).toEpochMilliseconds()
                  val durationMinutes = ((endMs - startMs) / (60 * 1000)).toInt()
                  analyticsTracker.logNapLogged(durationMinutes)
                } catch (e: Exception) {
                  // Silent fail - don't block UI on analytics error
                }
              }
              CreateNapUiState.Success
            }
            is ApiResult.Error -> CreateNapUiState.Error(result.error.getUserMessage(context))
            is ApiResult.Loading -> CreateNapUiState.Saving
          }
    }
  }
}
