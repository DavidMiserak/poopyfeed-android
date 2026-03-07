package net.poopyfeed.pf.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that short-circuits network requests if the device has no active internet
 * connection. This provides instant feedback to the user instead of waiting for OkHttp timeout.
 *
 * If [apiBaseUrl] is a local/private URL (e.g. 10.0.2.2, 127.0.0.1, 192.168.x.x), the connectivity
 * check is skipped so that emulator and same-LAN device setups can reach the host even when the
 * device reports no internet (common on emulators).
 *
 * If no active network exists or the network lacks INTERNET capability, throws IOException("No
 * internet connection"), which [ErrorMapper.toApiError] maps to [ApiError.NetworkError] →
 * [R.string.error_network].
 */
internal class ConnectivityInterceptor(
    private val context: Context,
    private val apiBaseUrl: String,
) : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    if (!isLocalOrPrivateUrl(apiBaseUrl) && !isConnected()) {
      throw IOException("No internet connection")
    }
    return chain.proceed(chain.request())
  }

  /**
   * True if the URL points to localhost or a private network (e.g. emulator host 10.0.2.2, or
   * machine IP 192.168.x.x). For such URLs we skip the connectivity check so the request is always
   * attempted.
   */
  private fun isLocalOrPrivateUrl(url: String): Boolean {
    val host = url.toHttpUrlOrNull()?.host ?: return false
    return host == "10.0.2.2" ||
        host == "localhost" ||
        host == "127.0.0.1" ||
        host.startsWith("192.168.") ||
        host.startsWith("10.") ||
        host.matches(Regex("^172\\.(1[6-9]|2[0-9]|3[0-1])\\..*"))
  }

  /**
   * Checks if the device has an active network with INTERNET capability. Returns false if no active
   * network exists or if the network lacks INTERNET.
   */
  private fun isConnected(): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
  }
}
