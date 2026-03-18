package net.poopyfeed.pf.sharing

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
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.ChildInvite
import net.poopyfeed.pf.data.repository.SharingRepository

/** UI state for the manage sharing screen. */
sealed interface SharingUiState {
  data object Loading : SharingUiState

  data class Ready(val items: List<SharingListItem>) : SharingUiState

  data class Error(val message: String) : SharingUiState
}

/**
 * ViewModel for [SharingFragment]. Loads invite links and people with access; supports refresh,
 * toggle/delete invite, and create invite via bottom sheet.
 */
@HiltViewModel
class SharingViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val sharingRepository: SharingRepository,
    @param:ApplicationContext private val context: Context,
    val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<SharingUiState> = MutableStateFlow(SharingUiState.Loading)
  val uiState: StateFlow<SharingUiState> = _uiState.asStateFlow()

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

  private val _errorMessage = MutableSharedFlow<String>(replay = 0)
  val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

  init {
    viewModelScope.launch {
      _isRefreshing.value = true
      load()
      _isRefreshing.value = false
    }
  }

  fun refresh() {
    viewModelScope.launch {
      _isRefreshing.value = true
      load()
      _isRefreshing.value = false
    }
  }

  fun toggleInvite(invite: ChildInvite) {
    viewModelScope.launch {
      when (val result = sharingRepository.toggleInvite(childId, invite.id)) {
        is ApiResult.Success -> refresh()
        is ApiResult.Error -> _errorMessage.emit(result.error.getUserMessage(context))
        is ApiResult.Loading -> Unit
      }
    }
  }

  fun deleteInvite(invite: ChildInvite) {
    viewModelScope.launch {
      when (val result = sharingRepository.deleteInvite(childId, invite.id)) {
        is ApiResult.Success -> refresh()
        is ApiResult.Error -> _errorMessage.emit(result.error.getUserMessage(context))
        is ApiResult.Loading -> Unit
      }
    }
  }

  private suspend fun load() {
    _uiState.value = SharingUiState.Loading
    val invitesResult = sharingRepository.listInvites(childId)
    val sharesResult = sharingRepository.listShares(childId)
    when {
      invitesResult is ApiResult.Error ->
          _uiState.value = SharingUiState.Error(invitesResult.error.getUserMessage(context))
      sharesResult is ApiResult.Error ->
          _uiState.value = SharingUiState.Error(sharesResult.error.getUserMessage(context))
      invitesResult is ApiResult.Success && sharesResult is ApiResult.Success -> {
        val items = buildList {
          add(
              SharingListItem.InviteLinkHeader(
                  context.getString(R.string.sharing_section_invite_links)))
          invitesResult.data.forEach { add(SharingListItem.InviteRow(it)) }
          add(SharingListItem.PeopleHeader(context.getString(R.string.sharing_section_people)))
          sharesResult.data.forEach { add(SharingListItem.ShareRow(it)) }
        }
        _uiState.value = SharingUiState.Ready(items)
      }
      else -> Unit
    }
  }
}
