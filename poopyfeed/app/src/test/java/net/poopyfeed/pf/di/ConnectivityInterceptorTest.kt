package net.poopyfeed.pf.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlin.test.assertFailsWith
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test

class ConnectivityInterceptorTest {

  private lateinit var context: Context
  private lateinit var cm: ConnectivityManager
  private lateinit var interceptor: ConnectivityInterceptor
  private lateinit var chain: Interceptor.Chain
  private lateinit var mockResponse: Response

  /** Remote API URL so the connectivity check is enforced in tests. */
  private val remoteApiUrl = "https://poopyfeed.net/api/v1/"

  @Before
  fun setup() {
    context = mockk()
    cm = mockk()
    interceptor = ConnectivityInterceptor(context, remoteApiUrl)
    chain = mockk()

    val mockRequest = mockk<Request>()
    mockResponse =
        Response.Builder()
            .request(mockRequest)
            .protocol(okhttp3.Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .build()

    every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns cm
  }

  @Test
  fun `intercept throws IOException when no active network`() {
    every { cm.activeNetwork } returns null

    val exception = assertFailsWith<IOException> { interceptor.intercept(chain) }
    assert(exception.message == "No internet connection")
  }

  @Test
  fun `intercept throws IOException when network has no INTERNET capability`() {
    val network = mockk<Network>()
    val capabilities = mockk<NetworkCapabilities>()

    every { cm.activeNetwork } returns network
    every { cm.getNetworkCapabilities(network) } returns capabilities
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns false

    val exception = assertFailsWith<IOException> { interceptor.intercept(chain) }
    assert(exception.message == "No internet connection")
  }

  @Test
  fun `intercept throws IOException when getNetworkCapabilities returns null`() {
    val network = mockk<Network>()

    every { cm.activeNetwork } returns network
    every { cm.getNetworkCapabilities(network) } returns null

    val exception = assertFailsWith<IOException> { interceptor.intercept(chain) }
    assert(exception.message == "No internet connection")
  }

  @Test
  fun `intercept delegates to chain when connected`() {
    val network = mockk<Network>()
    val capabilities = mockk<NetworkCapabilities>()
    val mockRequest = mockk<Request>()

    every { cm.activeNetwork } returns network
    every { cm.getNetworkCapabilities(network) } returns capabilities
    every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns true
    every { chain.request() } returns mockRequest
    every { chain.proceed(mockRequest) } returns mockResponse

    val result = interceptor.intercept(chain)

    assert(result == mockResponse)
  }

  @Test
  fun `intercept delegates to chain for local API URL without checking connectivity`() {
    val localInterceptor = ConnectivityInterceptor(context, "http://10.0.2.2:8000/api/v1/")
    val mockRequest = mockk<Request>()

    every { cm.activeNetwork } returns null

    every { chain.request() } returns mockRequest
    every { chain.proceed(mockRequest) } returns mockResponse

    val result = localInterceptor.intercept(chain)

    assert(result == mockResponse)
  }
}
