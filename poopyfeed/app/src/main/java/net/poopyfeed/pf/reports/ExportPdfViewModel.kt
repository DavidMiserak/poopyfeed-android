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
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import net.poopyfeed.pf.di.IoDispatcher

/** UI state for async PDF export progress bottom sheet. */
sealed interface PdfExportUiState {
  data class Polling(val progress: Int = 0, val statusText: String = "Preparing…") :
      PdfExportUiState

  data class Completed(val filename: String, val downloadUrl: String) : PdfExportUiState

  data class Downloaded(val uri: Uri) : PdfExportUiState

  data class Failed(val message: String) : PdfExportUiState
}

@HiltViewModel
class ExportPdfViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsRepo: AnalyticsRepository,
    @ApplicationContext private val appContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

  val childId: Int = checkNotNull(savedStateHandle["childId"])
  val taskId: String = checkNotNull(savedStateHandle["taskId"])

  private val _uiState = MutableStateFlow<PdfExportUiState>(PdfExportUiState.Polling())
  val uiState: StateFlow<PdfExportUiState> = _uiState.asStateFlow()

  /** Poll export status once. Called by Fragment on a 2-second timer. */
  fun pollOnce() {
    viewModelScope.launch {
      when (val result = analyticsRepo.getExportStatus(childId, taskId)) {
        is ApiResult.Success -> {
          val status = result.data
          when (status.status) {
            "completed" -> {
              val jobResult = status.result
              if (jobResult != null) {
                _uiState.value =
                    PdfExportUiState.Completed(jobResult.filename, jobResult.downloadUrl)
              } else {
                _uiState.value = PdfExportUiState.Failed("No download URL returned")
              }
            }
            "failed" -> {
              _uiState.value = PdfExportUiState.Failed(status.error ?: "Export failed")
            }
            else -> {
              val statusText = if (status.progress > 10) "Generating report…" else "Preparing…"
              _uiState.value = PdfExportUiState.Polling(status.progress, statusText)
            }
          }
        }
        is ApiResult.Error -> {
          _uiState.value = PdfExportUiState.Failed(result.error.displayMessage)
        }
        is ApiResult.Loading -> Unit
      }
    }
  }

  /** Download the completed PDF file and save to Downloads. */
  fun downloadFile(filename: String) {
    viewModelScope.launch {
      when (val result = analyticsRepo.downloadPdf(filename)) {
        is ApiResult.Success -> {
          val uri =
              saveToDownloads(
                  result.data,
                  "poopyfeed_report_${System.currentTimeMillis()}.pdf",
                  "application/pdf")
          if (uri != null) {
            _uiState.value = PdfExportUiState.Downloaded(uri)
          } else {
            _uiState.value = PdfExportUiState.Failed("Failed to save PDF")
          }
        }
        is ApiResult.Error -> {
          _uiState.value = PdfExportUiState.Failed(result.error.displayMessage)
        }
        is ApiResult.Loading -> Unit
      }
    }
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
          Log.e("ExportPdfViewModel", "PDF save error: ${e.message}", e)
          null
        }
      }
}
