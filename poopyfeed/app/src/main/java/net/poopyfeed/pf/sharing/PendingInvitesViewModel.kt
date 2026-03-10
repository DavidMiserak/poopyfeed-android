package net.poopyfeed.pf.sharing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.ShareInvite
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.data.repository.ChildrenRepository
import net.poopyfeed.pf.data.repository.SharingRepository

/** Display model for a pending invite with resolved child name. */
data class ShareInviteWithChildName(
    val invite: ShareInvite,
    val childName: String?,
)

/** UI state for the pending invites screen. */
sealed interface PendingInvitesUiState {
  data object Loading : PendingInvitesUiState

  data class Ready(val invites: List<ShareInviteWithChildName>) : PendingInvitesUiState

  data object Empty : PendingInvitesUiState

  data class Error(val message: String) : PendingInvitesUiState
}

/**
 * ViewModel for [PendingInvitesFragment]. Loads pending share invites, resolves child names, and
 * supports accepting an invite (then navigates to child detail).
 */
@HiltViewModel
class PendingInvitesViewModel
@Inject
constructor(
    private val sharingRepository: SharingRepository,
    private val childrenRepository: ChildrenRepository,
    private val cachedChildrenRepository: CachedChildrenRepository,
    @param:ApplicationContext private val context: Context,
    val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

  private val _uiState: MutableStateFlow<PendingInvitesUiState> =
      MutableStateFlow(PendingInvitesUiState.Loading)
  val uiState: StateFlow<PendingInvitesUiState> = _uiState.asStateFlow()

  /** One-shot: navigate to child detail after accept. Fragment should navigate then pop. */
  private val _navigateToChild = MutableSharedFlow<Int>(replay = 0)
  val navigateToChild: SharedFlow<Int> = _navigateToChild.asSharedFlow()

  private val _errorMessage = MutableSharedFlow<String>(replay = 0)
  val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

  init {
    loadInvites()
  }

  fun refresh() {
    loadInvites()
  }

  private fun loadInvites() {
    viewModelScope.launch {
      _uiState.value = PendingInvitesUiState.Loading
      when (val result = sharingRepository.getPendingInvites()) {
        is ApiResult.Success -> {
          if (result.data.isEmpty()) {
            _uiState.value = PendingInvitesUiState.Empty
          } else {
            val invites = result.data
            val withNames = coroutineScope {
              invites
                  .map { invite ->
                    async {
                      val childResult =
                          childrenRepository.getChild(invite.child).first {
                            it !is ApiResult.Loading
                          }
                      val name =
                          when (childResult) {
                            is ApiResult.Success -> childResult.data.name
                            else -> null
                          }
                      ShareInviteWithChildName(invite, name)
                    }
                  }
                  .awaitAll()
            }
            _uiState.value = PendingInvitesUiState.Ready(withNames)
          }
        }
        is ApiResult.Error ->
            _uiState.value = PendingInvitesUiState.Error(result.error.getUserMessage(context))
        is ApiResult.Loading -> Unit
      }
    }
  }

  /**
   * Accept an invite by token (from invite link). On success refreshes cache and emits
   * [navigateToChild].
   */
  fun acceptByToken(token: String) {
    viewModelScope.launch {
      when (val result = sharingRepository.acceptInvite(token)) {
        is ApiResult.Success -> {
          cachedChildrenRepository.refreshChildren()
          _navigateToChild.emit(result.data.id)
        }
        is ApiResult.Error -> _errorMessage.emit(result.error.getUserMessage(context))
        is ApiResult.Loading -> Unit
      }
    }
  }
}
