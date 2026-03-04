package net.poopyfeed.pf

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import net.poopyfeed.pf.data.models.UserProfileUpdate
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSettingsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val mockAuthRepository: AuthRepository = mockk(relaxed = true)
  private val mockTokenManager: TokenManager = mockk(relaxed = true)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun `loadProfile success emits Ready with sorted timezones`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val profile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(profile)

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Ready>(state)
    assertEquals(profile, state.profile)
    assertTrue(state.timezones.isNotEmpty())
    assertEquals(state.timezones.sorted(), state.timezones)
  }

  @Test
  fun `loadProfile with missing token emits Unauthorized`() = runTest {
    every { mockTokenManager.getToken() } returns null

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Unauthorized>(state)
  }

  @Test
  fun `loadProfile with 401 error clears token and emits Unauthorized`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"

    val httpError = ApiError.HttpError(statusCode = 401, errorMessage = "Unauthorized")
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Error(httpError)
    every { mockTokenManager.clearToken() } returns Unit

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Unauthorized>(state)
    verify(exactly = 1) { mockTokenManager.clearToken() }
  }

  @Test
  fun `loadProfile with non-401 error emits Error`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"

    val networkError = ApiError.NetworkError("Network down")
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Error(networkError)

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Error>(state)
    assertEquals(networkError.getUserMessage(), state.message)
  }

  @Test
  fun `saveProfile from Ready with success emits Saved`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile(first_name = "Old", last_name = "Name")
    val updatedProfile =
        initialProfile.copy(first_name = "New", last_name = "Name", timezone = "Europe/Berlin")

    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    coEvery {
      mockAuthRepository.updateProfile(
          UserProfileUpdate(first_name = "New", last_name = "Name", timezone = "Europe/Berlin"))
    } returns ApiResult.Success(updatedProfile)

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager)

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
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile(first_name = "Old", last_name = "Name")
    val apiError = ApiError.NetworkError("Network down")

    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    coEvery { mockAuthRepository.updateProfile(any()) } returns ApiResult.Error(apiError)

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager)

    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.saveProfile(firstName = "New", lastName = "Name", timezone = "Europe/Berlin")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Error>(state)
    assertEquals(apiError.getUserMessage(), state.message)
  }
}
