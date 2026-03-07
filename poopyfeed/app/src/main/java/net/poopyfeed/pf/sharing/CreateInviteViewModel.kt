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
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateShareRequest
import net.poopyfeed.pf.data.repository.SharingRepository

/** UI state for the create invite bottom sheet. Backend creates token-based invite (role only). */
sealed interface CreateInviteUiState {
  /** Form ready; only role is required. */
  data class Ready(
      val selectedRole: String?, // "co-parent" | "caregiver"
      val roleError: String?,
  ) : CreateInviteUiState

  /** Submitting invite. */
  data object Submitting : CreateInviteUiState

  /** Invite created; show [inviteCode] (token) for user to copy/share. */
  data class InviteCreated(val inviteCode: String) : CreateInviteUiState
}

/**
 * ViewModel for [CreateInviteBottomSheetFragment]. Sends role only (backend returns token/link).
 */
@HiltViewModel
class CreateInviteViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val sharingRepository: SharingRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _uiState: MutableStateFlow<CreateInviteUiState> =
      MutableStateFlow(CreateInviteUiState.Ready(selectedRole = null, roleError = null))
  val uiState: StateFlow<CreateInviteUiState> = _uiState.asStateFlow()

  private val _errorMessage = MutableSharedFlow<String>(replay = 0)
  val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

  fun setRole(role: String) {
    val current = _uiState.value
    if (current is CreateInviteUiState.Ready) {
      _uiState.value =
          current.copy(
              selectedRole = role,
              roleError = null,
          )
    }
  }

  fun submit() {
    val current = _uiState.value
    if (current !is CreateInviteUiState.Ready) return

    val role = current.selectedRole
    val roleError =
        when {
          role == null || role.isEmpty() -> context.getString(R.string.create_invite_role_error)
          else -> null
        }
    if (roleError != null) {
      _uiState.value = current.copy(roleError = roleError)
      return
    }

    val roleValue = requireNotNull(role)

    viewModelScope.launch {
      _uiState.value = CreateInviteUiState.Submitting
      when (val result =
          sharingRepository.createShare(childId, CreateShareRequest(role = roleValue))) {
        is ApiResult.Success -> {
          _uiState.value = CreateInviteUiState.InviteCreated(inviteCode = result.data.token)
        }
        is ApiResult.Error -> {
          _uiState.value =
              CreateInviteUiState.Ready(
                  selectedRole = current.selectedRole,
                  roleError = null,
              )
          _errorMessage.emit(result.error.getUserMessage(context))
        }
        is ApiResult.Loading -> Unit
      }
    }
  }
}
