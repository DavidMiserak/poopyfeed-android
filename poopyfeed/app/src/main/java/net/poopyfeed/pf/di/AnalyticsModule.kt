package net.poopyfeed.pf.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.poopyfeed.pf.analytics.AnalyticsTracker

/** Hilt module for Firebase Analytics and analytics tracking. */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

  @Provides
  @Singleton
  fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
      FirebaseAnalytics.getInstance(context)

  @Provides
  @Singleton
  fun provideAnalyticsTracker(firebaseAnalytics: FirebaseAnalytics): AnalyticsTracker =
      AnalyticsTracker(firebaseAnalytics)
}
