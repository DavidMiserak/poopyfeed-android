package net.poopyfeed.pf.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that short-circuits network requests if the device has no active internet
 * connection. This provides instant feedback to the user instead of waiting for OkHttp timeout.
 *
 * If no active network exists or the network lacks INTERNET capability, throws IOException("No
 * internet connection"), which [ErrorMapper.toApiError] maps to [ApiError.NetworkError] →
 * [R.string.error_network].
 */
internal class ConnectivityInterceptor(private val context: Context) : Interceptor {
  override fun intercept(chain: Interceptor.Chain): Response {
    if (!isConnected()) {
      throw IOException("No internet connection")
    }
    return chain.proceed(chain.request())
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
