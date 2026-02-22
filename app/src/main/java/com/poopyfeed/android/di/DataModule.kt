package com.poopyfeed.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    // TokenManager, CookieStore, and AuthRepository are @Singleton @Inject constructor
    // classes, so Hilt provides them automatically via constructor injection.
    // This module exists as a placeholder for future @Provides bindings (e.g., DataStore).
}
