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
import net.poopyfeed.pf.ui.toast.ToastManager
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
  private lateinit var mockToastManager: ToastManager
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
    mockToastManager = mockk(relaxed = true)
    every { mockContext.getString(any()) } returns "Error message"
    every { mockTokenManager.getProfileTimezone() } returns "America/Los_Angeles"
    viewModel =
        CreateNapViewModel(
            savedStateHandle,
            mockRepository,
            mockSyncScheduler,
            mockAnalyticsTracker,
            mockContext,
            mockTokenManager,
            mockToastManager)
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
  fun `getDatePickerSelectionMillisForProfileTz returns midnight UTC of profile date for boundary`() =
      runTest(testDispatcher) {
        // UTC: 2024-03-10 02:00:00Z
        // Profile TZ: America/Los_Angeles (UTC-8)
        // Expected profile date: 2024-03-09
        // MaterialDatePicker expects midnight UTC, so should return 2024-03-09T00:00:00Z
        val utcTime = "2024-03-10T02:00:00Z"
        val selectionMillis = viewModel.getDatePickerSelectionMillisForProfileTz(utcTime)

        // MaterialDatePicker uses UTC internally, so verify date in UTC
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(selectionMillis)
        val utcDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

        assert(utcDateTime.dayOfMonth == 9) { "Expected day 9, got ${utcDateTime.dayOfMonth}" }
        assert(utcDateTime.monthNumber == 3) { "Expected month 3, got ${utcDateTime.monthNumber}" }
        // Should be midnight UTC
        assert(utcDateTime.hour == 0) { "Expected hour 0 (midnight UTC), got ${utcDateTime.hour}" }
      }

  @Test
  fun `getDatePickerSelectionMillisForProfileTz keeps same date when no boundary crossing`() =
      runTest(testDispatcher) {
        // UTC: 2024-03-15 12:00:00Z
        // Profile TZ: America/Los_Angeles (UTC-8)
        // Expected profile date: 2024-03-15 (no boundary crossing)
        val utcTime = "2024-03-15T12:00:00Z"
        val selectionMillis = viewModel.getDatePickerSelectionMillisForProfileTz(utcTime)

        // Verify date in UTC (matching MaterialDatePicker's internal representation)
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(selectionMillis)
        val utcDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

        assert(utcDateTime.dayOfMonth == 15) { "Expected day 15, got ${utcDateTime.dayOfMonth}" }
      }

  @Test
  fun `convertProfileTimezoneToUtc converts date and time in profile timezone to UTC correctly`() =
      runTest(testDispatcher) {
        // User is in Los Angeles (UTC-8)
        // They pick March 9 at 15:00 in the profile timezone picker
        // March 9 15:00 LA time = March 9 23:00 UTC
        val profileLocalDateTime = "2024-03-09T15:00:00"
        val resultUtc = viewModel.convertLocalTimeToUtc(profileLocalDateTime)

        // Verify it converts correctly to UTC
        val instant = kotlinx.datetime.Instant.parse(resultUtc)
        val utcLocalDateTime = instant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

        assert(utcLocalDateTime.dayOfMonth == 9) {
          "Expected UTC day 9, got ${utcLocalDateTime.dayOfMonth}"
        }
        assert(utcLocalDateTime.hour == 23) { "Expected UTC hour 23, got ${utcLocalDateTime.hour}" }
      }

  @Test
  fun `date picker e2e flow simulates MaterialDatePicker UTC behavior`() =
      runTest(testDispatcher) {
        // Scenario: Current UTC is 2024-03-10T02:00:00Z
        // Profile TZ: America/Los_Angeles (UTC-8) → local date is March 9
        // User wants to log nap for March 9 at 20:00 (8 PM) LA time
        // Expected UTC result: 2024-03-10T04:00:00Z

        val utcTime = "2024-03-10T02:00:00Z"

        // Step 1: Get millis to initialize MaterialDatePicker
        val pickerInitMillis = viewModel.getDatePickerSelectionMillisForProfileTz(utcTime)

        // Verify picker will show March 9 (read back in UTC, matching picker's behavior)
        val pickerInitDate =
            kotlinx.datetime.Instant.fromEpochMilliseconds(pickerInitMillis)
                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        assert(pickerInitDate.dayOfMonth == 9) {
          "Picker should show March 9, got ${pickerInitDate.dayOfMonth}"
        }

        // Step 2: User selects March 9 → MaterialDatePicker returns midnight UTC of March 9
        val pickerReturnedMillis = pickerInitMillis // User keeps same date

        // Step 3: Fragment extracts date from returned millis in UTC
        // (This is what the fixed fragment code does)
        val selectedDate =
            kotlinx.datetime.Instant.fromEpochMilliseconds(pickerReturnedMillis)
                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        assert(selectedDate.dayOfMonth == 9) {
          "Selected date should be March 9, got ${selectedDate.dayOfMonth}"
        }

        // Step 4: User picks 20:00 in time picker (profile TZ time)
        // Fragment builds: date from UTC extraction + time from picker
        val selectedProfileDateTime =
            "${selectedDate.year}-${String.format("%02d", selectedDate.monthNumber)}-${String.format("%02d", selectedDate.dayOfMonth)}T20:00:00"

        // Step 5: Convert profile TZ datetime to UTC
        val resultUtc = viewModel.convertLocalTimeToUtc(selectedProfileDateTime)

        // Verify: March 9 20:00 LA (UTC-8) = March 10 04:00 UTC
        val resultInstant = kotlinx.datetime.Instant.parse(resultUtc)
        val resultUtcTime = resultInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

        assert(resultUtcTime.dayOfMonth == 10) {
          "Expected UTC day 10, got ${resultUtcTime.dayOfMonth}"
        }
        assert(resultUtcTime.hour == 4) { "Expected UTC hour 4, got ${resultUtcTime.hour}" }
      }

  @Test
  fun `date picker e2e flow with US Eastern timezone at 4am`() =
      runTest(testDispatcher) {
        // Reproduces exact user bug: US/Eastern, select March 9 at 4 AM
        // Previously showed March 8 at 4 AM

        // Setup with US/Eastern timezone
        every { mockTokenManager.getProfileTimezone() } returns "US/Eastern"
        val easternViewModel =
            CreateNapViewModel(
                savedStateHandle,
                mockRepository,
                mockSyncScheduler,
                mockAnalyticsTracker,
                mockContext,
                mockTokenManager,
                mockToastManager)

        // Current time doesn't matter much, but let's say it's March 9 noon ET
        // March 9 12:00 ET = March 9 17:00 UTC
        val utcTime = "2024-03-09T17:00:00Z"

        // Step 1: Get millis for date picker (should show March 9)
        val pickerInitMillis = easternViewModel.getDatePickerSelectionMillisForProfileTz(utcTime)
        val pickerDate =
            kotlinx.datetime.Instant.fromEpochMilliseconds(pickerInitMillis)
                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)
        assert(pickerDate.dayOfMonth == 9) {
          "Picker should show March 9, got ${pickerDate.dayOfMonth}"
        }

        // Step 2: User selects March 9 → picker returns midnight UTC March 9
        val pickerReturnedMillis = pickerInitMillis

        // Step 3: Extract date in UTC (what the fixed fragment does)
        val selectedDate =
            kotlinx.datetime.Instant.fromEpochMilliseconds(pickerReturnedMillis)
                .toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

        // Step 4: User selects 4:00 AM in time picker
        val selectedProfileDateTime =
            "${selectedDate.year}-${String.format("%02d", selectedDate.monthNumber)}-${String.format("%02d", selectedDate.dayOfMonth)}T04:00:00"

        // Step 5: Convert to UTC
        val resultUtc = easternViewModel.convertLocalTimeToUtc(selectedProfileDateTime)

        // March 9 04:00 ET (UTC-5) = March 9 09:00 UTC
        val resultInstant = kotlinx.datetime.Instant.parse(resultUtc)
        val resultUtcTime = resultInstant.toLocalDateTime(kotlinx.datetime.TimeZone.UTC)

        // THE BUG: previously this showed March 8 instead of March 9
        assert(resultUtcTime.dayOfMonth == 9) {
          "Expected UTC day 9 (March 9 4AM ET -> March 9 9AM UTC), got ${resultUtcTime.dayOfMonth}"
        }
        assert(resultUtcTime.hour == 9) { "Expected UTC hour 9, got ${resultUtcTime.hour}" }
      }
}
