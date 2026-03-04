package net.poopyfeed.pf.data.models

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import net.poopyfeed.pf.R
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class ErrorHandlingTest {

  private fun mockContextForErrorStrings(): Context {
    val context = mockk<Context>()
    every { context.getString(R.string.error_network) } returns
        "Network error. Please check your connection."
    every { context.getString(R.string.error_serialization) } returns
        "Data format error. Please try again."
    every { context.getString(R.string.error_unknown) } returns
        "Something went wrong. Please try again."
    return context
  }

  @Test
  fun `ApiError getUserMessage returns detail for HttpError`() {
    val context = mockk<Context>(relaxed = true)
    val error =
        ApiError.HttpError(
            statusCode = 400, errorMessage = "Bad Request", detail = "Email is invalid")
    assertEquals("Email is invalid", error.getUserMessage(context))
  }

  @Test
  fun `ApiError getUserMessage returns generic messages for other types`() {
    val context = mockContextForErrorStrings()
    val network = ApiError.NetworkError("No internet")
    val serialization = ApiError.SerializationError("Bad data")
    val unknown = ApiError.UnknownError("Something broke")
    assertEquals("Network error. Please check your connection.", network.getUserMessage(context))
    assertEquals("Data format error. Please try again.", serialization.getUserMessage(context))
    assertEquals("Something went wrong. Please try again.", unknown.getUserMessage(context))
  }

  @Test
  fun `toApiError maps HttpException to HttpError`() {
    val response = retrofit2.Response.error<Unit>(404, "Not Found".toResponseBody(null))
    val exception = retrofit2.HttpException(response)

    val apiError = exception.toApiError()

    assertIs<ApiError.HttpError>(apiError)
    assertEquals(404, apiError.statusCode)
  }

  @Test
  fun `toApiError maps IOException to NetworkError`() {
    val exception = IOException("Network down")

    val apiError = exception.toApiError()

    assertIs<ApiError.NetworkError>(apiError)
  }

  @Test
  fun `toApiError maps other exceptions to UnknownError`() {
    val exception = IllegalStateException("Unexpected")

    val apiError = exception.toApiError()

    assertIs<ApiError.UnknownError>(apiError)
  }

  @Test
  fun `ApiError getUserMessage for HttpError with null detail returns errorMessage`() {
    val context = mockk<Context>(relaxed = true)
    val error =
        ApiError.HttpError(statusCode = 500, errorMessage = "Internal Server Error", detail = null)
    assertEquals("Internal Server Error", error.getUserMessage(context))
  }
}
