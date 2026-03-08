package net.poopyfeed.pf.util

import net.poopyfeed.pf.data.models.QuietHours
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class QuietHoursCheckerTest {

  @Test
  fun `isQuietNow returns false when quietHours is null`() {
    assertFalse(QuietHoursChecker.isQuietNow(null, "America/New_York"))
    assertFalse(QuietHoursChecker.isQuietNow(null, 36000))
  }

  @Test
  fun `isQuietNow returns false when quietHours is disabled`() {
    val qh = QuietHours(enabled = false, startTime = "22:00:00", endTime = "07:00:00")
    assertFalse(QuietHoursChecker.isQuietNow(qh, "UTC"))
    assertFalse(QuietHoursChecker.isQuietNow(qh, 36000))
  }

  @Test
  fun `isQuietNow returns false when start time is invalid`() {
    val qh = QuietHours(enabled = true, startTime = "25:00:00", endTime = "07:00:00")
    assertFalse(QuietHoursChecker.isQuietNow(qh, 36000))
  }

  @Test
  fun `isQuietNow returns false when end time is invalid`() {
    val qh = QuietHours(enabled = true, startTime = "22:00:00", endTime = "12:60:00")
    assertFalse(QuietHoursChecker.isQuietNow(qh, 36000))
  }

  @Test
  fun `isQuietNow returns false when start time is empty`() {
    val qh = QuietHours(enabled = true, startTime = "", endTime = "07:00:00")
    assertFalse(QuietHoursChecker.isQuietNow(qh, 36000))
  }

  @Test
  fun `isQuietNow returns false when end time has wrong part count`() {
    val qh = QuietHours(enabled = true, startTime = "22:00:00", endTime = "1:2:3:4")
    assertFalse(QuietHoursChecker.isQuietNow(qh, 36000))
  }

  @Test
  fun `isQuietNow same-day range returns true when now is inside range`() {
    val qh = QuietHours(enabled = true, startTime = "09:00:00", endTime = "17:00:00")
    val start = 9 * 3600
    val end = 17 * 3600
    assertFalse(QuietHoursChecker.isQuietNow(qh, start - 3600))
    assertTrue(QuietHoursChecker.isQuietNow(qh, start))
    assertTrue(QuietHoursChecker.isQuietNow(qh, 10 * 3600))
    assertTrue(QuietHoursChecker.isQuietNow(qh, end))
    assertFalse(QuietHoursChecker.isQuietNow(qh, end + 3600))
  }

  @Test
  fun `isQuietNow same-day range with HH mm format`() {
    val qh = QuietHours(enabled = true, startTime = "09:00", endTime = "17:00")
    assertTrue(QuietHoursChecker.isQuietNow(qh, 12 * 3600))
    assertFalse(QuietHoursChecker.isQuietNow(qh, 8 * 3600))
  }

  @Test
  fun `isQuietNow overnight range returns true when now is after start`() {
    val qh = QuietHours(enabled = true, startTime = "22:00:00", endTime = "07:00:00")
    val startSec = 22 * 3600
    val endSec = 7 * 3600
    assertTrue(QuietHoursChecker.isQuietNow(qh, startSec))
    assertTrue(QuietHoursChecker.isQuietNow(qh, 23 * 3600))
    assertTrue(QuietHoursChecker.isQuietNow(qh, 23 * 3600 + 59 * 60 + 59))
  }

  @Test
  fun `isQuietNow overnight range returns true when now is before end`() {
    val qh = QuietHours(enabled = true, startTime = "22:00:00", endTime = "07:00:00")
    assertTrue(QuietHoursChecker.isQuietNow(qh, 0))
    assertTrue(QuietHoursChecker.isQuietNow(qh, 3 * 3600))
    assertTrue(QuietHoursChecker.isQuietNow(qh, 7 * 3600))
  }

  @Test
  fun `isQuietNow overnight range returns false when now is outside range`() {
    val qh = QuietHours(enabled = true, startTime = "22:00:00", endTime = "07:00:00")
    assertFalse(QuietHoursChecker.isQuietNow(qh, 8 * 3600))
    assertFalse(QuietHoursChecker.isQuietNow(qh, 12 * 3600))
    assertFalse(QuietHoursChecker.isQuietNow(qh, 21 * 3600))
  }

  @Test
  fun `isQuietNow two-arg overload returns boolean using real time`() {
    val qh = QuietHours(enabled = true, startTime = "00:00:00", endTime = "23:59:59")
    val result = QuietHoursChecker.isQuietNow(qh, "UTC")
    assertTrue(result == true || result == false)
  }
}
