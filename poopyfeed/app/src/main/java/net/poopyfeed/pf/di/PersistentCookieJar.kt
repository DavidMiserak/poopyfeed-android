package net.poopyfeed.pf.di

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

private const val PREF_KEY_COOKIES = "persistent_cookies"

/** DTO for persisting a cookie to SharedPreferences. Maps to [Cookie] fields. */
@Serializable
internal data class StoredCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean
)

/**
 * [CookieJar] that persists cookies to SharedPreferences so they survive process death. Used for
 * the two-step auth flow (sessionLogin then fetchAuthToken). Thread-safe.
 */
internal class PersistentCookieJar(private val prefs: SharedPreferences, private val json: Json) :
    CookieJar {

  private val lock = Any()

  @Volatile private var cachedCookies: List<StoredCookie>? = null

  override fun loadForRequest(url: HttpUrl): List<Cookie> {
    return synchronized(lock) {
      val stored = cachedCookies ?: loadFromPrefs()
      cachedCookies = stored
      val now = System.currentTimeMillis()
      stored
          .filter { it.expiresAt > now }
          .mapNotNull { toCookie(url, it) }
          .filter { it.matches(url) }
    }
  }

  override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
    if (cookies.isEmpty()) return
    synchronized(lock) {
      val stored = (cachedCookies ?: loadFromPrefs()).toMutableList()
      val now = System.currentTimeMillis()
      stored.removeAll { it.expiresAt <= now }
      for (cookie in cookies) {
        if (cookie.persistent || cookie.expiresAt > now) {
          stored.removeAll {
            it.name == cookie.name && it.domain == cookie.domain && it.path == cookie.path
          }
          stored.add(toStoredCookie(cookie))
        }
      }
      cachedCookies = stored
      prefs.edit {
        putString(
            PREF_KEY_COOKIES,
            json.encodeToString(ListSerializer(StoredCookie.serializer()), stored))
      }
    }
  }

  private fun loadFromPrefs(): List<StoredCookie> {
    val raw = prefs.getString(PREF_KEY_COOKIES, null) ?: return emptyList()
    return try {
      json.decodeFromString(ListSerializer(StoredCookie.serializer()), raw)
    } catch (_: Exception) {
      emptyList()
    }
  }

  private fun toStoredCookie(c: Cookie): StoredCookie =
      StoredCookie(
          name = c.name,
          value = c.value,
          domain = c.domain,
          path = c.path,
          expiresAt = c.expiresAt,
          secure = c.secure,
          httpOnly = c.httpOnly,
          hostOnly = c.hostOnly)

  private fun toCookie(url: HttpUrl, s: StoredCookie): Cookie? =
      try {
        val builder =
            Cookie.Builder()
                .name(s.name)
                .value(s.value)
                .domain(s.domain)
                .path(s.path)
                .expiresAt(s.expiresAt)
        if (s.secure) builder.secure()
        if (s.httpOnly) builder.httpOnly()
        if (s.hostOnly) builder.hostOnlyDomain(s.domain)
        builder.build()
      } catch (_: Exception) {
        null
      }

  companion object {
    /** Removes persisted cookies from the given SharedPreferences. */
    fun clear(prefs: SharedPreferences) {
      prefs.edit { remove(PREF_KEY_COOKIES) }
    }
  }
}
