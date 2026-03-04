package net.poopyfeed.pf

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SignupViewModel

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = SignupViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signUp success emits Success state with token`() = runTest {
        val token = "test-token-123"
        coEvery { authRepository.signup(any(), any()) } returns ApiResult.Success(token)

        viewModel.signUp("test@example.com", "password123")

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<SignupUiState.Success>(state)
        assertEquals(token, state.token)
    }

    @Test
    fun `signUp error emits Error state`() = runTest {
        val apiError = ApiError.NetworkError("Network down")
        coEvery { authRepository.signup(any(), any()) } returns ApiResult.Error(apiError)

        viewModel.signUp("test@example.com", "password123")

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<SignupUiState.Error>(state)
        assertEquals(apiError.getUserMessage(), state.message)
    }
}
