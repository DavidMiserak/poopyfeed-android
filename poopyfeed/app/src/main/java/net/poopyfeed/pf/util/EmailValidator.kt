package net.poopyfeed.pf.util

import android.util.Patterns

/**
 * Shared email validation for auth flows (login, signup). Uses Android's [Patterns.EMAIL_ADDRESS];
 * reuse for consistency and testability.
 */
object EmailValidator {

  /** Returns true if [email] is non-empty after trim and matches a valid email format. */
  fun isValid(email: String): Boolean {
    val trimmed = email.trim()
    return trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
  }
}
