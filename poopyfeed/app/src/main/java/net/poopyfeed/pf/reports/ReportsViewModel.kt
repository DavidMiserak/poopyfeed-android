package net.poopyfeed.pf.reports

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

  data class CsvReady(val uri: Uri) : ExportState

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
    @param:ApplicationContext private val appContext: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])

  private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
  val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

  fun exportCsv(days: Int = 30) {
    viewModelScope.launch {
      _exportState.value = ExportState.Exporting
      when (val result = analyticsRepo.exportCsv(childId, days)) {
        is ApiResult.Success -> {
          val filename = "poopyfeed_export_${System.currentTimeMillis()}.csv"
          val uri = saveToDownloads(result.data, filename, "text/csv")
          if (uri != null) {
            _exportState.value = ExportState.CsvReady(uri)
            analyticsTracker.logEvent("export_csv")
          } else {
            _exportState.value = ExportState.Error("Failed to save CSV file to storage")
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

  private suspend fun saveToDownloads(
      body: okhttp3.ResponseBody,
      filename: String,
      mimeType: String,
  ): Uri? =
      withContext(ioDispatcher) {
        try {
          val values =
              ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
              }
          val uri =
              appContext.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                  ?: throw IllegalStateException("MediaStore insert returned null")
          appContext.contentResolver.openOutputStream(uri)?.use { output ->
            body.byteStream().use { input -> input.copyTo(output) }
          } ?: throw IllegalStateException("Could not open output stream")
          uri
        } catch (e: Exception) {
          Log.e("ReportsViewModel", "CSV save error: ${e.message}", e)
          null
        }
      }
}
