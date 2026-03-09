package net.poopyfeed.pf

import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.models.ChildInvite
import net.poopyfeed.pf.data.models.ChildShare
import net.poopyfeed.pf.data.models.DashboardSummaryResponse
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.models.DiaperListResponse
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.models.FeedingListResponse
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.models.NapListResponse
import net.poopyfeed.pf.data.models.Notification
import net.poopyfeed.pf.data.models.PaginatedResponse
import net.poopyfeed.pf.data.models.PatternAlert
import net.poopyfeed.pf.data.models.PatternAlertsResponse
import net.poopyfeed.pf.data.models.ShareInvite
import net.poopyfeed.pf.data.models.SummaryDiapers
import net.poopyfeed.pf.data.models.SummaryFeedings
import net.poopyfeed.pf.data.models.SummarySleep
import net.poopyfeed.pf.data.models.TodaySummary
import net.poopyfeed.pf.data.models.UserProfile
import net.poopyfeed.pf.data.models.WeeklySummary

/**
 * Shared test fixtures for API models. Use in unit tests to avoid duplicating mock construction and
 * to keep mocks consistent when API models change.
 */
object TestFixtures {

  private const val TS = "2024-01-15T10:00:00Z"
  private const val TS12 = "2024-01-15T12:00:00Z"
  private const val TS13 = "2024-01-15T13:00:00Z"
  private const val TS14 = "2024-01-15T14:00:00Z"
  private const val TS14_30 = "2024-01-15T14:30:00Z"

  fun mockChild(
      id: Int = 1,
      name: String = "Baby Alice",
      date_of_birth: String = "2024-01-15",
      gender: String = "F",
      user_role: String = "owner",
      created_at: String = TS,
      updated_at: String = TS,
      last_feeding: String? = TS12,
      last_diaper_change: String? = TS14_30,
      last_nap: String? = TS13,
      can_edit: Boolean = true,
      feeding_reminder_interval: Int? = null,
      custom_bottle_low_oz: String? = null,
      custom_bottle_mid_oz: String? = null,
      custom_bottle_high_oz: String? = null,
  ) =
      Child(
          id = id,
          name = name,
          date_of_birth = date_of_birth,
          gender = gender,
          user_role = user_role,
          created_at = created_at,
          updated_at = updated_at,
          last_feeding = last_feeding,
          last_diaper_change = last_diaper_change,
          last_nap = last_nap,
          can_edit = can_edit,
          feeding_reminder_interval = feeding_reminder_interval,
          custom_bottle_low_oz = custom_bottle_low_oz,
          custom_bottle_mid_oz = custom_bottle_mid_oz,
          custom_bottle_high_oz = custom_bottle_high_oz,
      )

  fun mockFeeding(
      id: Int = 1,
      child: Int = 1,
      feeding_type: String = "bottle",
      amount_oz: Double? = 4.0,
      timestamp: String = TS12,
      created_at: String = TS12,
      updated_at: String = TS12,
      duration_minutes: Int? = null,
      side: String? = null,
  ) =
      Feeding(
          id = id,
          child = child,
          feeding_type = feeding_type,
          amount_oz = amount_oz,
          timestamp = timestamp,
          created_at = created_at,
          updated_at = updated_at,
          duration_minutes = duration_minutes,
          side = side,
      )

  /** List response shape returned by the feedings API (fed_at, amount_oz as string). */
  fun mockFeedingListResponse(
      id: Int = 1,
      feeding_type: String = "bottle",
      fed_at: String = TS12,
      amount_oz: String? = "4.0",
      created_at: String = TS12,
      updated_at: String = TS12,
      duration_minutes: Int? = null,
      side: String? = null,
  ) =
      FeedingListResponse(
          id = id,
          feeding_type = feeding_type,
          fed_at = fed_at,
          amount_oz = amount_oz,
          created_at = created_at,
          updated_at = updated_at,
          duration_minutes = duration_minutes,
          side = side,
      )

  fun mockDiaper(
      id: Int = 1,
      child: Int = 1,
      change_type: String = "both",
      timestamp: String = TS14,
      created_at: String = TS14,
      updated_at: String = TS14
  ) =
      Diaper(
          id = id,
          child = child,
          change_type = change_type,
          timestamp = timestamp,
          created_at = created_at,
          updated_at = updated_at)

  /** List response shape returned by the diapers API (changed_at). */
  fun mockDiaperListResponse(
      id: Int = 1,
      change_type: String = "both",
      changed_at: String = TS14,
      created_at: String = TS14,
      updated_at: String = TS14
  ) =
      DiaperListResponse(
          id = id,
          change_type = change_type,
          changed_at = changed_at,
          created_at = created_at,
          updated_at = updated_at)

  fun mockNap(
      id: Int = 1,
      child: Int = 1,
      start_time: String = TS13,
      end_time: String? = TS14,
      created_at: String = TS13,
      updated_at: String = TS14
  ) =
      Nap(
          id = id,
          child = child,
          start_time = start_time,
          end_time = end_time,
          created_at = created_at,
          updated_at = updated_at)

