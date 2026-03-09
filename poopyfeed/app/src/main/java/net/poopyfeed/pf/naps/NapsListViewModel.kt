package net.poopyfeed.pf.naps

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import javax.inject.Inject

@HiltViewModel
class NapsListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CachedNapsRepository,
) : ViewModel() {

    private val childId: Int = checkNotNull(savedStateHandle["childId"])

    val pagingData: Flow<PagingData<Nap>> = repo.pagedNaps(childId)

    private val _deleteError: MutableStateFlow<String?> = MutableStateFlow(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    fun deleteNap(napId: Int) {
        // TODO: Implement delete with error handling
    }

    fun endNap(napId: Int) {
        // TODO: Implement end nap with error handling
    }
}
