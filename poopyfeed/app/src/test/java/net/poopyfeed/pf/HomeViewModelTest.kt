package net.poopyfeed.pf

import android.app.Application
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.UserProfile
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.NetworkModule
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var application: Application

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    MockKAnnotations.init(this, relaxUnitFun = true)
    application = mockk(relaxed = true)

    mockkObject(NetworkModule)
    mockkConstructor(AuthRepository::class)
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun `when token present and profile loads successfully then Ready state emitted`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns "test-token"

    val profile =
        UserProfile(
            id = 1,
            email = "user@example.com",
            first_name = "Test",
            last_name = "User",
            timezone = "UTC")

    coEvery { anyConstructed<AuthRepository>().getProfile() } returns ApiResult.Success(profile)

    val viewModel = HomeViewModel(application)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<HomeUiState.Ready>(state)
    assertEquals("user@example.com", state.email)
  }

  @Test
  fun `when token is missing then Unauthorized state emitted without calling repository`() =
      runTest {
        every { NetworkModule.getAuthToken(any()) } returns null

        val viewModel = HomeViewModel(application)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Unauthorized>(state)
        coEvery { anyConstructed<AuthRepository>().getProfile() } returns
            ApiResult.Success(
                UserProfile(
                    id = 1,
                    email = "ignored@example.com",
                    first_name = "Ignored",
                    last_name = "User",
                    timezone = "UTC"))
      }

  @Test
  fun `when profile request returns 401 then token cleared and Unauthorized state emitted`() =
      runTest {
        every { NetworkModule.getAuthToken(any()) } returns "test-token"

        val httpError = ApiError.HttpError(statusCode = 401, errorMessage = "Unauthorized")
        coEvery { anyConstructed<AuthRepository>().getProfile() } returns ApiResult.Error(httpError)

        every { NetworkModule.clearAuthToken(any()) } returns Unit

        val viewModel = HomeViewModel(application)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Unauthorized>(state)
        verify(exactly = 1) { NetworkModule.clearAuthToken(any()) }
      }

  @Test
  fun `when profile request fails with non-401 error then Error state emitted`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns "test-token"

    val networkError = ApiError.NetworkError("Network down")
    coEvery { anyConstructed<AuthRepository>().getProfile() } returns ApiResult.Error(networkError)

    val viewModel = HomeViewModel(application)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<HomeUiState.Error>(state)
    assertEquals(networkError.getUserMessage(), state.message)
    verify(exactly = 0) { NetworkModule.clearAuthToken(any()) }
  }
}
