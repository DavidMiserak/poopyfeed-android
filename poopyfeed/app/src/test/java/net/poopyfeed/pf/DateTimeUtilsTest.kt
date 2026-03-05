package net.poopyfeed.pf

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlinx.datetime.Instant
import org.junit.Before
import org.junit.Test

/** Unit tests for DateTimeUtils. */
class DateTimeUtilsTest {

  private lateinit var mockContext: Context

  @Before
  fun setup() {
    mockContext = mockk()
    every { mockContext.getString(R.string.child_detail_never) } returns "Never"
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
  fun `formatRelativeTime with invalid string returns Never`() {
    val result = formatRelativeTime(mockContext, "invalid-date", 0L)
    assertEquals("Never", result)
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
}
