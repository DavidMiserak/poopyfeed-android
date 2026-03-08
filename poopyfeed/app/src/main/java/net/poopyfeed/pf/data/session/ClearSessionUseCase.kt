package net.poopyfeed.pf.data.session

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.tasks.await
import net.poopyfeed.pf.data.repository.CachedChildrenRepository
import net.poopyfeed.pf.data.repository.CachedDiapersRepository
import net.poopyfeed.pf.data.repository.CachedFeedingsRepository
import net.poopyfeed.pf.data.repository.CachedNapsRepository
import net.poopyfeed.pf.data.repository.NotificationsRepository
import net.poopyfeed.pf.di.TokenManager
import net.poopyfeed.pf.notifications.PoopyFeedMessagingService

/**
 * Clears local session data: Room cache (children + CASCADE to feedings/diapers/naps), in-memory
 * sync state on tracking repos, FCM token unregistration, cached quiet hours, and auth token. Call
 * on logout, account deletion, or 401.
 */
class ClearSessionUseCase
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val notificationsRepository: NotificationsRepository,
    private val cachedChildrenRepository: CachedChildrenRepository,
    private val cachedFeedingsRepository: CachedFeedingsRepository,
    private val cachedDiapersRepository: CachedDiapersRepository,
    private val cachedNapsRepository: CachedNapsRepository,
) {

  suspend operator fun invoke() {
    // Unregister FCM token before clearing auth (needs token for API call)
    try {
      val fcmToken = FirebaseMessaging.getInstance().token.await()
      notificationsRepository.unregisterDeviceToken(fcmToken)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to unregister FCM token", e)
    }

    // Clear cached quiet hours to prevent cross-account data leak
    context
        .getSharedPreferences(PoopyFeedMessagingService.PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .remove(PoopyFeedMessagingService.KEY_QUIET_HOURS_ENABLED)
        .remove(PoopyFeedMessagingService.KEY_QUIET_HOURS_START)
        .remove(PoopyFeedMessagingService.KEY_QUIET_HOURS_END)
        .apply()

    cachedChildrenRepository.clearCache()
    cachedFeedingsRepository.clearSessionCache()
    cachedDiapersRepository.clearSessionCache()
    cachedNapsRepository.clearSessionCache()
    tokenManager.clearToken()
  }

  companion object {
    private const val TAG = "ClearSession"
  }
}
