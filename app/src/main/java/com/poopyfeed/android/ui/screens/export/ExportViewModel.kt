package com.poopyfeed.android.ui.screens.export

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.poopyfeed.android.data.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class ExportUiState(
    val format: String = "csv",
    val days: Int = 30,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pdfPolling: Boolean = false,
    val pdfProgress: String? = null,
)

@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        private val analyticsRepository: AnalyticsRepository,
        @ApplicationContext private val context: Context,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        val childId: Int = savedStateHandle.get<String>("childId")?.toIntOrNull() ?: 0

        private val _uiState = MutableStateFlow(ExportUiState())
        val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

        fun onFormatChange(format: String) {
            _uiState.update { it.copy(format = format, error = null) }
        }

        fun onDaysChange(days: Int) {
            _uiState.update { it.copy(days = days.coerceIn(1, 90), error = null) }
        }

        fun export(
            onCsvDownload: (File) -> Unit,
            onPdfDownload: (File) -> Unit,
        ) {
            viewModelScope.launch {
                if (_uiState.value.format == "csv") {
                    _uiState.update { it.copy(isLoading = true, error = null) }
                    analyticsRepository.exportCsv(childId, _uiState.value.days).fold(
                        onSuccess = { body ->
                            _uiState.update { it.copy(isLoading = false) }
                            saveCsvAndNotify(body, onCsvDownload)
                        },
                        onFailure = { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: "Export failed",
                                )
                            }
                        },
                    )
                } else {
                    _uiState.update { it.copy(isLoading = true, pdfPolling = true, error = null) }
                    analyticsRepository.exportPdf(childId, _uiState.value.days).fold(
                        onSuccess = { taskId ->
                            pollPdfStatus(taskId, onPdfDownload)
                        },
                        onFailure = { e ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    pdfPolling = false,
                                    error = e.message ?: "Export failed",
                                )
                            }
                        },
                    )
                }
            }
        }

        private suspend fun pollPdfStatus(
            taskId: String,
            onPdfDownload: (File) -> Unit,
        ) {
            while (true) {
                _uiState.update { it.copy(pdfProgress = "Checking status...") }
                analyticsRepository.getExportStatus(childId, taskId).fold(
                    onSuccess = { status ->
                        when (status.status) {
                            "SUCCESS" -> {
                                val filename = status.result ?: return@fold
                                _uiState.update { it.copy(pdfProgress = "Downloading...") }
                                analyticsRepository.downloadPdf(filename).fold(
                                    onSuccess = { body ->
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                pdfPolling = false,
                                                pdfProgress = null,
                                            )
                                        }
                                        savePdfAndNotify(body, filename, onPdfDownload)
                                    },
                                    onFailure = { e ->
                                        _uiState.update {
                                            it.copy(
                                                isLoading = false,
                                                pdfPolling = false,
                                                pdfProgress = null,
                                                error = e.message ?: "Download failed",
                                            )
                                        }
                                    },
                                )
                                return
                            }
                            "FAILURE" -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        pdfPolling = false,
                                        pdfProgress = null,
                                        error = status.error ?: "PDF generation failed",
                                    )
                                }
                                return
                            }
                            else -> {
                                _uiState.update { it.copy(pdfProgress = "Generating PDF...") }
                                delay(2000)
                            }
                        }
                    },
                    onFailure = { e ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                pdfPolling = false,
                                pdfProgress = null,
                                error = e.message ?: "Status check failed",
                            )
                        }
                        return
                    },
                )
            }
        }

        private fun saveCsvAndNotify(
            body: ResponseBody,
            onDownload: (File) -> Unit,
        ) {
            val file = File(context.cacheDir, "analytics-export.csv")
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            onDownload(file)
        }

        private fun savePdfAndNotify(
            body: ResponseBody,
            filename: String,
            onDownload: (File) -> Unit,
        ) {
            val file = File(context.cacheDir, filename)
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            onDownload(file)
        }
    }
