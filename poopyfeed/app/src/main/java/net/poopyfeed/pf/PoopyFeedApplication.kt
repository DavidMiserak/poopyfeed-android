package net.poopyfeed.pf

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point for PoopyFeed. Enables Hilt dependency injection across the app. */
@HiltAndroidApp class PoopyFeedApplication : Application()
