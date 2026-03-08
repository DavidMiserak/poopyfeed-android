package net.poopyfeed.pf

import android.app.Application
import androidx.work.Configuration

/**
 * Base Application that provides [Configuration.Provider] for WorkManager. Used so a
 * [dagger.hilt.android.testing.CustomTestApplication] can extend it (Hilt forbids extending
 * a @HiltAndroidApp class). Production app uses [PoopyFeedApplication] which extends this and adds
 * Hilt + [HiltWorkerFactory].
 */
abstract class PoopyFeedApplicationBase : Application(), Configuration.Provider {
  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().build()
}
