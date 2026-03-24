package net.poopyfeed.pf.reports

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.db.FeedingTrendDayEntity
import net.poopyfeed.pf.data.db.SleepSummaryDayEntity
import net.poopyfeed.pf.data.models.WeeklySummaryData
import net.poopyfeed.pf.databinding.FragmentReportsBinding
import net.poopyfeed.pf.tour.TourManager
import net.poopyfeed.pf.tour.TourStep
import net.poopyfeed.pf.util.logScreenView

@AndroidEntryPoint
class ReportsFragment : Fragment() {

  @Inject lateinit var tourManager: TourManager

  private var _binding: FragmentReportsBinding? = null
  private val binding
    get() = _binding!!

  private val viewModel: ReportsViewModel by viewModels()
  private val chartsViewModel: ChartsViewModel by viewModels()

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

    setupChartTitles()
    setupClickListeners()
    collectFlows()

    chartsViewModel.loadCharts(getSelectedDays())

    if (tourManager.shouldShowPart(3)) {
      binding.root.postDelayed(
          {
            if (isAdded) showTourPart3()
          },
          TourManager.START_DELAY_MS,
      )
    }
  }

  private fun showTourPart3() {
    if (_binding == null) return
    val ctx = requireContext()
    val total = 2
    val steps =
        listOf(
            TourStep(
                binding.cardFeedingTrends.root,
                TourManager.buildTarget(
                    ctx,
                    binding.cardFeedingTrends.root,
                    getString(R.string.tour_p3_charts_title),
                    getString(R.string.tour_p3_charts_desc),
                    1,
                    total)),
            TourStep(
                binding.cardTimeline,
                TourManager.buildTarget(
                    ctx,
                    binding.cardTimeline,
                    getString(R.string.tour_p3_timeline_title),
                    getString(R.string.tour_p3_timeline_desc),
                    2,
                    total)),
        )
    tourManager.showSequence(requireActivity(), 3, steps)
  }

  private fun setupChartTitles() {
    binding.cardFeedingTrends.textChartTitle.text = getString(R.string.reports_feeding_trends_title)
    binding.cardSleepSummary.textChartTitle.text = getString(R.string.reports_sleep_summary_title)
  }

  private fun setupClickListeners() {
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

    binding.chipGroupDays.setOnCheckedStateChangeListener { _, _ ->
      chartsViewModel.loadCharts(getSelectedDays())
    }

    binding.cardFeedingTrends.buttonRetry.setOnClickListener {
      chartsViewModel.loadCharts(getSelectedDays())
    }
    binding.cardSleepSummary.buttonRetry.setOnClickListener {
      chartsViewModel.loadCharts(getSelectedDays())
    }
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
        launch {
          chartsViewModel.feedingTrendsState.collect { state -> updateFeedingTrendsCard(state) }
        }
        launch {
          chartsViewModel.sleepSummaryState.collect { state -> updateSleepSummaryCard(state) }
        }
      }
    }
  }

  private fun updateFeedingTrendsCard(state: ChartUiState<List<FeedingTrendDayEntity>>) {
    val card = binding.cardFeedingTrends
    when (state) {
      is ChartUiState.Loading -> {
        card.skeletonLoading.visibility = View.VISIBLE
        card.chartLine.visibility = View.GONE
        card.layoutError.visibility = View.GONE
        card.textNoData.visibility = View.GONE
        binding.textFeedingChartSummary.visibility = View.GONE
      }
      is ChartUiState.Ready -> {
        card.skeletonLoading.visibility = View.GONE
        card.layoutError.visibility = View.GONE
        if (state.data.isEmpty()) {
          card.chartLine.visibility = View.GONE
          card.textNoData.visibility = View.VISIBLE
          binding.textFeedingChartSummary.visibility = View.GONE
        } else {
          card.textNoData.visibility = View.GONE
          card.chartLine.visibility = View.VISIBLE
          renderFeedingLineChart(card.chartLine, state.data)
          val feedingSummary = state.weeklySummary
          if (feedingSummary != null) {
            binding.textFeedingChartSummary.text = formatFeedingSummary(feedingSummary)
            binding.textFeedingChartSummary.visibility = View.VISIBLE
          } else {
            binding.textFeedingChartSummary.visibility = View.GONE
          }
        }
      }
      is ChartUiState.Error -> {
        card.skeletonLoading.visibility = View.GONE
        card.chartLine.visibility = View.GONE
        card.textNoData.visibility = View.GONE
        binding.textFeedingChartSummary.visibility = View.GONE
        card.layoutError.visibility = View.VISIBLE
      }
    }
  }

  private fun updateSleepSummaryCard(state: ChartUiState<List<SleepSummaryDayEntity>>) {
    val card = binding.cardSleepSummary
    when (state) {
      is ChartUiState.Loading -> {
        card.skeletonLoading.visibility = View.VISIBLE
        card.chartBar.visibility = View.GONE
        card.layoutError.visibility = View.GONE
        card.textNoData.visibility = View.GONE
        binding.textSleepChartSummary.visibility = View.GONE
      }
      is ChartUiState.Ready -> {
        card.skeletonLoading.visibility = View.GONE
        card.layoutError.visibility = View.GONE
        if (state.data.isEmpty()) {
          card.chartBar.visibility = View.GONE
          card.textNoData.visibility = View.VISIBLE
          binding.textSleepChartSummary.visibility = View.GONE
        } else {
          card.textNoData.visibility = View.GONE
          card.chartBar.visibility = View.VISIBLE
          renderSleepBarChart(card.chartBar, state.data)
          val sleepSummary = state.weeklySummary
          if (sleepSummary != null) {
            binding.textSleepChartSummary.text = formatSleepSummary(sleepSummary, state.data)
            binding.textSleepChartSummary.visibility = View.VISIBLE
          } else {
            binding.textSleepChartSummary.visibility = View.GONE
          }
        }
      }
      is ChartUiState.Error -> {
        card.skeletonLoading.visibility = View.GONE
        card.chartBar.visibility = View.GONE
        card.textNoData.visibility = View.GONE
        binding.textSleepChartSummary.visibility = View.GONE
        card.layoutError.visibility = View.VISIBLE
      }
    }
  }

  private val brandOrange by lazy { Color.parseColor("#FF6B35") }
  private val axisTextColor by lazy {
    MaterialColors.getColor(
        requireContext(), com.google.android.material.R.attr.colorOnSurfaceVariant, Color.DKGRAY)
  }
  private val axisLineColor by lazy {
    MaterialColors.getColor(
        requireContext(), com.google.android.material.R.attr.colorOutlineVariant, Color.LTGRAY)
  }

  private fun getFredokaTypeface(): Typeface? =
      ResourcesCompat.getFont(requireContext(), R.font.fredoka)

  private fun renderFeedingLineChart(chart: LineChart, data: List<FeedingTrendDayEntity>) {
    val entries = data.mapIndexed { i, d -> Entry(i.toFloat(), d.count.toFloat()) }
    val labels = data.map { formatDateLabel(it.date) }
    val dataSet =
        LineDataSet(entries, "").apply {
          color = brandOrange
          setCircleColor(brandOrange)
          circleRadius = 3f
          lineWidth = 2f
          setDrawFilled(true)
          fillColor = brandOrange
          fillAlpha = 51
          setDrawValues(false)
          mode = LineDataSet.Mode.CUBIC_BEZIER
        }
    chart.apply {
      this.data = LineData(dataSet)
      description.isEnabled = false
      legend.isEnabled = false
      setTouchEnabled(true)
      setPinchZoom(false)
      xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 1f
        valueFormatter = IndexAxisValueFormatter(labels)
        typeface = getFredokaTypeface()
        textColor = axisTextColor
        axisLineColor = this@ReportsFragment.axisLineColor
        gridColor = this@ReportsFragment.axisLineColor
        setLabelCount(minOf(data.size, 7), true)
      }
      axisLeft.apply {
        granularity = 1f
        axisMinimum = 0f
        typeface = getFredokaTypeface()
        textColor = axisTextColor
        axisLineColor = this@ReportsFragment.axisLineColor
        gridColor = this@ReportsFragment.axisLineColor
      }
      axisRight.isEnabled = false
      animateX(300)
      invalidate()
    }
  }

  private fun renderSleepBarChart(chart: BarChart, data: List<SleepSummaryDayEntity>) {
    val entries =
        data.mapIndexed { i, d -> BarEntry(i.toFloat(), (d.total_minutes ?: 0).toFloat()) }
    val labels = data.map { formatDateLabel(it.date) }
    val dataSet =
        BarDataSet(entries, "").apply {
          color = brandOrange
          setDrawValues(false)
        }
    chart.apply {
      this.data = BarData(dataSet)
      description.isEnabled = false
      legend.isEnabled = false
      setTouchEnabled(true)
      setPinchZoom(false)
      xAxis.apply {
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 1f
        valueFormatter = IndexAxisValueFormatter(labels)
        typeface = getFredokaTypeface()
        textColor = axisTextColor
        axisLineColor = this@ReportsFragment.axisLineColor
        gridColor = this@ReportsFragment.axisLineColor
        setLabelCount(minOf(data.size, 7), true)
      }
      axisLeft.apply {
        granularity = 1f
        axisMinimum = 0f
        typeface = getFredokaTypeface()
        textColor = axisTextColor
        axisLineColor = this@ReportsFragment.axisLineColor
        gridColor = this@ReportsFragment.axisLineColor
      }
      axisRight.isEnabled = false
      animateY(300)
      invalidate()
    }
  }

  private fun formatDateLabel(isoDate: String): String =
      try {
        val parts = isoDate.split("-")
        val month =
            when (parts[1]) {
              "01" -> "Jan"
              "02" -> "Feb"
              "03" -> "Mar"
              "04" -> "Apr"
              "05" -> "May"
              "06" -> "Jun"
              "07" -> "Jul"
              "08" -> "Aug"
              "09" -> "Sep"
              "10" -> "Oct"
              "11" -> "Nov"
              "12" -> "Dec"
              else -> parts[1]
            }
        "$month ${parts[2].trimStart('0')}"
      } catch (_: Exception) {
        isoDate
      }

  private fun formatFeedingSummary(summary: WeeklySummaryData): String {
    val avg = getString(R.string.reports_avg_per_day, summary.avgPerDay)
    val trend = formatTrend(summary.trend)
    return "$avg · $trend"
  }

  private fun formatSleepSummary(
      summary: WeeklySummaryData,
      data: List<SleepSummaryDayEntity>
  ): String {
    val totalMinutes = data.sumOf { it.total_minutes ?: 0 }
    val days = data.size.coerceAtLeast(1)
    val hrsPerDay = totalMinutes / 60.0 / days
    return getString(R.string.reports_sleep_avg_format, summary.avgPerDay, hrsPerDay)
  }

  private fun formatTrend(trend: String): String =
      when (trend) {
        "increasing" -> getString(R.string.reports_trend_increasing)
        "decreasing" -> getString(R.string.reports_trend_decreasing)
        else -> getString(R.string.reports_trend_stable)
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
