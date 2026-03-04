package net.poopyfeed.pf

import net.poopyfeed.pf.data.models.Child
import net.poopyfeed.pf.data.models.Diaper
import net.poopyfeed.pf.data.models.Feeding
import net.poopyfeed.pf.data.models.Nap
import net.poopyfeed.pf.data.models.UserProfile

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
      last_nap: String? = TS13
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
          last_nap = last_nap)

  fun mockFeeding(
      id: Int = 1,
      child: Int = 1,
      feeding_type: String = "bottle",
      amount_oz: Double? = 4.0,
      timestamp: String = TS12,
      created_at: String = TS12,
      updated_at: String = TS12
  ) =
      Feeding(
          id = id,
          child = child,
          feeding_type = feeding_type,
          amount_oz = amount_oz,
          timestamp = timestamp,
          created_at = created_at,
          updated_at = updated_at)

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
}
