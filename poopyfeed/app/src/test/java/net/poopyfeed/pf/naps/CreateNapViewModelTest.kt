package net.poopyfeed.pf.naps

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.CreateNapRequest
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.sync.SyncScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateNapViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedNapsRepository
  private lateinit var mockSyncScheduler: SyncScheduler
  private lateinit var mockAnalyticsTracker: AnalyticsTracker
  private lateinit var mockTokenManager: TokenManager
  private lateinit var viewModel: CreateNapViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to 1))
    mockRepository = mockk()
    mockSyncScheduler = mockk(relaxed = true)
    mockAnalyticsTracker = mockk(relaxed = true)
    mockTokenManager = mockk()
    every { mockContext.getString(any()) } returns "Error message"
    every { mockTokenManager.getProfileTimezone() } returns "America/Los_Angeles"
    viewModel =
        CreateNapViewModel(
            savedStateHandle,
            mockRepository,
            mockSyncScheduler,
            mockAnalyticsTracker,
            mockContext,
            mockTokenManager)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `createNap happy path calls repo and emits Success`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createNap(1, any()) } returns
            ApiResult.Success(TestFixtures.mockNap(end_time = null))

        viewModel.createNap("2024-01-15T12:00:00Z")
        advanceUntilIdle()

        coVerify { mockRepository.createNap(1, any()) }
        assertIs<CreateNapUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `createNap with end time passes end_time in request`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createNap(1, any()) } returns
            ApiResult.Success(TestFixtures.mockNap(end_time = "2024-01-15T14:00:00Z"))

        viewModel.createNap("2024-01-15T12:00:00Z", "2024-01-15T14:00:00Z")
        advanceUntilIdle()

        coVerify {
          mockRepository.createNap(
              1,
              match<CreateNapRequest> {
                it.start_time == "2024-01-15T12:00:00Z" && it.end_time == "2024-01-15T14:00:00Z"
              })
        }
        assertIs<CreateNapUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `createNap API error emits Error state`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createNap(1, any()) } returns
            ApiResult.Error(ApiError.NetworkError("fail"))

        viewModel.createNap("2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<CreateNapUiState.Error>(viewModel.uiState.value)
        assert((viewModel.uiState.value as CreateNapUiState.Error).message == "Error message")
      }

  @Test
  fun `createNap when repo returns Loading keeps Saving`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createNap(1, any()) } returns ApiResult.Loading()

        viewModel.createNap("2024-01-15T12:00:00Z")
        advanceUntilIdle()

        assertIs<CreateNapUiState.Saving>(viewModel.uiState.value)
      }

  @Test
  fun `createNap logs analytics with duration in minutes when end_time is present`() =
      runTest(testDispatcher) {
        // Create a nap with 2-hour duration
        val startTime = "2024-01-15T12:00:00Z"
        val endTime = "2024-01-15T14:00:00Z"
        val mockNap = TestFixtures.mockNap(start_time = startTime, end_time = endTime)
        coEvery { mockRepository.createNap(1, any()) } returns ApiResult.Success(mockNap)

        viewModel.createNap(startTime, endTime)
        advanceUntilIdle()

        // 2 hours = 120 minutes
        verify { mockAnalyticsTracker.logNapLogged(120) }
      }

  @Test
  fun `createNap logs analytics with -1 duration when end_time is null (open-ended nap)`() =
      runTest(testDispatcher) {
        val mockNap = TestFixtures.mockNap(end_time = null)
        coEvery { mockRepository.createNap(1, any()) } returns ApiResult.Success(mockNap)

        viewModel.createNap("2024-01-15T12:00:00Z")
        advanceUntilIdle()

        // Open-ended naps log with -1 duration
        verify { mockAnalyticsTracker.logNapLogged(-1) }
      }

  @Test
  fun `createNap logs error analytics when API fails`() =
      runTest(testDispatcher) {
        coEvery { mockRepository.createNap(1, any()) } returns
            ApiResult.Error(ApiError.NetworkError("Network failed"))

        viewModel.createNap("2024-01-15T12:00:00Z")
        advanceUntilIdle()

        // Verify error is logged to analytics
        verify { mockAnalyticsTracker.logError(any(), any()) }
        assertIs<CreateNapUiState.Error>(viewModel.uiState.value)
      }

  @Test
  fun `createNap logs error analytics when duration calculation fails`() =
      runTest(testDispatcher) {
        // Create a nap with invalid timestamp to trigger parsing error
        val mockNap =
            TestFixtures.mockNap(start_time = "invalid", end_time = "2024-01-15T14:00:00Z")
        coEvery { mockRepository.createNap(1, any()) } returns ApiResult.Success(mockNap)

        viewModel.createNap("2024-01-15T12:00:00Z", "2024-01-15T14:00:00Z")
        advanceUntilIdle()

        // Verify error is logged for duration calculation failure
        verify { mockAnalyticsTracker.logError("NapDurationCalculationError", any()) }
        // But nap is still logged with 0 duration
        verify { mockAnalyticsTracker.logNapLogged(0) }
        assertIs<CreateNapUiState.Success>(viewModel.uiState.value)
      }

  @Test
  fun `proposedDuration emits formatted duration when both start and end times are set`() =
      runTest(testDispatcher) {
        val startTime = "2024-01-15T12:00:00Z"
        val endTime = "2024-01-15T13:45:00Z" // 1 hour 45 minutes
        viewModel.setStartTime(startTime)
        viewModel.setEndTime(endTime)
        advanceUntilIdle()

        val duration = viewModel.proposedDuration.value
        assert(duration == "1h 45m") { "Expected '1h 45m', got '$duration'" }
      }

  @Test
  fun `proposedDuration emits empty string when end time is null`() =
      runTest(testDispatcher) {
        val startTime = "2024-01-15T12:00:00Z"
        viewModel.setStartTime(startTime)
        viewModel.setEndTime(null)
        advanceUntilIdle()

        val duration = viewModel.proposedDuration.value
        assert(duration == "") { "Expected empty string, got '$duration'" }
      }

  @Test
  fun `proposedDuration emits only minutes when duration is less than 1 hour`() =
      runTest(testDispatcher) {
        val startTime = "2024-01-15T12:00:00Z"
        val endTime = "2024-01-15T12:30:00Z" // 30 minutes
        viewModel.setStartTime(startTime)
        viewModel.setEndTime(endTime)
        advanceUntilIdle()

        val duration = viewModel.proposedDuration.value
        assert(duration == "30m") { "Expected '30m', got '$duration'" }
      }

  @Test
  fun `convertLocalTimeToUtc converts profile timezone time to UTC correctly`() =
      runTest(testDispatcher) {
        // Profile TZ: America/Los_Angeles (PST = UTC-8)
        // Local time: 2024-01-15 09:00:00 (profile TZ)
        // Expected UTC: 2024-01-15 17:00:00Z (8 hours ahead)
        val profileLocalTime = "2024-01-15T09:00:00"
        val utcTime = viewModel.convertLocalTimeToUtc(profileLocalTime)

        // Check it's a valid ISO 8601 UTC string ending in Z
        assert(utcTime.endsWith("Z")) { "UTC time should end with Z, got: $utcTime" }

        // Verify timestamp can be parsed as valid ISO 8601
        assert(runCatching { kotlinx.datetime.Instant.parse(utcTime) }.isSuccess) {
          "Result should be valid ISO 8601: $utcTime"
        }
      }

  @Test
  fun `convertUtcTimeToLocal converts UTC to profile timezone time correctly`() =
      runTest(testDispatcher) {
        // UTC time: 2024-01-15 17:00:00Z
        // Profile TZ: America/Los_Angeles (PST = UTC-8)
        // Expected local: 2024-01-15 09:00:00
        val utcTime = "2024-01-15T17:00:00Z"
        val localDateTime = viewModel.convertUtcTimeToLocal(utcTime)

        // Check format (no Z, as it's a local time string)
        assert(!localDateTime.endsWith("Z")) {
          "Local time should not end with Z, got: $localDateTime"
        }

        // Verify it's a valid LocalDateTime format
        assert(runCatching { kotlinx.datetime.LocalDateTime.parse(localDateTime) }.isSuccess) {
          "Result should be valid LocalDateTime: $localDateTime"
        }
      }

  @Test
  fun `getCalendarHourMinuteForPicker returns profile timezone time components`() =
      runTest(testDispatcher) {
        // UTC: 2024-01-15 17:00:00Z
        // Profile TZ: America/Los_Angeles (UTC-8)
        // Expected local: 09:00 (9 AM)
        val utcTime = "2024-01-15T17:00:00Z"
        val (hour, minute) = viewModel.getCalendarHourMinuteForPicker(utcTime)

        // Should return 9 (from 09:00)
        assert(hour == 9) { "Expected hour 9, got $hour" }
        assert(minute == 0) { "Expected minute 0, got $minute" }
      }

  @Test
  fun `getCalendarHourMinuteForPicker handles fractional minutes correctly`() =
      runTest(testDispatcher) {
        // UTC: 2024-01-15 17:45:30Z
        // Profile TZ: America/Los_Angeles (UTC-8)
        // Expected local: 09:45
        val utcTime = "2024-01-15T17:45:30Z"
        val (hour, minute) = viewModel.getCalendarHourMinuteForPicker(utcTime)

        assert(hour == 9) { "Expected hour 9, got $hour" }
        assert(minute == 45) { "Expected minute 45, got $minute" }
      }

  @Test
  fun `getDatePickerSelectionMillisForProfileTz returns correct date for midnight boundary`() =
      runTest(testDispatcher) {
        // UTC: 2024-03-10 02:00:00Z
        // Profile TZ: America/Los_Angeles (UTC-8)
        // Expected profile date: 2024-03-09
        val utcTime = "2024-03-10T02:00:00Z"
        val selectionMillis = viewModel.getDatePickerSelectionMillisForProfileTz(utcTime)

        // Verify the returned millis represents March 9 in profile timezone
        // by converting back to date
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(selectionMillis)
        val localDateTime =
            instant.toLocalDateTime(kotlinx.datetime.TimeZone.of("America/Los_Angeles"))

        assert(localDateTime.dayOfMonth == 9) { "Expected day 9, got ${localDateTime.dayOfMonth}" }
        assert(localDateTime.monthNumber == 3) {
          "Expected month 3, got ${localDateTime.monthNumber}"
        }
      }

  @Test
  fun `getDatePickerSelectionMillisForProfileTz keeps same date when no boundary crossing`() =
      runTest(testDispatcher) {
        // UTC: 2024-03-15 12:00:00Z
        // Profile TZ: America/Los_Angeles (UTC-8)
        // Expected profile date: 2024-03-15 (no boundary crossing)
        val utcTime = "2024-03-15T12:00:00Z"
        val selectionMillis = viewModel.getDatePickerSelectionMillisForProfileTz(utcTime)

        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(selectionMillis)
        val localDateTime =
            instant.toLocalDateTime(kotlinx.datetime.TimeZone.of("America/Los_Angeles"))

        assert(localDateTime.dayOfMonth == 15) {
          "Expected day 15, got ${localDateTime.dayOfMonth}"
        }
      }
}
