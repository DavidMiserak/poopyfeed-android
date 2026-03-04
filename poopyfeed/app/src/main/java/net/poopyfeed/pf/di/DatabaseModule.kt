package net.poopyfeed.pf.di

import android.content.Context
import net.poopyfeed.pf.data.db.*

/**
 * Manual DI provider for Room database dependencies.
 * Provides database instance and DAO singletons.
 */
object DatabaseModule {

    private var database: PoopyFeedDatabase? = null

    fun provideDatabase(context: Context): PoopyFeedDatabase {
        return database ?: PoopyFeedDatabase.getInstance(context.applicationContext)
            .also { database = it }
    }

    fun provideChildDao(context: Context): ChildDao =
        provideDatabase(context).childDao()

    fun provideFeedingDao(context: Context): FeedingDao =
        provideDatabase(context).feedingDao()

    fun provideDiaperDao(context: Context): DiaperDao =
        provideDatabase(context).diaperDao()

    fun provideNapDao(context: Context): NapDao =
        provideDatabase(context).napDao()
}
