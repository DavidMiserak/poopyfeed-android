package net.poopyfeed.pf.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.models.NotificationPreference
import net.poopyfeed.pf.data.models.UpdateChildRequest
import net.poopyfeed.pf.data.models.UpdateNotificationPreferenceRequest
import net.poopyfeed.pf.data.repository.CachedChildrenRepository

/** UI state for the edit child bottom sheet. */
sealed interface EditChildUiState {
  /** Loading child data. */
  data object Loading : EditChildUiState

  /** Child loaded; form can be edited. [canEditReminder] true for owner/co-parent. */
  data class Ready(
      val child: Child,
      val canEditReminder: Boolean,
  ) : EditChildUiState

  /** Failed to load child. */
  data class Error(val message: String) : EditChildUiState

  /** Saving in progress. */
  data object Saving : EditChildUiState

  /** Save succeeded; fragment should dismiss. */
  data object Success : EditChildUiState

  /** Save failed with API/network error. */
  data class SaveError(val message: String) : EditChildUiState

  /** Validation error (e.g. empty name). */
  data class ValidationError(
      val nameError: String? = null,
      val dobError: String? = null,
      val bottleError: String? = null,
  ) : EditChildUiState
}

/** Notification preference loading state. */
sealed interface NotificationPrefState {
  data object Loading : NotificationPrefState

  data class Loaded(val pref: NotificationPreference) : NotificationPrefState

  data class Error(val message: String) : NotificationPrefState
}

/**
 * ViewModel for [EditChildFragment]. Loads child by ID, exposes form state, and saves updates
 * (name, DOB, gender, feeding reminder interval, custom bottle amounts) via PATCH. Also manages
 * per-child notification preferences. Feeding reminder and notification preferences are only
 * relevant when [can_edit] is true (owner/co-parent).
 */
