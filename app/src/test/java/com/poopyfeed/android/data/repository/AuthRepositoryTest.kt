package com.poopyfeed.android.data.repository

import com.poopyfeed.android.data.local.TokenManager
import com.poopyfeed.android.data.remote.AuthApi
import com.poopyfeed.android.data.remote.CookieStore
import com.poopyfeed.android.data.remote.dto.AllauthData
import com.poopyfeed.android.data.remote.dto.AllauthResponse
import com.poopyfeed.android.data.remote.dto.AllauthUser
import com.poopyfeed.android.data.remote.dto.TokenResponse
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class AuthRepositoryTest {
    private lateinit var authApi: AuthApi
    private lateinit var tokenManager: TokenManager
    private lateinit var cookieStore: CookieStore
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        authApi = mock()
        tokenManager = mock()
        cookieStore = mock()
        repository = AuthRepository(authApi, tokenManager, cookieStore)
    }

    @Test
    fun `login success stores token`() =
        runTest {
            val allauthResponse =
                AllauthResponse(
                    status = 200,
                    data = AllauthData(user = AllauthUser(id = 1, email = "test@example.com")),
                )
            whenever(authApi.login(org.mockito.kotlin.any()))
                .thenReturn(Response.success(allauthResponse))
            whenever(authApi.getToken())
                .thenReturn(Response.success(TokenResponse(authToken = "test-token")))

            val result = repository.login("test@example.com", "password123")

            assertTrue(result.isSuccess)
            verify(tokenManager).saveToken("test-token")
        }

    @Test
    fun `login failure returns error`() =
        runTest {
            val errorBody =
                """{"non_field_errors":["Invalid credentials"]}"""
                    .toResponseBody("application/json".toMediaType())
            whenever(authApi.login(org.mockito.kotlin.any()))
                .thenReturn(Response.error(400, errorBody))

            val result = repository.login("test@example.com", "wrong")

            assertTrue(result.isFailure)
            assertEquals("Invalid credentials", result.exceptionOrNull()?.message)
        }

    @Test
    fun `login token retrieval failure returns error`() =
        runTest {
            val allauthResponse =
                AllauthResponse(
                    status = 200,
                    data = AllauthData(user = AllauthUser(id = 1, email = "test@example.com")),
                )
            whenever(authApi.login(org.mockito.kotlin.any()))
                .thenReturn(Response.success(allauthResponse))
            val errorBody = "".toResponseBody("application/json".toMediaType())
            whenever(authApi.getToken())
                .thenReturn(Response.error(401, errorBody))

            val result = repository.login("test@example.com", "password123")

            assertTrue(result.isFailure)
            assertEquals("Failed to retrieve auth token", result.exceptionOrNull()?.message)
        }

    @Test
    fun `signup success stores token`() =
        runTest {
            val allauthResponse =
                AllauthResponse(
                    status = 200,
                    data = AllauthData(user = AllauthUser(id = 1, email = "new@example.com")),
                )
            whenever(authApi.signup(org.mockito.kotlin.any()))
                .thenReturn(Response.success(allauthResponse))
            whenever(authApi.getToken())
                .thenReturn(Response.success(TokenResponse(authToken = "new-token")))

            val result = repository.signup("new@example.com", "password123")

            assertTrue(result.isSuccess)
            verify(tokenManager).saveToken("new-token")
        }

    @Test
    fun `signup failure returns error`() =
        runTest {
            val errorBody =
                """{"email":["A user with this email already exists."]}"""
                    .toResponseBody("application/json".toMediaType())
            whenever(authApi.signup(org.mockito.kotlin.any()))
                .thenReturn(Response.error(409, errorBody))

            val result = repository.signup("existing@example.com", "password123")

            assertTrue(result.isFailure)
            assertEquals("A user with this email already exists.", result.exceptionOrNull()?.message)
        }

    @Test
    fun `logout clears token and cookies`() =
        runTest {
            whenever(authApi.logout()).thenReturn(Response.success(Unit))

            val result = repository.logout()

            assertTrue(result.isSuccess)
            verify(tokenManager).clearToken()
            verify(cookieStore).clear()
        }

    @Test
    fun `logout clears local state even when API fails`() =
        runTest {
            whenever(authApi.logout()).thenThrow(RuntimeException("Network error"))

            val result = repository.logout()

            assertTrue(result.isSuccess)
            verify(tokenManager).clearToken()
            verify(cookieStore).clear()
        }

    @Test
    fun `hasToken returns true when token exists`() =
        runTest {
            whenever(tokenManager.getToken()).thenReturn("some-token")

            assertTrue(repository.hasToken())
        }

    @Test
    fun `hasToken returns false when no token`() =
        runTest {
            whenever(tokenManager.getToken()).thenReturn(null)

            assertTrue(!repository.hasToken())
        }

    // Error parsing tests
    @Test
    fun `parseErrorBody handles non_field_errors`() {
        val body = """{"non_field_errors":["Error one","Error two"]}"""
        val result = AuthRepository.parseErrorBody(body)
        assertEquals("Error one. Error two", result)
    }

    @Test
    fun `parseErrorBody handles detail field`() {
        val body = """{"detail":"Not authenticated"}"""
        val result = AuthRepository.parseErrorBody(body)
        assertEquals("Not authenticated", result)
    }

    @Test
    fun `parseErrorBody handles field errors`() {
        val body = """{"email":["This field is required."],"password":["Too short."]}"""
        val result = AuthRepository.parseErrorBody(body)
        assertTrue(result.contains("This field is required."))
        assertTrue(result.contains("Too short."))
    }

    @Test
    fun `parseErrorBody handles null body`() {
        val result = AuthRepository.parseErrorBody(null)
        assertEquals(RepositoryConstants.UNKNOWN_ERROR_MESSAGE, result)
    }

    @Test
    fun `parseErrorBody handles empty body`() {
        val result = AuthRepository.parseErrorBody("")
        assertEquals(RepositoryConstants.UNKNOWN_ERROR_MESSAGE, result)
    }

    @Test
    fun `parseErrorBody handles invalid JSON`() {
        val result = AuthRepository.parseErrorBody("not json")
        assertEquals(RepositoryConstants.UNKNOWN_ERROR_MESSAGE, result)
    }

    @Test
    fun `getNetworkErrorMessage handles no internet`() {
        val result =
            AuthRepository.getNetworkErrorMessage(
                Exception("Unable to resolve host api.example.com"),
            )
        assertEquals("No internet connection. Please check your network.", result)
    }

    @Test
    fun `getNetworkErrorMessage handles timeout`() {
        val result = AuthRepository.getNetworkErrorMessage(Exception("connect timeout"))
        assertEquals("Request timed out. Please try again.", result)
    }

    @Test
    fun `getNetworkErrorMessage handles connection refused`() {
        val result = AuthRepository.getNetworkErrorMessage(Exception("Connection refused"))
        assertEquals("Cannot reach the server. Please try again later.", result)
    }

    @Test
    fun `getNetworkErrorMessage handles generic error`() {
        val result = AuthRepository.getNetworkErrorMessage(Exception("Something broke"))
        assertEquals("Something broke", result)
    }
}
