package net.poopyfeed.pf.naps

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateNapRequest
import net.poopyfeed.pf.data.models.toToastMessage
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.sync.SyncScheduler
import net.poopyfeed.pf.ui.toast.ToastManager
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
    private val tokenManager: TokenManager,
    private val toastManager: ToastManager,
) : ViewModel() {

  private val childId: Int =
      savedStateHandle.get<Int>("childId")
          ?: throw IllegalArgumentException("CreateNapViewModel requires childId argument")

  private val _uiState: MutableStateFlow<CreateNapUiState> = MutableStateFlow(CreateNapUiState.Idle)
  val uiState: StateFlow<CreateNapUiState> = _uiState.asStateFlow()

  private val _startTime: MutableStateFlow<String> = MutableStateFlow("")
  private val _endTime: MutableStateFlow<String?> = MutableStateFlow(null)

  val proposedDuration: StateFlow<String> =
      combine(_startTime, _endTime) { start, end ->
            if (start.isEmpty() || end == null) {
              return@combine ""
            }
            try {
              val startMs = Instant.parse(start).toEpochMilliseconds()
              val endMs = Instant.parse(end).toEpochMilliseconds()
              val diffMs = endMs - startMs
              val totalMinutes = (diffMs / (60 * 1000)).toInt()
              when {
                totalMinutes < 60 -> "${totalMinutes}m"
                totalMinutes % 60 == 0 -> "${totalMinutes / 60}h"
                else -> "${totalMinutes / 60}h ${totalMinutes % 60}m"
              }
            } catch (_: Exception) {
              ""
            }
          }
          .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, "")

  fun setStartTime(startTime: String) {
    _startTime.value = startTime
  }

  fun setEndTime(endTime: String?) {
    _endTime.value = endTime
  }

  /**
   * Converts a local time string (in the profile timezone) to UTC ISO 8601 format. Used when user
   * picks a time in their profile timezone via the date picker. For example, if the profile is in
   * Los Angeles (UTC-8) and the user picks "09:00:00", this converts to "17:00:00Z" (UTC).
   *
   * @param profileLocalTime Local datetime string without timezone (e.g. "2024-01-15T09:00:00")
   * @return UTC ISO 8601 string with Z suffix (e.g. "2024-01-15T17:00:00Z")
   */
  fun convertLocalTimeToUtc(profileLocalTime: String): String {
    return try {
      val tzId = tokenManager.getProfileTimezone() ?: "UTC"
      val javaLocalDateTime = JavaLocalDateTime.parse(profileLocalTime)
      val zoneId = ZoneId.of(tzId)
      val zonedDateTime = javaLocalDateTime.atZone(zoneId)
      val instant = zonedDateTime.toInstant()
      // Convert to ISO 8601 UTC format (with Z suffix)
      Instant.fromEpochMilliseconds(instant.toEpochMilli()).toString()
    } catch (e: Exception) {
      profileLocalTime + "Z" // Fallback to treating as UTC if parsing fails
    }
  }

  /**
   * Converts a UTC ISO 8601 timestamp to a local time string in the profile timezone. Used for
   * displaying times to the user. For example, if the profile is in Los Angeles (UTC-8) and the UTC
   * time is "17:00:00Z", this returns "09:00:00" (local time).
   *
   * @param utcTime UTC ISO 8601 string (e.g. "2024-01-15T17:00:00Z")
   * @return Local datetime string without timezone (e.g. "2024-01-15T09:00:00")
   */
  fun convertUtcTimeToLocal(utcTime: String): String {
    return try {
      val tzId = tokenManager.getProfileTimezone() ?: "UTC"
      val instant = Instant.parse(utcTime)
      val localDateTime = instant.toLocalDateTime(TimeZone.of(tzId))
      localDateTime.toString()
    } catch (e: Exception) {
      utcTime // Fallback to UTC time if parsing fails
    }
  }

  /**
   * Extracts hour and minute components from a UTC ISO 8601 timestamp, converted to the profile
   * timezone. Used for initializing MaterialTimePicker with the correct local time.
   *
   * @param utcTime UTC ISO 8601 string (e.g., "2024-01-15T17:00:00Z")
   * @return Pair of (hour, minute) in profile timezone (e.g., Pair(9, 0) for Los Angeles when UTC
   *   is 17:00)
   */
  fun getCalendarHourMinuteForPicker(utcTime: String): Pair<Int, Int> {
    return try {
      val tzId = tokenManager.getProfileTimezone() ?: "UTC"
      val instant = Instant.parse(utcTime)
      val localDateTime = instant.toLocalDateTime(TimeZone.of(tzId))
      Pair(localDateTime.hour, localDateTime.minute)
    } catch (e: Exception) {
      Pair(0, 0) // Fallback to midnight
    }
  }

  /**
   * Gets the epoch milliseconds for midnight UTC of the date in profile timezone. Used to set
   * MaterialDatePicker selection to the correct profile timezone date.
   *
   * MaterialDatePicker uses UTC internally: setSelection() expects midnight UTC of the desired
   * date, and the returned millis from onPositiveButtonClick is midnight UTC of the selected date.
   *
   * @param utcTime UTC ISO 8601 string
   * @return Epoch milliseconds representing midnight UTC of the profile timezone date
   */
  fun getDatePickerSelectionMillisForProfileTz(utcTime: String): Long {
    return try {
      val tzId = tokenManager.getProfileTimezone() ?: "UTC"
      val instant = Instant.parse(utcTime)
      val localDateTime = instant.toLocalDateTime(TimeZone.of(tzId))
      val localDate = localDateTime.date

      // MaterialDatePicker expects midnight UTC for the desired date.
      // Convert profile timezone date to midnight UTC (not midnight profile TZ).
      val javaLocalDateTime =
          JavaLocalDateTime.of(localDate.year, localDate.monthNumber, localDate.dayOfMonth, 0, 0, 0)
      val zonedDateTime = javaLocalDateTime.atZone(ZoneId.of("UTC"))
      zonedDateTime.toInstant().toEpochMilli()
    } catch (e: Exception) {
      System.currentTimeMillis() // Fallback to today
    }
  }

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
              toastManager.showSuccess("✓ Nap recorded")
              CreateNapUiState.Success
            }
            is ApiResult.Error -> {
              handleAndLogError(analyticsTracker, result.error, "createNap")
              toastManager.showError(result.error.toToastMessage())
              CreateNapUiState.Error(result.error.getUserMessage(context))
            }
            is ApiResult.Loading -> CreateNapUiState.Saving
          }
    }
  }
}
