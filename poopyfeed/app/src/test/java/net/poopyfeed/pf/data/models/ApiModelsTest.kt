package net.poopyfeed.pf.data.models

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class ApiModelsTest {

  @Test
  fun `PaginatedResponse hasNextPage true when next is non-null`() {
    val response =
        PaginatedResponse(
            count = 25, next = "http://api/children/?page=2", results = (1..20).toList())
    assertTrue(response.hasNextPage)
  }

  @Test
  fun `PaginatedResponse hasNextPage false when next is null`() {
    val response = PaginatedResponse(count = 3, next = null, results = listOf(1, 2, 3))
    assertFalse(response.hasNextPage)
  }

  @Test
  fun `PaginatedResponse hasNextPage false when count zero and next null`() {
    val response = PaginatedResponse(count = 0, next = null, results = emptyList<String>())
    assertFalse(response.hasNextPage)
  }

  @Test
  fun `AuthToken and AuthTokenResponse expose token fields`() {
    val tokenResponse = AuthTokenResponse(auth_token = "token-123")
    val legacyToken = AuthToken(key = "legacy-token-xyz")

    assertEquals("token-123", tokenResponse.auth_token)
    assertEquals("legacy-token-xyz", legacyToken.key)
  }

  @Test
  fun `ChangePasswordRequest and ChangePasswordResponse map fields correctly`() {
    val request =
        ChangePasswordRequest(
            current_password = "old-pass",
            new_password = "new-pass",
            new_password_confirm = "new-pass")
    val response = ChangePasswordResponse(detail = "OK", auth_token = "rotated-token")

    assertEquals("old-pass", request.current_password)
    assertEquals("new-pass", request.new_password)
    assertEquals("new-pass", request.new_password_confirm)
    assertEquals("OK", response.detail)
    assertEquals("rotated-token", response.auth_token)
  }

  @Test
  fun `DeleteAccountRequest and CreateShareRequest map fields correctly`() {
    val deleteRequest = DeleteAccountRequest(current_password = "super-secret")
    val shareRequest = CreateShareRequest(email = "friend@example.com", role = "co-parent")

    assertEquals("super-secret", deleteRequest.current_password)
    assertEquals("friend@example.com", shareRequest.email)
    assertEquals("co-parent", shareRequest.role)
  }

  @Test
  fun `ShareInvite exposes all properties`() {
    val invite =
        ShareInvite(
            id = 1,
            child = 42,
            invited_email = "caregiver@example.com",
            role = "caregiver",
            status = "pending",
            created_at = "2024-01-01T10:00:00Z",
            updated_at = "2024-01-01T10:00:00Z")

    assertEquals(1, invite.id)
    assertEquals(42, invite.child)
    assertEquals("caregiver@example.com", invite.invited_email)
    assertEquals("caregiver", invite.role)
    assertEquals("pending", invite.status)
  }

  @Test
  fun `SignupRequest optional re_password behaves as expected`() {
    val withoutConfirm = SignupRequest(email = "user@example.com", password = "pw123")
    val withConfirm =
        SignupRequest(email = "user@example.com", password = "pw123", re_password = "pw123")

    assertNull(withoutConfirm.re_password)
    assertNotNull(withConfirm.re_password)
    assertEquals("pw123", withConfirm.re_password)
  }
}
