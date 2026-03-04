package net.poopyfeed.pf.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import net.poopyfeed.pf.data.db.ChildDao
import net.poopyfeed.pf.data.db.DiaperDao
import net.poopyfeed.pf.data.db.FeedingDao
import net.poopyfeed.pf.data.db.NapDao
import net.poopyfeed.pf.data.db.PoopyFeedDatabase

/** Hilt module for Room database and DAOs. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): PoopyFeedDatabase =
      PoopyFeedDatabase.getInstance(context.applicationContext)

  @Provides @Singleton fun provideChildDao(db: PoopyFeedDatabase): ChildDao = db.childDao()

  @Provides @Singleton fun provideFeedingDao(db: PoopyFeedDatabase): FeedingDao = db.feedingDao()

  @Provides @Singleton fun provideDiaperDao(db: PoopyFeedDatabase): DiaperDao = db.diaperDao()

  @Provides @Singleton fun provideNapDao(db: PoopyFeedDatabase): NapDao = db.napDao()
}
