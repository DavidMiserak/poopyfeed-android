package net.poopyfeed.pf.di

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_KEY_AUTH_TOKEN = "auth_token"

@Singleton
class TokenManager @Inject constructor(private val prefs: SharedPreferences) {

  fun getToken(): String? = prefs.getString(PREF_KEY_AUTH_TOKEN, null)

  fun saveToken(token: String) {
    prefs.edit { putString(PREF_KEY_AUTH_TOKEN, token) }
  }

  fun clearToken() {
    prefs.edit { remove(PREF_KEY_AUTH_TOKEN) }
    PersistentCookieJar.clear(prefs)
  }
}