@HiltViewModel
class EditChildViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedChildrenRepository,
    private val apiService: PoopyFeedApiService,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<EditChildUiState> =
      MutableStateFlow(EditChildUiState.Loading)
  val uiState: StateFlow<EditChildUiState> = _uiState.asStateFlow()

  private val _deleteSuccess: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 0)
  val deleteSuccess: SharedFlow<Unit> = _deleteSuccess.asSharedFlow()

  private val _deleteError: MutableSharedFlow<String> = MutableSharedFlow(replay = 0)
  val deleteError: SharedFlow<String> = _deleteError.asSharedFlow()

  private val _notificationPrefState: MutableStateFlow<NotificationPrefState?> =
      MutableStateFlow(null)
  val notificationPrefState: StateFlow<NotificationPrefState?> =
      _notificationPrefState.asStateFlow()

  private val _preferenceSaving: MutableStateFlow<Boolean> = MutableStateFlow(false)
  val preferenceSaving: StateFlow<Boolean> = _preferenceSaving.asStateFlow()

  init {
    viewModelScope.launch {
      repo
          .getChildCached(childId)
          .catch { e ->
            _uiState.value =
                EditChildUiState.Error(
                    e.message
                        ?: context.getString(net.poopyfeed.pf.R.string.child_detail_error_generic))
          }
          .map { child ->
            if (child == null) {
              EditChildUiState.Error(
                  context.getString(net.poopyfeed.pf.R.string.child_detail_error_generic))
            } else {
              EditChildUiState.Ready(child = child, canEditReminder = child.can_edit)
            }
          }
          .collect { state ->
            // Only transition from Loading to Ready/Error; don't overwrite Saving/Success/SaveError
            if (_uiState.value is EditChildUiState.Loading) {
              _uiState.value = state
              if (state is EditChildUiState.Ready) {
                loadNotificationPreference()
              }
            }
          }
    }
  }

  /** Loads the notification preference for this child from the API. */
  private fun loadNotificationPreference() {
    _notificationPrefState.value = NotificationPrefState.Loading
    viewModelScope.launch {
      try {
        val prefs = apiService.getNotificationPreferences()
        val pref = prefs.find { it.childId == childId }
        if (pref != null) {
          _notificationPrefState.value = NotificationPrefState.Loaded(pref)
        } else {
          _notificationPrefState.value = null
        }
      } catch (e: Exception) {
        _notificationPrefState.value =
            NotificationPrefState.Error(
                context.getString(net.poopyfeed.pf.R.string.notification_pref_error))
      }
    }
  }

  /** Toggles a notification preference field and persists via PATCH. */
  fun toggleNotificationPref(field: String, value: Boolean) {
    val current = _notificationPrefState.value
    if (current !is NotificationPrefState.Loaded) return

    _preferenceSaving.value = true
    viewModelScope.launch {
      try {
        val request =
            when (field) {
              "notify_feedings" -> UpdateNotificationPreferenceRequest(notifyFeedings = value)
              "notify_diapers" -> UpdateNotificationPreferenceRequest(notifyDiapers = value)
              "notify_naps" -> UpdateNotificationPreferenceRequest(notifyNaps = value)
              else -> return@launch
            }
        val updated = apiService.updateNotificationPreference(current.pref.id, request)
        _notificationPrefState.value = NotificationPrefState.Loaded(updated)
        _preferenceSaving.value = false
      } catch (e: Exception) {
        _preferenceSaving.value = false
      }
    }
  }

  /**
   * Saves the form. Validates name, DOB, and bottle amounts; builds [UpdateChildRequest] and
   * PATCHes. On success emits [EditChildUiState.Success]; on error [EditChildUiState.SaveError] or
   * [EditChildUiState.ValidationError].
   */
  fun save(
      name: String,
      dateOfBirth: String,
      gender: String,
      feedingReminderIntervalHours: Int?,
      bottleLow: String?,
      bottleMid: String?,
      bottleHigh: String?,
  ) {
    val nameError = if (name.trim().isEmpty()) "Name is required" else null
    val dobError = if (dateOfBirth.isEmpty()) "Date of birth is required" else null
    val bottleError = validateBottleAmounts(bottleLow, bottleMid, bottleHigh)
    if (nameError != null || dobError != null || bottleError != null) {
      _uiState.value =
          EditChildUiState.ValidationError(
              nameError = nameError, dobError = dobError, bottleError = bottleError)
      return
    }

    viewModelScope.launch {
      val current = _uiState.value
      if (current !is EditChildUiState.Ready) return@launch

      _uiState.value = EditChildUiState.Saving

      val request =
          UpdateChildRequest(
              name = name.trim(),
              date_of_birth = dateOfBirth,
              gender = gender,
              feeding_reminder_interval = feedingReminderIntervalHours,
              custom_bottle_low_oz = bottleLow?.ifBlank { null },
              custom_bottle_mid_oz = bottleMid?.ifBlank { null },
              custom_bottle_high_oz = bottleHigh?.ifBlank { null },
          )

      val result = repo.updateChild(childId, request)
      _uiState.update {
        when (result) {
          is ApiResult.Success -> EditChildUiState.Success
          is ApiResult.Error -> EditChildUiState.SaveError(result.error.getUserMessage(context))
          is ApiResult.Loading -> EditChildUiState.Saving
        }
      }
    }
  }

  /**
   * Validates custom bottle amounts. Returns an error string if invalid, null if valid. Rules: all
   * blank is valid, all three set with low < mid < high is valid, partial is invalid.
   */
  private fun validateBottleAmounts(low: String?, mid: String?, high: String?): String? {
    val lowVal = low?.toDoubleOrNull()
    val midVal = mid?.toDoubleOrNull()
    val highVal = high?.toDoubleOrNull()
    val lowBlank = low.isNullOrBlank()
    val midBlank = mid.isNullOrBlank()
    val highBlank = high.isNullOrBlank()

    // All blank is valid (use defaults)
    if (lowBlank && midBlank && highBlank) return null

    // Partial fill
    if (lowBlank || midBlank || highBlank) {
      return context.getString(net.poopyfeed.pf.R.string.bottle_amount_error_partial)
    }

    // Range check
    if (lowVal == null || midVal == null || highVal == null) {
      return context.getString(net.poopyfeed.pf.R.string.bottle_amount_error_range)
    }
    if (lowVal < 0.1 ||
        lowVal > 50 ||
        midVal < 0.1 ||
        midVal > 50 ||
        highVal < 0.1 ||
        highVal > 50) {
      return context.getString(net.poopyfeed.pf.R.string.bottle_amount_error_range)
    }

    // Order check
    if (lowVal >= midVal) {
      return context.getString(net.poopyfeed.pf.R.string.bottle_amount_error_order_low_mid)
    }
    if (midVal >= highVal) {
      return context.getString(net.poopyfeed.pf.R.string.bottle_amount_error_order_mid_high)
    }

    return null
  }

  /** Deletes the child from the API and cache. Emits [deleteSuccess] or [deleteError]. */
  fun deleteChild() {
    viewModelScope.launch {
      val result = repo.deleteChild(childId)
      when (result) {
        is ApiResult.Success -> _deleteSuccess.emit(Unit)
        is ApiResult.Error -> _deleteError.emit(result.error.getUserMessage(context))
        is ApiResult.Loading -> {
          /* no-op */
        }
      }
    }
  }
}
