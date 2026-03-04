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
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.models.UserProfile
import net.poopyfeed.pf.data.models.UserProfileUpdate
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.NetworkModule
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSettingsViewModelTest {

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
  fun `loadProfile success emits Ready with sorted timezones`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns "test-token"

    val profile =
        UserProfile(
            id = 1,
            email = "user@example.com",
            first_name = "Test",
            last_name = "User",
            timezone = "UTC")

    coEvery { anyConstructed<AuthRepository>().getProfile() } returns ApiResult.Success(profile)

    val viewModel = AccountSettingsViewModel(application)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Ready>(state)
    assertEquals(profile, state.profile)
    assertTrue(state.timezones.isNotEmpty())
    assertEquals(state.timezones.sorted(), state.timezones)
  }

  @Test
  fun `loadProfile with missing token emits Unauthorized`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns null

    val viewModel = AccountSettingsViewModel(application)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Unauthorized>(state)
  }

  @Test
  fun `loadProfile with 401 error clears token and emits Unauthorized`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns "test-token"

    val httpError = ApiError.HttpError(statusCode = 401, errorMessage = "Unauthorized")
    coEvery { anyConstructed<AuthRepository>().getProfile() } returns ApiResult.Error(httpError)
    every { NetworkModule.clearAuthToken(any()) } returns Unit

    val viewModel = AccountSettingsViewModel(application)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Unauthorized>(state)
    verify(exactly = 1) { NetworkModule.clearAuthToken(any()) }
  }

  @Test
  fun `loadProfile with non-401 error emits Error`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns "test-token"

    val networkError = ApiError.NetworkError("Network down")
    coEvery { anyConstructed<AuthRepository>().getProfile() } returns ApiResult.Error(networkError)

    val viewModel = AccountSettingsViewModel(application)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Error>(state)
    assertEquals(networkError.getUserMessage(), state.message)
  }

  @Test
  fun `saveProfile from Ready with success emits Saved`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns "test-token"

    val initialProfile =
        UserProfile(
            id = 1,
            email = "user@example.com",
            first_name = "Old",
            last_name = "Name",
            timezone = "UTC")

    val updatedProfile =
        initialProfile.copy(first_name = "New", last_name = "Name", timezone = "Europe/Berlin")

    coEvery { anyConstructed<AuthRepository>().getProfile() } returns
        ApiResult.Success(initialProfile)
    coEvery {
      anyConstructed<AuthRepository>()
          .updateProfile(
              UserProfileUpdate(first_name = "New", last_name = "Name", timezone = "Europe/Berlin"))
    } returns ApiResult.Success(updatedProfile)

    val viewModel = AccountSettingsViewModel(application)

    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.saveProfile(firstName = "New", lastName = "Name", timezone = "Europe/Berlin")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Saved>(state)
    assertEquals(updatedProfile, state.profile)
    assertTrue(state.timezones.isNotEmpty())
  }

  @Test
  fun `saveProfile error emits Error state`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns "test-token"

    val initialProfile =
        UserProfile(
            id = 1,
            email = "user@example.com",
            first_name = "Old",
            last_name = "Name",
            timezone = "UTC")

    val apiError = ApiError.NetworkError("Network down")

    coEvery { anyConstructed<AuthRepository>().getProfile() } returns
        ApiResult.Success(initialProfile)
    coEvery { anyConstructed<AuthRepository>().updateProfile(any()) } returns
        ApiResult.Error(apiError)

    val viewModel = AccountSettingsViewModel(application)

    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.saveProfile(firstName = "New", lastName = "Name", timezone = "Europe/Berlin")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Error>(state)
    assertEquals(apiError.getUserMessage(), state.message)
  }
}
