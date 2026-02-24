package com.poopyfeed.android.ui.screens.greeting

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
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingViewModelTest {
    private lateinit var authRepository: AuthRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mock()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is LOADING`() =
        runTest {
            whenever(authRepository.hasToken()).thenReturn(false)
            val viewModel = GreetingViewModel(authRepository)
            assertEquals(AuthCheckState.LOADING, viewModel.authState.value)
        }

    @Test
    fun `emits AUTHENTICATED when token exists`() =
        runTest {
            whenever(authRepository.hasToken()).thenReturn(true)
            val viewModel = GreetingViewModel(authRepository)
            advanceUntilIdle()
            assertEquals(AuthCheckState.AUTHENTICATED, viewModel.authState.value)
        }

    @Test
    fun `emits UNAUTHENTICATED when no token`() =
        runTest {
            whenever(authRepository.hasToken()).thenReturn(false)
            val viewModel = GreetingViewModel(authRepository)
            advanceUntilIdle()
            assertEquals(AuthCheckState.UNAUTHENTICATED, viewModel.authState.value)
        }
}
