package net.poopyfeed.pf

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import net.poopyfeed.pf.notifications.PoopyFeedMessagingService

/** Application entry point for PoopyFeed. Enables Hilt dependency injection across the app. */
@HiltAndroidApp
class PoopyFeedApplication : PoopyFeedApplicationBase() {

  @Inject lateinit var workerFactory: HiltWorkerFactory

  override val workManagerConfiguration: Configuration
    get() =
        if (::workerFactory.isInitialized) {
          Configuration.Builder().setWorkerFactory(workerFactory).build()
        } else {
          super.workManagerConfiguration
        }

  override fun onCreate() {
    super.onCreate()
    PoopyFeedMessagingService.createNotificationChannels(this)
  }
}
