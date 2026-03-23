package net.poopyfeed.pf.reports

import android.view.View
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import net.poopyfeed.pf.R
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.db.FeedingTrendDayEntity
import net.poopyfeed.pf.data.db.SleepSummaryDayEntity
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AnalyticsRepository
import net.poopyfeed.pf.data.repository.CachedChartsRepository
import net.poopyfeed.pf.idleMainLooperUntil
import net.poopyfeed.pf.launchFragmentInHiltContainer
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** UI tests for [ReportsFragment] using Hilt + Robolectric. */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ReportsFragmentTest {

  @get:Rule(order = 0) var hiltRule = HiltAndroidRule(this)

  @BindValue @JvmField val chartsRepo: CachedChartsRepository = mockk(relaxed = true)
  @BindValue @JvmField val analyticsRepo: AnalyticsRepository = mockk(relaxed = true)

  private val feedingFlow = MutableStateFlow<List<FeedingTrendDayEntity>>(emptyList())
  private val sleepFlow = MutableStateFlow<List<SleepSummaryDayEntity>>(emptyList())

  @Before
  fun setup() {
    hiltRule.inject()
    every { chartsRepo.getFeedingTrends(1, any()) } returns feedingFlow
    every { chartsRepo.getSleepSummary(1, any()) } returns sleepFlow
    coEvery { chartsRepo.refreshFeedingTrends(1, any()) } returns
        ApiResult.Success(TestFixtures.mockFeedingTrendsResponse(dailyData = emptyList()))
    coEvery { chartsRepo.refreshSleepSummary(1, any()) } returns
        ApiResult.Success(TestFixtures.mockSleepSummaryResponse(dailyData = emptyList()))
  }

  @Test
  fun `charts show no-data views when empty Ready state is emitted`() {
    var fragment: ReportsFragment? = null
    launchFragmentInHiltContainer<ReportsFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", 1) },
    ) {
      fragment = this
    }

    idleMainLooperUntil(maxIterations = 500) {
      val root = fragment?.view ?: return@idleMainLooperUntil false
      val feedingCard = root.findViewById<View>(R.id.card_feeding_trends)
      val sleepCard = root.findViewById<View>(R.id.card_sleep_summary)
      feedingCard.findViewById<View>(R.id.text_no_data).visibility == View.VISIBLE &&
          sleepCard.findViewById<View>(R.id.text_no_data).visibility == View.VISIBLE
    }

    val root = fragment!!.requireView()
    val feedingCard = root.findViewById<View>(R.id.card_feeding_trends)
    val sleepCard = root.findViewById<View>(R.id.card_sleep_summary)

    assertEquals(View.VISIBLE, feedingCard.findViewById<View>(R.id.text_no_data).visibility)
    assertEquals(View.GONE, feedingCard.findViewById<View>(R.id.skeleton_loading).visibility)
    assertEquals(View.GONE, feedingCard.findViewById<View>(R.id.chart_line).visibility)

    assertEquals(View.VISIBLE, sleepCard.findViewById<View>(R.id.text_no_data).visibility)
    assertEquals(View.GONE, sleepCard.findViewById<View>(R.id.skeleton_loading).visibility)
    assertEquals(View.GONE, sleepCard.findViewById<View>(R.id.chart_bar).visibility)
  }

  @Test
  fun `charts use theme-aware axis colors instead of hardcoded dark text`() {
    feedingFlow.value = TestFixtures.mockFeedingTrendDayEntities()
    sleepFlow.value = TestFixtures.mockSleepSummaryDayEntities()

    var fragment: ReportsFragment? = null
    launchFragmentInHiltContainer<ReportsFragment>(
        fragmentArgs = android.os.Bundle().apply { putInt("childId", 1) },
    ) {
      fragment = this
    }

    idleMainLooperUntil(maxIterations = 500) {
      val root = fragment?.view ?: return@idleMainLooperUntil false
      val feedingCard = root.findViewById<View>(R.id.card_feeding_trends)
      val sleepCard = root.findViewById<View>(R.id.card_sleep_summary)
      feedingCard.findViewById<View>(R.id.chart_line).visibility == View.VISIBLE &&
          sleepCard.findViewById<View>(R.id.chart_bar).visibility == View.VISIBLE
    }

    val root = fragment!!.requireView()
    val expectedAxisTextColor =
        MaterialColors.getColor(root, com.google.android.material.R.attr.colorOnSurfaceVariant)

    val feedingChart =
        root
            .findViewById<View>(R.id.card_feeding_trends)
            .findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chart_line)
    val sleepChart =
        root
            .findViewById<View>(R.id.card_sleep_summary)
            .findViewById<com.github.mikephil.charting.charts.BarChart>(R.id.chart_bar)

    assertEquals(expectedAxisTextColor, feedingChart.xAxis.textColor)
    assertEquals(expectedAxisTextColor, feedingChart.axisLeft.textColor)
    assertEquals(expectedAxisTextColor, sleepChart.xAxis.textColor)
    assertEquals(expectedAxisTextColor, sleepChart.axisLeft.textColor)
  }
}
