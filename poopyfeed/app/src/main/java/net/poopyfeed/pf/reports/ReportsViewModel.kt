package net.poopyfeed.pf.reports

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import net.poopyfeed.pf.di.IoDispatcher

/** Export state for the Reports screen. */
sealed interface ExportState {
  data object Idle : ExportState

  data object Exporting : ExportState

  data class CsvReady(val file: File) : ExportState

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
    @ApplicationContext private val appContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
  val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

  fun exportCsv(days: Int = 30) {
    viewModelScope.launch {
      _exportState.value = ExportState.Exporting
      when (val result = analyticsRepo.exportCsv(childId, days)) {
        is ApiResult.Success -> {
          val file = saveToFile(result.data, "poopyfeed_export_${System.currentTimeMillis()}.csv")
          if (file != null) {
            _exportState.value = ExportState.CsvReady(file)
            analyticsTracker.logEvent("export_csv")
          } else {
            _exportState.value = ExportState.Error("Failed to save file")
          }
        }
        is ApiResult.Error -> {
          _exportState.value = ExportState.Error(result.error.displayMessage)
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
          _exportState.value = ExportState.Error(result.error.displayMessage)
        }
        else -> Unit
      }
    }
  }

  fun clearExportState() {
    _exportState.value = ExportState.Idle
  }

  private suspend fun saveToFile(body: okhttp3.ResponseBody, filename: String): File? =
      withContext(ioDispatcher) {
        try {
          val dir = File(appContext.cacheDir, "exports")
          if (!dir.exists()) dir.mkdirs()
          val file = File(dir, filename)
          file.outputStream().use { output ->
            body.byteStream().use { input -> input.copyTo(output) }
          }
          file
        } catch (_: Exception) {
          null
        }
      }
}
