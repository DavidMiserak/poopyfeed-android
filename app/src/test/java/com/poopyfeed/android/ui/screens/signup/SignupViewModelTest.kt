package com.poopyfeed.android.ui.screens.signup

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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: SignupViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mock()
        viewModel = SignupViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
        assertNull(state.nameError)
        assertNull(state.emailError)
        assertNull(state.passwordError)
        assertNull(state.confirmPasswordError)
        assertNull(state.apiError)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
    }

    @Test
    fun `onNameChange updates name and clears errors`() {
        viewModel.onNameChange("John")
        assertEquals("John", viewModel.uiState.value.name)
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `onEmailChange updates email and clears errors`() {
        viewModel.onEmailChange("test@example.com")
        assertEquals("test@example.com", viewModel.uiState.value.email)
    }

    @Test
    fun `onPasswordChange updates password and clears errors`() {
        viewModel.onPasswordChange("password123")
        assertEquals("password123", viewModel.uiState.value.password)
    }

    @Test
    fun `onConfirmPasswordChange updates confirmPassword and clears errors`() {
        viewModel.onConfirmPasswordChange("password123")
        assertEquals("password123", viewModel.uiState.value.confirmPassword)
    }

    @Test
    fun `signup with empty name shows error`() {
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.signup()
        assertEquals("Name is required", viewModel.uiState.value.nameError)
    }

    @Test
    fun `signup with empty email shows error`() {
        viewModel.onNameChange("John")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.signup()
        assertNotNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `signup with mismatched passwords shows error`() {
        viewModel.onNameChange("John")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("different")
        viewModel.signup()
        assertEquals("Passwords do not match", viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun `signup with empty confirm password shows error`() {
        viewModel.onNameChange("John")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.signup()
        assertEquals("Please confirm your password", viewModel.uiState.value.confirmPasswordError)
    }

    @Test
    fun `signup success sets isSuccess`() = runTest {
        whenever(authRepository.signup(any(), any()))
            .thenReturn(Result.success(Unit))

        viewModel.onNameChange("John")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.signup()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `signup sends only email and password to API (not name)`() = runTest {
        whenever(authRepository.signup(any(), any()))
            .thenReturn(Result.success(Unit))

        viewModel.onNameChange("John Doe")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.signup()
        advanceUntilIdle()

        verify(authRepository).signup("test@example.com", "password123")
    }

    @Test
    fun `signup failure sets apiError`() = runTest {
        whenever(authRepository.signup(any(), any()))
            .thenReturn(Result.failure(Exception("Email already exists")))

        viewModel.onNameChange("John")
        viewModel.onEmailChange("test@example.com")
        viewModel.onPasswordChange("password123")
        viewModel.onConfirmPasswordChange("password123")
        viewModel.signup()
        advanceUntilIdle()

        assertEquals("Email already exists", viewModel.uiState.value.apiError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `validateName returns error for blank name`() {
        assertEquals("Name is required", SignupViewModel.validateName(""))
        assertEquals("Name is required", SignupViewModel.validateName("   "))
    }

    @Test
    fun `validateName returns null for valid name`() {
        assertNull(SignupViewModel.validateName("John"))
    }

    @Test
    fun `validateConfirmPassword returns error when blank`() {
        assertEquals(
            "Please confirm your password",
            SignupViewModel.validateConfirmPassword("pass", ""),
        )
    }

    @Test
    fun `validateConfirmPassword returns error when mismatched`() {
        assertEquals(
            "Passwords do not match",
            SignupViewModel.validateConfirmPassword("pass1", "pass2"),
        )
    }

    @Test
    fun `validateConfirmPassword returns null when matching`() {
        assertNull(SignupViewModel.validateConfirmPassword("password123", "password123"))
    }
}
