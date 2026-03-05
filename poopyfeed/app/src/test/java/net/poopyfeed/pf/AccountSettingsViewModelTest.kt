package net.poopyfeed.pf

import android.content.Context
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

private const val VALID_PASSWORD = "password123"
private const val NEW_PASSWORD = "newpassword123"
private const val NEW_TOKEN = "new-auth-token"

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSettingsViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val mockAuthRepository: AuthRepository = mockk(relaxed = true)
  private val mockTokenManager: TokenManager = mockk(relaxed = true)
  private val mockContext: Context = mockk(relaxed = true)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    every { mockContext.getString(R.string.error_network) } returns
        "Network error. Please check your connection."
    every { mockContext.getString(R.string.error_serialization) } returns
        "Data format error. Please try again."
    every { mockContext.getString(R.string.error_unknown) } returns
        "Something went wrong. Please try again."
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

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)

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

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)

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

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)

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

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Error>(state)
    assertEquals(networkError.getUserMessage(mockContext), state.message)
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

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)

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

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)

    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.saveProfile(firstName = "New", lastName = "Name", timezone = "Europe/Berlin")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.Error>(state)
    assertEquals(apiError.getUserMessage(mockContext), state.message)
  }

  // ========== PASSWORD CHANGE TESTS ==========

  @Test
  fun `changePassword success stores new token and emits PasswordChanged`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    coEvery { mockAuthRepository.changePassword(VALID_PASSWORD, NEW_PASSWORD) } returns
        ApiResult.Success(NEW_TOKEN)
    every { mockTokenManager.setToken(NEW_TOKEN) } returns Unit

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.changePassword(VALID_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.PasswordChanged>(state)
    verify(exactly = 1) { mockTokenManager.setToken(NEW_TOKEN) }
  }

  @Test
  fun `changePassword with mismatched passwords emits PasswordChangeError`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    every { mockContext.getString(R.string.account_password_validation_error_mismatch) } returns
        "Passwords do not match."

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.changePassword(VALID_PASSWORD, NEW_PASSWORD, "different-password")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.PasswordChangeError>(state)
    assertEquals("Passwords do not match.", state.message)
  }

  @Test
  fun `changePassword with short password emits PasswordChangeError`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    every { mockContext.getString(R.string.account_password_validation_error_short) } returns
        "Password must be at least 8 characters."

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.changePassword(VALID_PASSWORD, "short", "short")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.PasswordChangeError>(state)
    assertEquals("Password must be at least 8 characters.", state.message)
  }

  @Test
  fun `changePassword with empty current password emits PasswordChangeError`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    every { mockContext.getString(R.string.account_current_password_label) } returns
        "Current password"

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.changePassword("", NEW_PASSWORD, NEW_PASSWORD)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.PasswordChangeError>(state)
  }

  @Test
  fun `changePassword with API error emits PasswordChangeError`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    val apiError =
        ApiError.HttpError(statusCode = 400, errorMessage = "Bad Request", detail = "Invalid password")

    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    coEvery { mockAuthRepository.changePassword(VALID_PASSWORD, NEW_PASSWORD) } returns
        ApiResult.Error(apiError)

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.changePassword(VALID_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.PasswordChangeError>(state)
    assertEquals("Invalid password", state.message)
  }

  // ========== ACCOUNT DELETION TESTS ==========

  @Test
  fun `deleteAccount success clears token and emits AccountDeleted`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    coEvery { mockAuthRepository.deleteAccount(VALID_PASSWORD) } returns ApiResult.Success(Unit)
    every { mockTokenManager.clearToken() } returns Unit

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount(VALID_PASSWORD)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.AccountDeleted>(state)
    verify(exactly = 1) { mockTokenManager.clearToken() }
  }

  @Test
  fun `deleteAccount with empty password emits DeletionError`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    every { mockContext.getString(R.string.account_current_password_label) } returns
        "Current password"

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount("")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.DeletionError>(state)
  }

  @Test
  fun `deleteAccount with wrong password emits DeletionError`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    val apiError =
        ApiError.HttpError(
            statusCode = 400,
            errorMessage = "Bad Request",
            detail = "Current password is incorrect."
        )

    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    coEvery { mockAuthRepository.deleteAccount("wrong-password") } returns
        ApiResult.Error(apiError)

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount("wrong-password")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.DeletionError>(state)
    assertEquals("Current password is incorrect.", state.message)
  }

  @Test
  fun `deleteAccount with network error emits DeletionError`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    val apiError = ApiError.NetworkError("Network down")

    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)
    coEvery { mockAuthRepository.deleteAccount(VALID_PASSWORD) } returns ApiResult.Error(apiError)

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount(VALID_PASSWORD)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<AccountSettingsUiState.DeletionError>(state)
    assertEquals(apiError.getUserMessage(mockContext), state.message)
  }

  // ========== STATE CLEARING TESTS ==========

  @Test
  fun `clearPasswordChangeState from PasswordChanged reloads profile`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val initialProfile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(initialProfile)

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    // Simulate reaching PasswordChanged state
    viewModel.clearPasswordChangeState()
    testDispatcher.scheduler.advanceUntilIdle()

    verify(atLeast = 2) { mockAuthRepository.getProfile() }
  }

  @Test
  fun `changePassword no-op when not in Ready state`() = runTest {
    every { mockTokenManager.getToken() } returns null

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.changePassword(VALID_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
    testDispatcher.scheduler.advanceUntilIdle()

    assertIs<AccountSettingsUiState.Unauthorized>(viewModel.uiState.value)
    verify(exactly = 0) { mockAuthRepository.changePassword(any(), any()) }
  }

  @Test
  fun `deleteAccount no-op when not in Ready state`() = runTest {
    every { mockTokenManager.getToken() } returns null

    val viewModel = AccountSettingsViewModel(mockAuthRepository, mockTokenManager, mockContext)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.deleteAccount(VALID_PASSWORD)
    testDispatcher.scheduler.advanceUntilIdle()

    assertIs<AccountSettingsUiState.Unauthorized>(viewModel.uiState.value)
    verify(exactly = 0) { mockAuthRepository.deleteAccount(any()) }
  }
}
