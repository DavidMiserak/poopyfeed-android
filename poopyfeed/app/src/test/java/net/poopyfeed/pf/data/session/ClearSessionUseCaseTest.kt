package net.poopyfeed.pf.data.session

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClearSessionUseCaseTest {

  private val context: Context = mockk(relaxed = true)
  private val tokenManager: TokenManager = mockk(relaxed = true)
  private val notificationsRepository: NotificationsRepository = mockk(relaxed = true)
  private val cachedChildrenRepository: CachedChildrenRepository = mockk(relaxed = true)
  private val cachedFeedingsRepository: CachedFeedingsRepository = mockk(relaxed = true)
  private val cachedDiapersRepository: CachedDiapersRepository = mockk(relaxed = true)
  private val cachedNapsRepository: CachedNapsRepository = mockk(relaxed = true)

  private val useCase =
      ClearSessionUseCase(
          context = context,
          tokenManager = tokenManager,
          notificationsRepository = notificationsRepository,
          cachedChildrenRepository = cachedChildrenRepository,
          cachedFeedingsRepository = cachedFeedingsRepository,
          cachedDiapersRepository = cachedDiapersRepository,
          cachedNapsRepository = cachedNapsRepository,
      )

  @org.junit.Before
  fun setup() {
    // Mock FirebaseMessaging to throw so unregisterFcmToken()'s try-catch handles it
    mockkStatic(FirebaseMessaging::class)
    every { FirebaseMessaging.getInstance() } throws
        IllegalStateException("Firebase not initialized in tests")
    // Mock Log to prevent "not mocked" RuntimeException inside catch blocks
    mockkStatic(Log::class)
    every { Log.w(any<String>(), any<String>(), any()) } returns 0
  }

  @org.junit.After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `invoke clears caches and token even when FCM unregister fails`() = runTest {
    coEvery { cachedChildrenRepository.clearCache() } returns Unit

    useCase()

    // FCM unregister is skipped (Firebase throws in test), but caches are still cleared
    coVerify { cachedChildrenRepository.clearCache() }
    verify { cachedFeedingsRepository.clearSessionCache() }
    verify { cachedDiapersRepository.clearSessionCache() }
    verify { cachedNapsRepository.clearSessionCache() }
    verify { tokenManager.clearToken() }
  }
}
