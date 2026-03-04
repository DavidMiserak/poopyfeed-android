package net.poopyfeed.pf

import android.content.Context
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

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
  fun `checkExistingToken returns true when token present`() {
    every { mockTokenManager.getToken() } returns "test-token"

    val viewModel = LoginViewModel(mockAuthRepository, mockTokenManager, mockContext)

    assertTrue(viewModel.checkExistingToken())
  }

  @Test
  fun `checkExistingToken returns false when token null`() {
    every { mockTokenManager.getToken() } returns null

    val viewModel = LoginViewModel(mockAuthRepository, mockTokenManager, mockContext)

    assertFalse(viewModel.checkExistingToken())
  }

  @Test
  fun `initial state is Idle`() {
    every { mockTokenManager.getToken() } returns null

    val viewModel = LoginViewModel(mockAuthRepository, mockTokenManager, mockContext)

    assertIs<LoginUiState.Idle>(viewModel.uiState.value)
  }

  @Test
  fun `login success saves token and emits Success`() = runTest {
    every { mockTokenManager.getToken() } returns null
    every { mockTokenManager.saveToken(any()) } returns Unit
    coEvery { mockAuthRepository.login(any(), any()) } returns ApiResult.Success("token-123")

    val viewModel = LoginViewModel(mockAuthRepository, mockTokenManager, mockContext)
    viewModel.login("user@example.com", "password123")

    testDispatcher.scheduler.advanceUntilIdle()

    assertIs<LoginUiState.Success>(viewModel.uiState.value)
    verify(exactly = 1) { mockTokenManager.saveToken("token-123") }
  }

  @Test
  fun `login error emits Error state with message`() = runTest {
    every { mockTokenManager.getToken() } returns null
    val apiError = ApiError.NetworkError("Network down")
    coEvery { mockAuthRepository.login(any(), any()) } returns ApiResult.Error(apiError)

    val viewModel = LoginViewModel(mockAuthRepository, mockTokenManager, mockContext)
    viewModel.login("user@example.com", "wrong")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<LoginUiState.Error>(state)
    assertEquals(apiError.getUserMessage(mockContext), state.message)
  }

  @Test
  fun `clearError from Error state sets Idle`() = runTest {
    every { mockTokenManager.getToken() } returns null
    coEvery { mockAuthRepository.login(any(), any()) } returns
        ApiResult.Error(ApiError.NetworkError("err"))

    val viewModel = LoginViewModel(mockAuthRepository, mockTokenManager, mockContext)
    viewModel.login("a@b.com", "p")
    testDispatcher.scheduler.advanceUntilIdle()
    assertIs<LoginUiState.Error>(viewModel.uiState.value)

    viewModel.clearError()
    assertIs<LoginUiState.Idle>(viewModel.uiState.value)
  }

  @Test
  fun `clearError when not Error state leaves state unchanged`() {
    every { mockTokenManager.getToken() } returns null

    val viewModel = LoginViewModel(mockAuthRepository, mockTokenManager, mockContext)
    viewModel.clearError()

    assertIs<LoginUiState.Idle>(viewModel.uiState.value)
  }
}
