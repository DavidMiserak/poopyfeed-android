package net.poopyfeed.pf.naps

import androidx.lifecycle.SavedStateHandle
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import net.poopyfeed.pf.analytics.AnalyticsTracker
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.sync.SyncScheduler
import net.poopyfeed.pf.ui.toast.ToastManager
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests for date picker timezone handling in CreateNapBottomSheetFragment. Validates that profile
 * timezone is used consistently for date and time selection, and that MaterialDatePicker's
 * UTC-based date representation is handled correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateNapBottomSheetFragmentTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var mockContext: android.content.Context
  private lateinit var savedStateHandle: SavedStateHandle
  private lateinit var mockRepository: CachedNapsRepository
  private lateinit var mockSyncScheduler: SyncScheduler
  private lateinit var mockAnalyticsTracker: AnalyticsTracker
  private lateinit var mockTokenManager: TokenManager
  private lateinit var mockToastManager: ToastManager
  private lateinit var viewModel: CreateNapViewModel
  private val childId = 1

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk()
    savedStateHandle = SavedStateHandle(mapOf("childId" to childId))
    mockRepository = mockk()
    mockSyncScheduler = mockk(relaxed = true)
    mockAnalyticsTracker = mockk(relaxed = true)
    mockTokenManager = mockk()
    mockToastManager = mockk(relaxed = true)

    every { mockContext.getString(any()) } returns "Error message"
    every { mockTokenManager.getProfileTimezone() } returns "US/Eastern"
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
  fun `date picker shows profile timezone date in UTC millis for MaterialDatePicker`() =
      runTest(testDispatcher) {
        // Current UTC: 2024-03-10T02:00:00Z
        // US/Eastern (UTC-5): 2024-03-09T21:00:00
        // Date picker should show March 9
        val utcTime = "2024-03-10T02:00:00Z"

        val pickerMillis = viewModel.getDatePickerSelectionMillisForProfileTz(utcTime)

        // MaterialDatePicker uses UTC internally, so verify date in UTC
        val pickerDate =
            kotlin.time.Instant.fromEpochMilliseconds(pickerMillis).toLocalDateTime(TimeZone.UTC)

        assert(pickerDate.day == 9) { "Picker should show March 9, got ${pickerDate.day}" }
        assert(pickerDate.hour == 0) { "Should be midnight UTC, got ${pickerDate.hour}" }
      }

  @Test
  fun `selecting March 9 at 4am Eastern produces correct UTC time`() =
      runTest(testDispatcher) {
        // Reproduces exact user bug report:
        // User selects March 9 at 4:00 AM in US/Eastern timezone
        // Previously showed March 8 at 4:00 AM (off by one day)

        // Step 1: Date picker returns midnight UTC of March 9
        // (MaterialDatePicker always returns midnight UTC of selected date)
        val pickerReturnedMillis =
            kotlin.time.Instant.parse("2024-03-09T00:00:00Z").toEpochMilliseconds()

        // Step 2: Fragment extracts date from millis using UTC
        // (This is the fix - previously used profile TZ which shifted date back)
        val selectedDate =
            kotlin.time.Instant.fromEpochMilliseconds(pickerReturnedMillis)
                .toLocalDateTime(TimeZone.UTC)

        assert(selectedDate.day == 9) { "Selected date should be March 9, got ${selectedDate.day}" }

        // Step 3: Combine date (from UTC extraction) + time (from profile TZ picker)
        val selectedProfileDateTime =
            "${String.format("%04d-%02d-%02d", selectedDate.year, selectedDate.month.number,
                selectedDate.day
            )}T04:00:00"

        assert(selectedProfileDateTime == "2024-03-09T04:00:00") {
          "Should be 2024-03-09T04:00:00, got $selectedProfileDateTime"
        }

        // Step 4: Convert profile TZ datetime to UTC
        val resultUtc = viewModel.convertLocalTimeToUtc(selectedProfileDateTime)

        // March 9 04:00 ET (UTC-5) = March 9 09:00 UTC
        val resultInstant = kotlin.time.Instant.parse(resultUtc)
        val resultUtcTime = resultInstant.toLocalDateTime(TimeZone.UTC)

        assert(resultUtcTime.day == 9) {
          "BUG CHECK: Expected March 9, got March ${resultUtcTime.day} (was showing yesterday)"
        }
        assert(resultUtcTime.hour == 9) { "Expected 09:00 UTC, got ${resultUtcTime.hour}" }
      }

  @Test
  fun `midnight boundary does not shift date when extracting from picker millis`() =
      runTest(testDispatcher) {
        // The core bug: MaterialDatePicker returns midnight UTC.
        // Converting midnight UTC to a behind-UTC timezone shifts the date back.
        // Fix: extract date in UTC, not profile TZ.

        val pickerReturnedMillis =
            kotlin.time.Instant.parse("2024-03-09T00:00:00Z").toEpochMilliseconds()

        // WRONG (old behavior): extract in profile TZ
        val wrongDate =
            kotlin.time.Instant.fromEpochMilliseconds(pickerReturnedMillis)
                .toLocalDateTime(TimeZone.of("US/Eastern"))
        // March 9 00:00 UTC in Eastern = March 8 19:00 → day = 8 (WRONG!)
        assert(wrongDate.day == 8) {
          "Sanity check: profile TZ extraction gives March ${wrongDate.day} (should be 8)"
        }

        // CORRECT (new behavior): extract in UTC
        val correctDate =
            kotlin.time.Instant.fromEpochMilliseconds(pickerReturnedMillis)
                .toLocalDateTime(TimeZone.UTC)
        assert(correctDate.day == 9) {
          "UTC extraction should give March 9, got ${correctDate.day}"
        }
      }

  @Test
  fun `full e2e flow with US Eastern at evening time`() =
      runTest(testDispatcher) {
        // Current time: March 9 at 8 PM ET (March 10 01:00 UTC)
        val utcTime = "2024-03-10T01:00:00Z"

        // Step 1: Get initial date picker millis
        val pickerInitMillis = viewModel.getDatePickerSelectionMillisForProfileTz(utcTime)
        val pickerInitDate =
            kotlin.time.Instant.fromEpochMilliseconds(pickerInitMillis)
                .toLocalDateTime(TimeZone.UTC)
        assert(pickerInitDate.day == 9) {
          "Picker should initialize to March 9, got ${pickerInitDate.day}"
        }

        // Step 2: User keeps March 9, picker returns midnight UTC March 9
        val pickerReturnedMillis = pickerInitMillis

        // Step 3: Extract date in UTC
        val selectedDate =
            kotlin.time.Instant.fromEpochMilliseconds(pickerReturnedMillis)
                .toLocalDateTime(TimeZone.UTC)

        // Step 4: User picks 21:00 (9 PM) in time picker
        val selectedProfileDateTime =
            "${selectedDate.year}-${String.format("%02d", selectedDate.month.number)}-${String.format("%02d",
                selectedDate.day
            )}T21:00:00"

        // Step 5: Convert to UTC
        val resultUtc = viewModel.convertLocalTimeToUtc(selectedProfileDateTime)

        // March 9 21:00 ET (UTC-5) = March 10 02:00 UTC
        val resultInstant = kotlin.time.Instant.parse(resultUtc)
        val resultUtcTime = resultInstant.toLocalDateTime(TimeZone.UTC)

        assert(resultUtcTime.day == 10) { "Expected March 10, got ${resultUtcTime.day}" }
        assert(resultUtcTime.hour == 2) { "Expected 02:00 UTC, got ${resultUtcTime.hour}" }
      }
}
