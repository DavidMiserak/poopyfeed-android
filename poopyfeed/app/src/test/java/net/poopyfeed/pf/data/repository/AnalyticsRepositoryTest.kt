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
}
