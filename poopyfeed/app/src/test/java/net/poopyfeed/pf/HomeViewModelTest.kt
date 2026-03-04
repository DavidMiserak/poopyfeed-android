package net.poopyfeed.pf

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
import net.poopyfeed.pf.data.models.ApiError
import net.poopyfeed.pf.data.models.ApiResult
import net.poopyfeed.pf.data.repository.AuthRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

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
  fun `when token present and profile loads successfully then Ready state emitted`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"
    val profile = TestFixtures.mockUserProfile()
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Success(profile)

    val viewModel = HomeViewModel(mockAuthRepository, mockTokenManager)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<HomeUiState.Ready>(state)
    assertEquals("user@example.com", state.email)
  }

  @Test
  fun `when token is missing then Unauthorized state emitted without calling repository`() =
      runTest {
        every { mockTokenManager.getToken() } returns null

        val viewModel = HomeViewModel(mockAuthRepository, mockTokenManager)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Unauthorized>(state)
      }

  @Test
  fun `when profile request returns 401 then token cleared and Unauthorized state emitted`() =
      runTest {
        every { mockTokenManager.getToken() } returns "test-token"

        val httpError = ApiError.HttpError(statusCode = 401, errorMessage = "Unauthorized")
        coEvery { mockAuthRepository.getProfile() } returns ApiResult.Error(httpError)
        every { mockTokenManager.clearToken() } returns Unit

        val viewModel = HomeViewModel(mockAuthRepository, mockTokenManager)

        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<HomeUiState.Unauthorized>(state)
        verify(exactly = 1) { mockTokenManager.clearToken() }
      }

  @Test
  fun `when profile request fails with non-401 error then Error state emitted`() = runTest {
    every { mockTokenManager.getToken() } returns "test-token"

    val networkError = ApiError.NetworkError("Network down")
    coEvery { mockAuthRepository.getProfile() } returns ApiResult.Error(networkError)

    val viewModel = HomeViewModel(mockAuthRepository, mockTokenManager)

    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertIs<HomeUiState.Error>(state)
    assertEquals(networkError.getUserMessage(), state.message)
    verify(exactly = 0) { mockTokenManager.clearToken() }
  }
}
