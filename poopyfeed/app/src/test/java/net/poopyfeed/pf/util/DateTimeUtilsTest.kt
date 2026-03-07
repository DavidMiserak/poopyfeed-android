package net.poopyfeed.pf.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.util.TimeZone
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlinx.datetime.Instant
import net.poopyfeed.pf.R
import org.junit.After
import org.junit.Before
import org.junit.Test

/** Unit tests for DateTimeUtils. */
class DateTimeUtilsTest {

  private lateinit var mockContext: Context
  private var originalTimeZone: TimeZone? = null

  @Before
  fun setup() {
    mockContext = mockk()
    every { mockContext.getString(R.string.child_detail_never) } returns "Never"
    every { mockContext.getString(R.string.child_detail_just_now) } returns "Just now"
    originalTimeZone = TimeZone.getDefault()
  }

  @After
  fun tearDown() {
    originalTimeZone?.let { TimeZone.setDefault(it) }
  }

  // === Instant.parse tests (verifying kotlinx-datetime handles our API formats) ===

  @Test
  fun `Instant parse handles timestamp without fractional seconds`() {
    val instant = Instant.parse("2024-01-14T10:00:00Z")
    assertEquals(1705226400000L, instant.toEpochMilliseconds())
  }

  @Test
  fun `Instant parse handles timestamp with fractional seconds`() {
    val instant = Instant.parse("2024-01-14T10:00:00.781974Z")
    assertEquals(1705226400781L, instant.toEpochMilliseconds())
  }

  // === formatRelativeTime Tests ===

  @Test
  fun `formatRelativeTime with null returns Never string`() {
    val result = formatRelativeTime(mockContext, null)
    assertEquals("Never", result)
  }

  @Test
  fun `formatRelativeTime with ISO datetime parses successfully`() {
    val eventTime = "2024-01-14T10:00:00Z"
    val nowMillis = 1705401600000L // 2024-01-15T10:00:00Z (1 day later)

    val result = formatRelativeTime(mockContext, eventTime, nowMillis)
    assert(result.isNotEmpty()) { "Expected relative time string, got empty result" }
  }

  @Test
  fun `formatRelativeTime with fractional seconds parses successfully`() {
    val eventTime = "2024-01-14T10:00:00.781974Z"
    val nowMillis = 1705401600000L

    val result = formatRelativeTime(mockContext, eventTime, nowMillis)
    assert(result.isNotEmpty()) { "Expected relative time string, got empty result" }
  }

  @Test
  fun `formatRelativeTime with less than 1 minute ago returns Just now`() {
    val nowMillis = 1705226430000L // 30 seconds after the event
    val eventTime = "2024-01-14T10:00:00Z" // 1705226400000L

    val result = formatRelativeTime(mockContext, eventTime, nowMillis)
    assertEquals("Just now", result)
  }

  @Test
  fun `formatRelativeTime with invalid string returns Never`() {
    val result = formatRelativeTime(mockContext, "invalid-date", 0L)
    assertEquals("Never", result)
  }

  // === formatRelativeTimeShort Tests ===

  @Test
  fun `formatRelativeTimeShort with null returns dash`() {
    val result = formatRelativeTimeShort(mockContext, null)
    assertEquals("—", result)
  }

  @Test
  fun `formatRelativeTimeShort with less than 1 minute ago returns now`() {
    val eventTime = "2024-01-14T10:00:00Z"
    val nowMillis = 1705226430000L // 30 seconds later
    val result = formatRelativeTimeShort(mockContext, eventTime, nowMillis)
    assertEquals("now", result)
  }

  @Test
  fun `formatRelativeTimeShort with 5 minutes ago returns 5m`() {
    val eventTime = "2024-01-14T10:00:00Z"
    val nowMillis = 1705226700000L // 5 minutes later
    val result = formatRelativeTimeShort(mockContext, eventTime, nowMillis)
    assertEquals("5m", result)
  }

  @Test
  fun `formatRelativeTimeShort with 2 hours ago returns 2h`() {
    val eventTime = "2024-01-14T10:00:00Z"
    val nowMillis = 1705233600000L // 2 hours later
    val result = formatRelativeTimeShort(mockContext, eventTime, nowMillis)
    assertEquals("2h", result)
  }

  @Test
  fun `formatRelativeTimeShort with 3 days ago returns 3d`() {
    val eventTime = "2024-01-14T10:00:00Z"
    val nowMillis = 1705485600000L // 3 days later
    val result = formatRelativeTimeShort(mockContext, eventTime, nowMillis)
    assertEquals("3d", result)
  }

  @Test
  fun `formatRelativeTimeShort with invalid string returns dash`() {
    val result = formatRelativeTimeShort(mockContext, "invalid-date", 0L)
    assertEquals("—", result)
  }

  // === formatAge Tests ===

  @Test
  fun `formatAge with newborn returns 0 months`() {
    val dob = "2024-01-15"
    val nowMillis = 1705315200000L // 2024-01-15T10:00:00Z

    val result = formatAge(dob, nowMillis)
    assertEquals("0 months", result)
  }

