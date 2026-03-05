package net.poopyfeed.pf.data.session

import javax.inject.Inject
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.di.TokenManager

/**
 * Clears local session data: Room cache (children + CASCADE to feedings/diapers/naps), in-memory
 * sync state on tracking repos, and auth token. Call on logout, account deletion, or 401.
 */
class ClearSessionUseCase
@Inject
constructor(
    private val tokenManager: TokenManager,
    private val cachedChildrenRepository: CachedChildrenRepository,
    private val cachedFeedingsRepository: CachedFeedingsRepository,
    private val cachedDiapersRepository: CachedDiapersRepository,
    private val cachedNapsRepository: CachedNapsRepository,
) {

  suspend operator fun invoke() {
    cachedChildrenRepository.clearCache()
    cachedFeedingsRepository.clearSessionCache()
    cachedDiapersRepository.clearSessionCache()
    cachedNapsRepository.clearSessionCache()
    tokenManager.clearToken()
  }
}
