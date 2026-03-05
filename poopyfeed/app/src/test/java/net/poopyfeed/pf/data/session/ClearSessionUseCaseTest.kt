package net.poopyfeed.pf.data.session

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.di.TokenManager
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClearSessionUseCaseTest {

  private val tokenManager: TokenManager = mockk(relaxed = true)
  private val cachedChildrenRepository: CachedChildrenRepository = mockk(relaxed = true)
  private val cachedFeedingsRepository: CachedFeedingsRepository = mockk(relaxed = true)
  private val cachedDiapersRepository: CachedDiapersRepository = mockk(relaxed = true)
  private val cachedNapsRepository: CachedNapsRepository = mockk(relaxed = true)

  private val useCase =
      ClearSessionUseCase(
          tokenManager = tokenManager,
          cachedChildrenRepository = cachedChildrenRepository,
          cachedFeedingsRepository = cachedFeedingsRepository,
          cachedDiapersRepository = cachedDiapersRepository,
          cachedNapsRepository = cachedNapsRepository,
      )

  @Test
  fun `invoke clears children cache then tracking sync state then token`() = runTest {
    coEvery { cachedChildrenRepository.clearCache() } returns Unit

    useCase()

    coVerify { cachedChildrenRepository.clearCache() }
    verify { cachedFeedingsRepository.clearSessionCache() }
    verify { cachedDiapersRepository.clearSessionCache() }
    verify { cachedNapsRepository.clearSessionCache() }
    verify { tokenManager.clearToken() }
  }
}
