package net.poopyfeed.pf.diapers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import javax.inject.Inject

@HiltViewModel
class DiapersListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedDiapersRepository,
) : ViewModel() {

    private val childId: Int = checkNotNull(savedStateHandle["childId"])

    val pagingData: Flow<PagingData<Diaper>> = repo.pagedDiapers(childId)

    private val _errorMessage: MutableStateFlow<String?> = MutableStateFlow(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
}
