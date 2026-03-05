package net.poopyfeed.pf.di

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_KEY_AUTH_TOKEN = "auth_token"
private const val PREF_KEY_PROFILE_TIMEZONE = "profile_timezone"

/**
 * Manages the auth token used for API requests. Token is stored in SharedPreferences and added to
 * requests via the OkHttp auth interceptor. [clearToken] also clears [PersistentCookieJar] for full
 * logout.
 */
@Singleton
class TokenManager @Inject constructor(private val prefs: SharedPreferences) {

  /** Returns the stored auth token, or null if not logged in. */
  fun getToken(): String? = prefs.getString(PREF_KEY_AUTH_TOKEN, null)

  /** Persists the auth token for subsequent API calls. */
  fun saveToken(token: String) {
    prefs.edit { putString(PREF_KEY_AUTH_TOKEN, token) }
  }

  /** Returns the cached profile timezone (IANA format), or null if not set. */
  fun getProfileTimezone(): String? = prefs.getString(PREF_KEY_PROFILE_TIMEZONE, null)

  /** Caches the user's profile timezone from the API response. */
  fun saveProfileTimezone(tz: String) {
    prefs.edit { putString(PREF_KEY_PROFILE_TIMEZONE, tz) }
  }

  /** Removes the token and persisted session cookies (call on logout). */
  fun clearToken() {
    prefs.edit {
      remove(PREF_KEY_AUTH_TOKEN)
      remove(PREF_KEY_PROFILE_TIMEZONE)
    }
    PersistentCookieJar.clear(prefs)
  }
}
