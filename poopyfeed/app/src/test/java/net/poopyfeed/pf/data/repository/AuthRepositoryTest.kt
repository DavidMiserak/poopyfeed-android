package net.poopyfeed.pf.data.repository

import io.mockk.Ordering
import io.mockk.coVerify
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.TestFixtures
import net.poopyfeed.pf.data.api.PoopyFeedApiService
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.AuthTokenResponse
import net.poopyfeed.pf.data.models.SessionLoginResponse
import net.poopyfeed.pf.data.models.SignupRequest
import net.poopyfeed.pf.data.models.UserProfile
import net.poopyfeed.pf.data.models.UserProfileUpdate
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthRepositoryTest {

  private lateinit var apiService: PoopyFeedApiService
  private lateinit var repository: AuthRepository
  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setup() {
    apiService = io.mockk.mockk()
    repository = AuthRepository(apiService, ioDispatcher = testDispatcher)
  }

  @Test
  fun `login success returns token in Success`() = runTest {
    val email = "user@example.com"
    val password = "password123"

    io.mockk.coEvery {
      apiService.sessionLogin(net.poopyfeed.pf.data.models.LoginRequest(email, password))
    } returns SessionLoginResponse(status = 200)

    io.mockk.coEvery { apiService.fetchAuthToken() } returns
        AuthTokenResponse(auth_token = "token-123")

    val result = repository.login(email, password)

    assertIs<ApiResult.Success<String>>(result)
    assertEquals("token-123", result.data)
  }

  @Test
  fun `login calls sessionLogin then fetchAuthToken in order`() = runTest {
    io.mockk.coEvery { apiService.sessionLogin(any()) } returns SessionLoginResponse(status = 200)
    io.mockk.coEvery { apiService.fetchAuthToken() } returns
        AuthTokenResponse(auth_token = "token-xyz")

    repository.login("user@example.com", "password123")

    coVerify(ordering = Ordering.ORDERED) {
      apiService.sessionLogin(any())
      apiService.fetchAuthToken()
    }
  }

  @Test
  fun `login token exchange failure returns Error`() = runTest {
    io.mockk.coEvery { apiService.sessionLogin(any()) } returns SessionLoginResponse(status = 200)
    io.mockk.coEvery { apiService.fetchAuthToken() } throws IOException("Token exchange failed")

    val result = repository.login("user@example.com", "password123")

    assertIs<ApiResult.Error<String>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `login network error returns Error with NetworkError`() = runTest {
    io.mockk.coEvery { apiService.sessionLogin(any()) } throws IOException("Network down")

    val result = repository.login("user@example.com", "password123")

    assertIs<ApiResult.Error<String>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `signup success returns token in Success`() = runTest {
    val email = "user@example.com"
    val password = "password123"

    io.mockk.coEvery {
      apiService.signup(SignupRequest(email = email, password = password))
    } returns SessionLoginResponse(status = 200)

    io.mockk.coEvery { apiService.fetchAuthToken() } returns
        AuthTokenResponse(auth_token = "token-456")

    val result = repository.signup(email, password)

    assertIs<ApiResult.Success<String>>(result)
    assertEquals("token-456", result.data)
  }

  @Test
  fun `signup calls signup then fetchAuthToken in order`() = runTest {
    io.mockk.coEvery { apiService.signup(any()) } returns SessionLoginResponse(status = 200)
    io.mockk.coEvery { apiService.fetchAuthToken() } returns
        AuthTokenResponse(auth_token = "token-789")

    repository.signup("new@example.com", "password123")

    coVerify(ordering = Ordering.ORDERED) {
      apiService.signup(any())
      apiService.fetchAuthToken()
    }
  }

  @Test
  fun `signup token exchange failure returns Error`() = runTest {
    io.mockk.coEvery { apiService.signup(any()) } returns SessionLoginResponse(status = 200)
    io.mockk.coEvery { apiService.fetchAuthToken() } throws IOException("Token exchange failed")

    val result = repository.signup("new@example.com", "password123")

    assertIs<ApiResult.Error<String>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }

  @Test
  fun `getProfile success returns Success with profile`() = runTest {
    val profile = TestFixtures.mockUserProfile()
    io.mockk.coEvery { apiService.getProfile() } returns profile

    val result = repository.getProfile()

    assertIs<ApiResult.Success<UserProfile>>(result)
    assertEquals(profile, result.data)
  }

  @Test
  fun `getProfile http error returns Error with HttpError`() = runTest {
    val errorResponse = retrofit2.Response.error<UserProfile>(404, "Not Found".toResponseBody(null))

    io.mockk.coEvery { apiService.getProfile() } throws retrofit2.HttpException(errorResponse)

    val result = repository.getProfile()

    assertIs<ApiResult.Error<UserProfile>>(result)
    assertIs<ApiError.HttpError>(result.error)
    assertEquals(404, result.error.statusCode)
  }

  @Test
  fun `updateProfile success returns updated profile`() = runTest {
    val update =
        UserProfileUpdate(first_name = "New", last_name = "Name", timezone = "Europe/Berlin")
    val updatedProfile =
        TestFixtures.mockUserProfile(
            first_name = "New", last_name = "Name", timezone = "Europe/Berlin")
    io.mockk.coEvery { apiService.updateProfile(update) } returns updatedProfile

    val result = repository.updateProfile(update)

    assertIs<ApiResult.Success<UserProfile>>(result)
    assertEquals(updatedProfile, result.data)
  }

  @Test
  fun `logout success returns Success Unit`() = runTest {
    io.mockk.coEvery { apiService.logoutSession() } returns Unit

    val result = repository.logout()

    assertIs<ApiResult.Success<Unit>>(result)
  }

  @Test
  fun `logout network error returns Error`() = runTest {
    io.mockk.coEvery { apiService.logoutSession() } throws IOException("Network down")

    val result = repository.logout()

    assertIs<ApiResult.Error<Unit>>(result)
    assertIs<ApiError.NetworkError>(result.error)
  }
}
