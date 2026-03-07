package net.poopyfeed.pf.children

import android.content.Context
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
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.SharingRepository

/**
 * ViewModel for [ChildrenListFabBottomSheetFragment]. Accepts an invite by token and emits the new
 * child ID on success for the fragment to navigate to child detail.
 */
@HiltViewModel
class ChildrenListFabViewModel
@Inject
constructor(
    private val sharingRepository: SharingRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

  private val _navigateToChildId = MutableSharedFlow<Int>(replay = 0)
  val navigateToChildId: SharedFlow<Int> = _navigateToChildId.asSharedFlow()

  private val _isSubmitting = MutableStateFlow(false)
  val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

  private val _errorMessage = MutableSharedFlow<String>(replay = 0)
  val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

  fun acceptByToken(token: String) {
    if (token.isBlank()) return
    viewModelScope.launch {
      _isSubmitting.value = true
      when (val result = sharingRepository.acceptInvite(token)) {
        is ApiResult.Success -> _navigateToChildId.emit(result.data.id)
        is ApiResult.Error -> _errorMessage.emit(result.error.getUserMessage(context))
        is ApiResult.Loading -> Unit
      }
      _isSubmitting.value = false
    }
  }
}
