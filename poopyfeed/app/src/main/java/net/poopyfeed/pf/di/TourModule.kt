package net.poopyfeed.pf.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import net.poopyfeed.pf.tour.TourPreferences

@Module
@InstallIn(SingletonComponent::class)
object TourModule {

  @Provides
  @Singleton
  @Named("tourPrefs")
  fun provideTourSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
      context.getSharedPreferences(TourPreferences.PREFS_NAME, Context.MODE_PRIVATE)

  @Provides
  @Singleton
  fun provideTourPreferences(@Named("tourPrefs") prefs: SharedPreferences): TourPreferences =
      TourPreferences(prefs)
}