  /** List response shape returned by the naps API (napped_at, ended_at). */
  fun mockNapListResponse(
      id: Int = 1,
      napped_at: String = TS13,
      ended_at: String? = TS14,
      created_at: String = TS13,
      updated_at: String = TS14
  ) =
      NapListResponse(
          id = id,
          napped_at = napped_at,
          ended_at = ended_at,
          created_at = created_at,
          updated_at = updated_at)

  fun mockUserProfile(
      id: Int = 1,
      email: String = "user@example.com",
      first_name: String = "Test",
      last_name: String = "User",
      timezone: String = "UTC"
  ) =
      UserProfile(
          id = id,
          email = email,
          first_name = first_name,
          last_name = last_name,
          timezone = timezone)

  fun mockNotification(
      id: Int = 1,
      eventType: String = "feeding",
      message: String = "Baby was fed",
      isRead: Boolean = false,
      createdAt: String = TS12,
      actorName: String = "Caregiver",
      childName: String = "Baby Alice",
      childId: Int = 1
  ) =
      Notification(
          id = id,
          eventType = eventType,
          message = message,
          isRead = isRead,
          createdAt = createdAt,
          actorName = actorName,
          childName = childName,
          childId = childId)

  fun mockShareInvite(
      id: Int = 1,
      child: Int = 1,
      token: String = "abc123token",
      role: String = "co-parent",
      isActive: Boolean = true,
      createdAt: String = TS12,
      inviteUrl: String? = null
  ) =
      ShareInvite(
          id = id,
          child = child,
          token = token,
          role = role,
          isActive = isActive,
          createdAt = createdAt,
          inviteUrl = inviteUrl)

  fun mockChildShare(
      id: Int = 1,
      userEmail: String = "partner@example.com",
      role: String = "co-parent",
      roleDisplay: String? = "Co-parent",
      createdAt: String = TS12
  ) =
      ChildShare(
          id = id,
          userEmail = userEmail,
          role = role,
          roleDisplay = roleDisplay,
          createdAt = createdAt)

  fun mockChildInvite(
      id: Int = 1,
      token: String = "abc123token",
      role: String = "co-parent",
      roleDisplay: String = "Co-parent",
      isActive: Boolean = true,
      createdAt: String = TS12,
      inviteUrl: String? = "https://example.com/children/accept-invite/abc123token/"
  ) =
      ChildInvite(
          id = id,
          token = token,
          role = role,
          roleDisplay = roleDisplay,
          isActive = isActive,
          createdAt = createdAt,
          inviteUrl = inviteUrl)

  fun mockDashboardSummaryResponse(
      childId: Int = 1,
      todayFeedings: Int = 5,
      todayDiapers: Int = 3,
      todayNaps: Int = 2,
      unreadCount: Int = 0,
  ): DashboardSummaryResponse {
    val today =
        TodaySummary(
            childId = childId,
            period = "today",
            feedings =
                SummaryFeedings(count = todayFeedings, totalOz = 12.0, bottle = 3, breast = 2),
            diapers = SummaryDiapers(count = todayDiapers, wet = 1, dirty = 1, both = 1),
            sleep = SummarySleep(naps = todayNaps, totalMinutes = 90, avgDuration = 45),
            lastUpdated = TS,
        )
    val weekly =
        WeeklySummary(
            childId = childId,
            period = "2024-01-09 to 2024-01-15",
            feedings = SummaryFeedings(count = 35, totalOz = 84.0, bottle = 20, breast = 15),
            diapers = SummaryDiapers(count = 21, wet = 7, dirty = 7, both = 7),
            sleep = SummarySleep(naps = 14, totalMinutes = 630, avgDuration = 45),
            lastUpdated = TS,
        )
    return DashboardSummaryResponse(today = today, weekly = weekly, unreadCount = unreadCount)
  }

  fun mockPatternAlert(
      alert: Boolean = false,
      message: String? = null,
      data_points: Int = 0,
  ) = PatternAlert(alert = alert, message = message, data_points = data_points)

  fun mockPatternAlertsResponse(
      childId: Int = 1,
      feedingAlert: Boolean = false,
      feedingMessage: String? = null,
      feedingDataPoints: Int = 0,
      napAlert: Boolean = false,
      napMessage: String? = null,
      napDataPoints: Int = 0,
  ) =
      PatternAlertsResponse(
          child_id = childId,
          feeding =
              PatternAlert(
                  alert = feedingAlert, message = feedingMessage, data_points = feedingDataPoints),
          nap = PatternAlert(alert = napAlert, message = napMessage, data_points = napDataPoints),
      )

  /** Mock paginated response for feedings (used in RemoteMediator tests). */
  fun mockPaginatedFeedingsResponse(
      results: List<FeedingListResponse> = listOf(mockFeedingListResponse()),
      count: Int = results.size,
      next: String? = null,
      previous: String? = null,
  ): PaginatedResponse<FeedingListResponse> =
      PaginatedResponse(
          count = count,
          next = next,
          previous = previous,
          results = results
      )

  /** Mock paginated response for generic type (base pagination pattern). */
  inline fun <reified T> mockPaginatedResponse(
      results: List<T>,
      count: Int = results.size,
      next: String? = null,
      previous: String? = null,
  ): PaginatedResponse<T> =
      PaginatedResponse(
          count = count,
          next = next,
          previous = previous,
          results = results
      )
}
