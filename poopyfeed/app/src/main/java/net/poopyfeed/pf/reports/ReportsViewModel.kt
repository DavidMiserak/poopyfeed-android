package net.poopyfeed.pf.reports

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AnalyticsRepository

/** Export state for the Reports screen. */
sealed interface ExportState {
  data object Idle : ExportState

  data object Exporting : ExportState

  data class CsvReady(val body: okhttp3.ResponseBody) : ExportState

  data class PdfStarted(val taskId: String, val days: Int) : ExportState

  data class Error(val message: String) : ExportState
}

@HiltViewModel
class ReportsViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsRepo: AnalyticsRepository,
    val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
  val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

  fun exportCsv(days: Int = 30) {
    viewModelScope.launch {
      _exportState.value = ExportState.Exporting
      when (val result = analyticsRepo.exportCsv(childId, days)) {
        is ApiResult.Success -> {
          _exportState.value = ExportState.CsvReady(result.data)
          analyticsTracker.logEvent("export_csv")
        }
        is ApiResult.Error -> {
          _exportState.value = ExportState.Error(result.error.errorMessage())
        }
        else -> Unit
      }
    }
  }

  fun startPdfExport(days: Int = 30) {
    viewModelScope.launch {
      _exportState.value = ExportState.Exporting
      when (val result = analyticsRepo.exportPdf(childId, days)) {
        is ApiResult.Success -> {
          _exportState.value = ExportState.PdfStarted(result.data.taskId, days)
          analyticsTracker.logEvent("export_pdf")
        }
        is ApiResult.Error -> {
          _exportState.value = ExportState.Error(result.error.errorMessage())
        }
        else -> Unit
      }
    }
  }

  fun clearExportState() {
    _exportState.value = ExportState.Idle
  }

  private fun ApiError.errorMessage(): String =
      when (this) {
        is ApiError.HttpError -> detail ?: errorMessage
        is ApiError.NetworkError -> errorMessage
        is ApiError.SerializationError -> errorMessage
        is ApiError.UnknownError -> errorMessage
      }
}
