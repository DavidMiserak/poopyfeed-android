package net.poopyfeed.pf.util

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class EmailValidatorTest {

  @Test
  fun `isValid returns true for valid email`() {
    assertTrue(EmailValidator.isValid("user@example.com"))
    assertTrue(EmailValidator.isValid("test+tag@domain.co.uk"))
    assertTrue(EmailValidator.isValid("a@b.co"))
  }

  @Test
  fun `isValid returns true for email with leading and trailing spaces`() {
    assertTrue(EmailValidator.isValid("  user@example.com  "))
  }

  @Test
  fun `isValid returns false for empty string`() {
    assertFalse(EmailValidator.isValid(""))
  }

  @Test
  fun `isValid returns false for blank string`() {
    assertFalse(EmailValidator.isValid("   "))
  }

  @Test
  fun `isValid returns false for invalid formats`() {
    assertFalse(EmailValidator.isValid("notanemail"))
    assertFalse(EmailValidator.isValid("@nodomain.com"))
    assertFalse(EmailValidator.isValid("missing@"))
    assertFalse(EmailValidator.isValid("spaces in@email.com"))
  }
}
