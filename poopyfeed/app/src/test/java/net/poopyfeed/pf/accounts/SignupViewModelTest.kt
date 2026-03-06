package net.poopyfeed.pf.accounts

import android.content.Context
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import net.poopyfeed.pf.R
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private val mockAuthRepository: AuthRepository = mockk(relaxed = true)
  private val mockTokenManager: TokenManager = mockk(relaxed = true)
  private val mockContext: Context = mockk(relaxed = true)
  private lateinit var viewModel: SignupViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    every { mockContext.getString(R.string.error_network) } returns
        "Network error. Please check your connection."
    every { mockContext.getString(R.string.error_serialization) } returns
        "Data format error. Please try again."
    every { mockContext.getString(R.string.error_unknown) } returns
        "Something went wrong. Please try again."
    viewModel = SignupViewModel(mockAuthRepository, mockTokenManager, mockContext)
  }

  @After
  fun tearDown() {
    clearAllMocks()
    unmockkAll()
    Dispatchers.resetMain()
  }

  @Test
  fun `initial state is Idle`() {
    val state = viewModel.uiState.value
    assertIs<SignupUiState.Idle>(state)
  }

  @Test
  fun `signUp success saves token and emits Success`() = runTest {
    val token = "test-token-123"
    coEvery { mockAuthRepository.signup(any(), any()) } returns ApiResult.Success(token)
    every { mockTokenManager.saveToken(token) } returns Unit

    viewModel.signUp("test@example.com", "password123")

    testDispatcher.scheduler.advanceUntilIdle()

    assertIs<SignupUiState.Success>(viewModel.uiState.value)
    verify(exactly = 1) { mockTokenManager.saveToken(token) }
  }

  @Test
  fun `signUp error emits Error state`() = runTest {
    val apiError = ApiError.NetworkError("Network down")
    coEvery { mockAuthRepository.signup(any(), any()) } returns ApiResult.Error(apiError)

    viewModel.signUp("test@example.com", "password123")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<SignupUiState.Error>(state)
    assertEquals(apiError.getUserMessage(mockContext), state.message)
  }

  @Test
  fun `clearError from Error returns Idle`() = runTest {
    val apiError = ApiError.NetworkError("Network down")
    coEvery { mockAuthRepository.signup(any(), any()) } returns ApiResult.Error(apiError)

    viewModel.signUp("test@example.com", "password123")

    testDispatcher.scheduler.advanceUntilIdle()

    val errorState = viewModel.uiState.value
    assertIs<SignupUiState.Error>(errorState)

    viewModel.clearError()

    val idleState = viewModel.uiState.value
    assertIs<SignupUiState.Idle>(idleState)
  }
}
