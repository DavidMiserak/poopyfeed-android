package net.poopyfeed.pf.data.models

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ErrorHandlingTest {

    @Test
    fun `ApiError getUserMessage returns detail for HttpError`() {
        val error = ApiError.HttpError(
            statusCode = 400,
            errorMessage = "Bad Request",
            detail = "Email is invalid"
        )

        assertEquals("Email is invalid", error.getUserMessage())
    }

    @Test
    fun `ApiError getUserMessage returns generic messages for other types`() {
        val network = ApiError.NetworkError("No internet")
        val serialization = ApiError.SerializationError("Bad data")
        val unknown = ApiError.UnknownError("Something broke")

        assertEquals("Network error. Please check your connection.", network.getUserMessage())
        assertEquals("Data format error. Please try again.", serialization.getUserMessage())
        assertEquals("Something went wrong. Please try again.", unknown.getUserMessage())
    }

    @Test
    fun `toApiError maps HttpException to HttpError`() {
        val response = retrofit2.Response.error<Unit>(
            404,
            "Not Found".toResponseBody(null)
        )
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
}
