package com.poopyfeed.android.ui.screens.login

import com.poopyfeed.android.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mock()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.uiState.value
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.apiError)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `onEmailChange updates email and clears errors`() {
        viewModel.onEmailChange("test@example.com")
        assertEquals("test@example.com", viewModel.uiState.value.email)
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `onPasswordChange updates password and clears errors`() {
        viewModel.onPasswordChange("password123")
        assertEquals("password123", viewModel.uiState.value.password)
        assertNull(viewModel.uiState.value.passwordError)
    }

    @Test
    fun `login with empty email shows error`() {
        viewModel.onPasswordChange("password123")
        viewModel.login()
        assertNotNull(viewModel.uiState.value.emailError)
        assertEquals("Email is required", viewModel.uiState.value.emailError)
    }

    @Test
    fun `login with invalid email shows error`() {
        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("password123")
        viewModel.login()
        assertEquals("Enter a valid email address", viewModel.uiState.value.emailError)
    }

    @Test
    fun `login with empty password shows error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.login()
        assertEquals("Password is required", viewModel.uiState.value.passwordError)
    }

    @Test
    fun `login with short password shows error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("short")
        viewModel.login()
        assertEquals("Password must be at least 8 characters", viewModel.uiState.value.passwordError)
    }

    @Test
    fun `login success sets isSuccess`() =
        runTest {
            whenever(authRepository.login(any(), any()))
                .thenReturn(Result.success(Unit))

            viewModel.onEmailChange("test@example.com")
            viewModel.onPasswordChange("password123")
            viewModel.login()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isSuccess)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `login failure sets apiError`() =
        runTest {
            whenever(authRepository.login(any(), any()))
                .thenReturn(Result.failure(Exception("Invalid credentials")))

            viewModel.onEmailChange("test@example.com")
            viewModel.onPasswordChange("password123")
            viewModel.login()
            advanceUntilIdle()

            assertEquals("Invalid credentials", viewModel.uiState.value.apiError)
            assertFalse(viewModel.uiState.value.isLoading)
            assertFalse(viewModel.uiState.value.isSuccess)
        }

    @Test
    fun `validateEmail returns null for valid email`() {
        assertNull(LoginViewModel.validateEmail("test@example.com"))
    }

    @Test
    fun `validateEmail returns error for blank email`() {
        assertEquals("Email is required", LoginViewModel.validateEmail(""))
        assertEquals("Email is required", LoginViewModel.validateEmail("   "))
    }

    @Test
    fun `validatePassword returns null for valid password`() {
        assertNull(LoginViewModel.validatePassword("password123"))
    }

    @Test
    fun `validatePassword returns error for blank password`() {
        assertEquals("Password is required", LoginViewModel.validatePassword(""))
    }

    @Test
    fun `validatePassword returns error for short password`() {
        assertEquals("Password must be at least 8 characters", LoginViewModel.validatePassword("short"))
    }
}
