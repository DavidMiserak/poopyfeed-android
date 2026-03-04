package net.poopyfeed.pf

import android.app.Application
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
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
import net.poopyfeed.pf.di.NetworkModule
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var application: Application

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
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
  fun `checkExistingToken returns true when token present`() {
    every { NetworkModule.getAuthToken(any()) } returns "test-token"

    val viewModel = LoginViewModel(application)

    assertTrue(viewModel.checkExistingToken())
  }

  @Test
  fun `checkExistingToken returns false when token null`() {
    every { NetworkModule.getAuthToken(any()) } returns null

    val viewModel = LoginViewModel(application)

    assertFalse(viewModel.checkExistingToken())
  }

  @Test
  fun `initial state is Idle`() {
    every { NetworkModule.getAuthToken(any()) } returns null

    val viewModel = LoginViewModel(application)

    assertIs<LoginUiState.Idle>(viewModel.uiState.value)
  }

  @Test
  fun `login success saves token and emits Success`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns null
    every { NetworkModule.saveAuthToken(any(), any()) } returns Unit
    coEvery { anyConstructed<AuthRepository>().login(any(), any()) } returns
        ApiResult.Success("token-123")

    val viewModel = LoginViewModel(application)
    viewModel.login("user@example.com", "password123")

    testDispatcher.scheduler.advanceUntilIdle()

    assertIs<LoginUiState.Success>(viewModel.uiState.value)
    verify(exactly = 1) { NetworkModule.saveAuthToken(any(), "token-123") }
  }

  @Test
  fun `login error emits Error state with message`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns null
    val apiError = ApiError.NetworkError("Network down")
    coEvery { anyConstructed<AuthRepository>().login(any(), any()) } returns
        ApiResult.Error(apiError)

    val viewModel = LoginViewModel(application)
    viewModel.login("user@example.com", "wrong")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<LoginUiState.Error>(state)
    assertEquals(apiError.getUserMessage(), state.message)
  }

  @Test
  fun `clearError from Error state sets Idle`() = runTest {
    every { NetworkModule.getAuthToken(any()) } returns null
    coEvery { anyConstructed<AuthRepository>().login(any(), any()) } returns
        ApiResult.Error(ApiError.NetworkError("err"))

    val viewModel = LoginViewModel(application)
    viewModel.login("a@b.com", "p")
    testDispatcher.scheduler.advanceUntilIdle()
    assertIs<LoginUiState.Error>(viewModel.uiState.value)

    viewModel.clearError()
    assertIs<LoginUiState.Idle>(viewModel.uiState.value)
  }

  @Test
  fun `clearError when not Error state leaves state unchanged`() {
    every { NetworkModule.getAuthToken(any()) } returns null

    val viewModel = LoginViewModel(application)
    viewModel.clearError()

    assertIs<LoginUiState.Idle>(viewModel.uiState.value)
  }
}
