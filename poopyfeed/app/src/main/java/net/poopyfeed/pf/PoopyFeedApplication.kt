package net.poopyfeed.pf

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.poopyfeed.pf.notifications.PoopyFeedMessagingService

/** Application entry point for PoopyFeed. Enables Hilt dependency injection across the app. */
@HiltAndroidApp
class PoopyFeedApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    PoopyFeedMessagingService.createNotificationChannels(this)
  }
}
