package net.poopyfeed.pf

import android.app.Application
import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
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
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.NetworkModule
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var application: Application
  private lateinit var viewModel: SignupViewModel

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    MockKAnnotations.init(this, relaxUnitFun = true)
    application = mockk(relaxed = true)

    mockkObject(NetworkModule)
    mockkConstructor(AuthRepository::class)

    viewModel = SignupViewModel(application)
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
  fun `signUp success emits Success state with token`() = runTest {
    val token = "test-token-123"
    coEvery { anyConstructed<AuthRepository>().signup(any(), any()) } returns
        ApiResult.Success(token)

    viewModel.signUp("test@example.com", "password123")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<SignupUiState.Success>(state)
    assertEquals(token, state.token)
  }

  @Test
  fun `signUp error emits Error state`() = runTest {
    val apiError = ApiError.NetworkError("Network down")
    coEvery { anyConstructed<AuthRepository>().signup(any(), any()) } returns
        ApiResult.Error(apiError)

    viewModel.signUp("test@example.com", "password123")

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<SignupUiState.Error>(state)
    assertEquals(apiError.getUserMessage(), state.message)
  }

  @Test
  fun `clearError from Error returns Idle`() = runTest {
    val apiError = ApiError.NetworkError("Network down")
    coEvery { anyConstructed<AuthRepository>().signup(any(), any()) } returns
        ApiResult.Error(apiError)

    viewModel.signUp("test@example.com", "password123")

    testDispatcher.scheduler.advanceUntilIdle()

    val errorState = viewModel.uiState.value
    assertIs<SignupUiState.Error>(errorState)

    viewModel.clearError()

    val idleState = viewModel.uiState.value
    assertIs<SignupUiState.Idle>(idleState)
  }
}
