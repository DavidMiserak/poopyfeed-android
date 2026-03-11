package net.poopyfeed.pf.data.repository

import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** Unit tests for AnalyticsRepository. */
class AnalyticsRepositoryTest {

  private lateinit var mockApiService: PoopyFeedApiService
  private lateinit var repository: AnalyticsRepository

  @Before
  fun setup() {
    mockApiService = mockk()
    repository = AnalyticsRepository(mockApiService)
  }

  @Test
  fun `getPatternAlerts success returns ApiResult Success with data`() = runTest {
    val mockAlerts =
        TestFixtures.mockPatternAlertsResponse(
            feedingAlert = true, feedingMessage = "Baby usually feeds every 3h — it's been 3h 30m")
    coEvery { mockApiService.getPatternAlerts(1) } returns mockAlerts

    val result = repository.getPatternAlerts(1)

    assertTrue(result is ApiResult.Success)
    assertEquals(mockAlerts, result.data)
    assertTrue(result.data.feeding.alert)
    assertEquals("Baby usually feeds every 3h — it's been 3h 30m", result.data.feeding.message)
  }

  @Test
  fun `getPatternAlerts HTTP error returns ApiResult Error`() = runTest {
    val httpException = HttpException(Response.error<Unit>(404, mockk(relaxed = true)))
    coEvery { mockApiService.getPatternAlerts(1) } throws httpException

    val result = repository.getPatternAlerts(1)

    assertTrue(result is ApiResult.Error)
    assertTrue(result.error is ApiError.HttpError)
  }

  @Test
  fun `getPatternAlerts network error returns ApiResult Error`() = runTest {
    coEvery { mockApiService.getPatternAlerts(1) } throws java.io.IOException("Network error")

    val result = repository.getPatternAlerts(1)

    assertTrue(result is ApiResult.Error)
    assertTrue(result.error is ApiError.NetworkError)
  }

  @Test
  fun `getPatternAlerts unknown error returns ApiResult Error`() = runTest {
    coEvery { mockApiService.getPatternAlerts(1) } throws RuntimeException("Unexpected error")

    val result = repository.getPatternAlerts(1)

    assertTrue(result is ApiResult.Error)
    assertTrue(result.error is ApiError.UnknownError)
  }

  @Test
  fun `getTimeline success returns ApiResult Success with paginated events`() = runTest {
    val mockEvents =
        listOf(
            TestFixtures.mockTimelineEvent(at = "2026-03-09T14:00:00Z"),
            TestFixtures.mockTimelineEvent(at = "2026-03-09T10:00:00Z"),
        )
    val mockResponse = TestFixtures.mockPaginatedResponse(results = mockEvents)
    coEvery { mockApiService.getTimeline(1, 1, 100) } returns mockResponse

    val result = repository.getTimeline(1)

    assertTrue(result is ApiResult.Success)
    assertEquals(mockResponse, result.data)
    assertEquals(2, result.data.results.size)
  }

  @Test
  fun `getTimeline HTTP error returns ApiResult Error`() = runTest {
    val httpException = HttpException(Response.error<Unit>(500, mockk(relaxed = true)))
    coEvery { mockApiService.getTimeline(1, 1, 100) } throws httpException

    val result = repository.getTimeline(1)

    assertTrue(result is ApiResult.Error)
    assertTrue(result.error is ApiError.HttpError)
  }

  @Test
  fun `getTimeline network error returns ApiResult Error`() = runTest {
    coEvery { mockApiService.getTimeline(1, 1, 100) } throws java.io.IOException("Network error")

    val result = repository.getTimeline(1)

    assertTrue(result is ApiResult.Error)
    assertTrue(result.error is ApiError.NetworkError)
  }

  @Test
  fun `exportCsv success returns ApiResult Success with ResponseBody`() = runTest {
    val mockBody = mockk<okhttp3.ResponseBody>(relaxed = true)
    coEvery { mockApiService.exportCsv(1, 30) } returns mockBody

    val result = repository.exportCsv(1, 30)

    assertTrue(result is ApiResult.Success)
    assertEquals(mockBody, result.data)
  }

  @Test
  fun `exportCsv network error returns ApiResult Error`() = runTest {
    coEvery { mockApiService.exportCsv(1, 30) } throws java.io.IOException("Network error")

    val result = repository.exportCsv(1, 30)

    assertTrue(result is ApiResult.Error)
    assertTrue(result.error is ApiError.NetworkError)
  }

  @Test
  fun `exportPdf success returns ApiResult Success with ExportJobResponse`() = runTest {
    val mockResponse = TestFixtures.mockExportJobResponse()
    coEvery { mockApiService.exportPdf(1, any()) } returns mockResponse

    val result = repository.exportPdf(1, 30)

    assertTrue(result is ApiResult.Success)
    assertEquals(mockResponse, result.data)
  }

  @Test
  fun `getExportStatus success returns ApiResult Success`() = runTest {
    val mockStatus = TestFixtures.mockJobStatusResponse(status = "completed", progress = 100)
    coEvery { mockApiService.getExportStatus(1, "task-123") } returns mockStatus

    val result = repository.getExportStatus(1, "task-123")

    assertTrue(result is ApiResult.Success)
    assertEquals("completed", result.data.status)
    assertEquals(100, result.data.progress)
  }

  @Test
  fun `downloadPdf success returns ApiResult Success with ResponseBody`() = runTest {
    val mockBody = mockk<okhttp3.ResponseBody>(relaxed = true)
    coEvery { mockApiService.downloadPdf("report.pdf") } returns mockBody

    val result = repository.downloadPdf("report.pdf")

    assertTrue(result is ApiResult.Success)
  }

  @Test
  fun `getWeeklySummary success returns ApiResult Success`() = runTest {
    val mockSummary = TestFixtures.mockWeeklySummary()
    coEvery { mockApiService.getWeeklySummary(1) } returns mockSummary

    val result = repository.getWeeklySummary(1)

    assertTrue(result is ApiResult.Success)
    assertEquals(mockSummary, result.data)
  }

  @Test
  fun `getWeeklySummary network error returns ApiResult Error`() = runTest {
    coEvery { mockApiService.getWeeklySummary(1) } throws java.io.IOException("fail")

    val result = repository.getWeeklySummary(1)

    assertTrue(result is ApiResult.Error)
    assertTrue(result.error is ApiError.NetworkError)
  }
}
