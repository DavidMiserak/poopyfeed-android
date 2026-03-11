package net.poopyfeed.pf.reports

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentExportPdfBottomSheetBinding

@AndroidEntryPoint
class ExportPdfBottomSheetFragment : BottomSheetDialogFragment() {

  private var _binding: FragmentExportPdfBottomSheetBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: ExportPdfViewModel by viewModels()
  private var pollingJob: Job? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentExportPdfBottomSheetBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.buttonCancel.setOnClickListener { dismiss() }
    binding.buttonRetry.setOnClickListener {
      binding.textError.visibility = View.GONE
      binding.buttonRetry.visibility = View.GONE
      binding.progressBar.visibility = View.VISIBLE
      startPolling()
    }

    collectFlows()
    startPolling()
  }

  private fun startPolling() {
    pollingJob?.cancel()
    pollingJob =
        viewLifecycleOwner.lifecycleScope.launch {
          viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
              viewModel.pollOnce()
              delay(2000L)
            }
          }
        }
  }

  private fun stopPolling() {
    pollingJob?.cancel()
    pollingJob = null
  }

  private fun collectFlows() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.uiState.collect { state ->
            when (state) {
              is PdfExportUiState.Polling -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.progressBar.setProgressCompat(state.progress, true)
                binding.textStatus.text = state.statusText
                binding.textError.visibility = View.GONE
                binding.buttonRetry.visibility = View.GONE
                binding.buttonShare.visibility = View.GONE
              }
              is PdfExportUiState.Completed -> {
                stopPolling()
                binding.textStatus.text = getString(R.string.export_pdf_ready)
                binding.progressBar.setProgressCompat(100, true)
                binding.buttonShare.visibility = View.VISIBLE
                binding.buttonShare.setOnClickListener { viewModel.downloadFile(state.filename) }
              }
              is PdfExportUiState.Downloaded -> {
                sharePdf(state.file)
                dismiss()
              }
              is PdfExportUiState.Failed -> {
                stopPolling()
                binding.progressBar.visibility = View.GONE
                binding.textStatus.text = getString(R.string.export_pdf_failed)
                binding.textError.visibility = View.VISIBLE
                binding.textError.text = state.message
                binding.buttonRetry.visibility = View.VISIBLE
                binding.buttonShare.visibility = View.GONE
              }
            }
          }
        }
      }
    }
  }

  private fun sharePdf(file: File) {
    try {
      val uri =
          FileProvider.getUriForFile(
              requireContext(), "${requireContext().packageName}.fileprovider", file)
      val shareIntent =
          Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
      startActivity(Intent.createChooser(shareIntent, getString(R.string.reports_share)))
    } catch (_: Exception) {
      // File share failed — dismissed already
    }
  }

  override fun onDestroyView() {
    stopPolling()
    super.onDestroyView()
    _binding = null
  }
}
