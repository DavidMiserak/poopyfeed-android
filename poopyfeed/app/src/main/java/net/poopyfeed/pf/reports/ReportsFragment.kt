package net.poopyfeed.pf.reports

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.databinding.FragmentReportsBinding
import net.poopyfeed.pf.util.logScreenView

@AndroidEntryPoint
class ReportsFragment : Fragment() {

  private var _binding: FragmentReportsBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: ReportsViewModel by viewModels()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentReportsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    logScreenView(viewModel.analyticsTracker, "Reports")

    binding.buttonExportCsv.setOnClickListener { viewModel.exportCsv(getSelectedDays()) }
    binding.buttonExportPdf.setOnClickListener { viewModel.startPdfExport(getSelectedDays()) }
    binding.cardTimeline.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_reports_to_timeline, bundle)
    }
    binding.cardPediatrician.setOnClickListener {
      val bundle = Bundle().apply { putInt("childId", viewModel.childId) }
      findNavController().navigate(R.id.action_reports_to_pediatricianSummary, bundle)
    }

    collectFlows()
  }

  private fun getSelectedDays(): Int =
      when (binding.chipGroupDays.checkedChipId) {
        R.id.chip_7_days -> 7
        R.id.chip_60_days -> 60
        R.id.chip_90_days -> 90
        else -> 30
      }

  private fun collectFlows() {
    viewLifecycleOwner.lifecycleScope.launch {
      viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch {
          viewModel.exportState.collect { state ->
            when (state) {
              is ExportState.Idle -> {
                binding.buttonExportCsv.isEnabled = true
                binding.buttonExportPdf.isEnabled = true
              }
              is ExportState.Exporting -> {
                binding.buttonExportCsv.isEnabled = false
                binding.buttonExportPdf.isEnabled = false
              }
              is ExportState.CsvReady -> {
                showCsvSuccess(state.uri)
                viewModel.clearExportState()
              }
              is ExportState.PdfStarted -> {
                val bundle =
                    Bundle().apply {
                      putInt("childId", viewModel.childId)
                      putString("taskId", state.taskId)
                      putInt("days", state.days)
                    }
                findNavController().navigate(R.id.action_reports_to_exportPdfSheet, bundle)
                viewModel.clearExportState()
              }
              is ExportState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.clearExportState()
              }
            }
          }
        }
      }
    }
  }

  private fun showCsvSuccess(uri: Uri) {
    Snackbar.make(binding.root, getString(R.string.reports_csv_saved), Snackbar.LENGTH_LONG)
        .setAction(getString(R.string.reports_share)) {
          val shareIntent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
          startActivity(Intent.createChooser(shareIntent, getString(R.string.reports_share)))
        }
        .show()
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