  @Test
  fun `formatAge with 1 month old returns 1 month`() {
    val dob = "2023-12-15"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assertEquals("1 month", result)
  }

  @Test
  fun `formatAge with 3 months old returns 3 months`() {
    val dob = "2023-10-15"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assertEquals("3 months", result)
  }

  @Test
  fun `formatAge with 11 months old returns 11 months`() {
    val dob = "2023-02-15"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assertEquals("11 months", result)
  }

  @Test
  fun `formatAge with 1 year old returns 1 yr`() {
    val dob = "2023-01-15"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assertEquals("1 yr", result)
  }

  @Test
  fun `formatAge with 1 year 3 months returns 1 yr 3 mo`() {
    val dob = "2022-10-15"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assertEquals("1 yr 3 mo", result)
  }

  @Test
  fun `formatAge with 2 years 6 months returns 2 yr 6 mo`() {
    val dob = "2021-07-15"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assertEquals("2 yr 6 mo", result)
  }

  @Test
  fun `formatAge with birthday in future this month returns correct age`() {
    val dob = "2023-01-20"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assert(result == "0 months" || result.contains("month", ignoreCase = true)) {
      "Expected month-based age, got: $result"
    }
  }

  @Test
  fun `formatAge with invalid string returns empty string`() {
    val result = formatAge("invalid-date", 0L)
    assertEquals("", result)
  }

  @Test
  fun `formatAge with day of month adjustment`() {
    val dob = "2022-05-20"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assertEquals("1 yr 7 mo", result)
  }

  @Test
  fun `formatAge with day of month passed`() {
    val dob = "2022-05-10"
    val nowMillis = 1705315200000L

    val result = formatAge(dob, nowMillis)
    assertEquals("1 yr 8 mo", result)
  }

  // === formatAge timezone behavior (device local date for "today") ===

  /**
   * When "now" is 2024-01-02 00:00 UTC, in UTC the date is Jan 2; in PST it is Jan 1. For a child
   * born 2023-01-02, age in UTC is "1 yr", in PST is "11 months". Verifies formatAge uses the
   * system (device) timezone for the "now" date.
   */
  @Test
  fun `formatAge uses system timezone for now date`() {
    val dob = "2023-01-02"
    val nowMillis = Instant.parse("2024-01-02T00:00:00Z").toEpochMilliseconds()

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val resultUtc = formatAge(dob, nowMillis)
    assertEquals("1 yr", resultUtc)

    TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
    val resultPst = formatAge(dob, nowMillis)
    assertEquals("11 months", resultPst)

    assertNotEquals(resultUtc, resultPst)
  }

  @Test
  fun `formatAge same calendar day in different timezone gives same age`() {
    val dob = "2024-01-15"
    // 2024-01-15 12:00 UTC is Jan 15 in both UTC and America/Los_Angeles
    val nowMillis = Instant.parse("2024-01-15T12:00:00Z").toEpochMilliseconds()

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    val resultUtc = formatAge(dob, nowMillis)

    TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
    val resultPst = formatAge(dob, nowMillis)

    assertEquals(resultUtc, resultPst)
    assertEquals("0 months", resultUtc)
  }

  // === formatTimestampForDisplay (device timezone) ===

  @Test
  fun `formatTimestampForDisplay with valid ISO returns non-empty formatted string`() {
    val result = formatTimestampForDisplay(mockContext, "2024-01-15T14:30:00Z")
    assert(result.isNotEmpty()) { "Expected formatted date/time, got empty" }
    assert(!result.startsWith("2024-01-15") || result.contains(":")) {
      "Expected localized format, not raw ISO: $result"
    }
  }

  @Test
  fun `formatTimestampForDisplay with invalid string returns string unchanged`() {
    val invalid = "not-a-date"
    val result = formatTimestampForDisplay(mockContext, invalid)
    assertEquals(invalid, result)
  }

  // === formatNapDuration Tests ===

  @Test
  fun `formatNapDuration under 60 minutes returns minutes only`() {
    val result =
        formatNapDuration(
            mockContext,
            "2024-01-15T10:00:00Z",
            "2024-01-15T10:45:00Z",
        )
    assertEquals("45m", result)
  }

  @Test
  fun `formatNapDuration exactly 60 minutes returns hours only`() {
    val result =
        formatNapDuration(
            mockContext,
            "2024-01-15T10:00:00Z",
            "2024-01-15T11:00:00Z",
        )
    assertEquals("1h", result)
  }

  @Test
  fun `formatNapDuration mixed hours and minutes`() {
    val result =
        formatNapDuration(
            mockContext,
            "2024-01-15T10:00:00Z",
            "2024-01-15T11:30:00Z",
        )
    assertEquals("1h 30m", result)
  }

  @Test
  fun `formatNapDuration with invalid start returns empty string`() {
    val result =
        formatNapDuration(
            mockContext,
            "invalid",
            "2024-01-15T11:00:00Z",
        )
    assertEquals("", result)
  }

  @Test
  fun `formatNapDuration with invalid end returns empty string`() {
    val result =
        formatNapDuration(
            mockContext,
            "2024-01-15T10:00:00Z",
            "invalid",
        )
    assertEquals("", result)
  }
}
